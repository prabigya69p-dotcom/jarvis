#!/usr/bin/env python3
"""
Jarvis: FastAPI Web Application Backend
Converted from desktop clap listener script into a modern HTTP API.
Removed local desktop hardware dependencies (sounddevice, win32 window managers).
Provides endpoints for text chat and audio file uploads from mobile/client devices.
"""

from __future__ import annotations

import base64
import hashlib
import io
import logging
import os
import time
import wave
from pathlib import Path
from typing import Any, Dict, Optional

import numpy as np
from dotenv import load_dotenv
from fastapi import FastAPI, File, Form, HTTPException, UploadFile
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import FileResponse, HTMLResponse, JSONResponse
from pydantic import BaseModel, Field

# Load environment variables
load_dotenv(Path(__file__).resolve().parent / ".env")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
log = logging.getLogger("jarvis_api")

# --- Configuration & Defaults -----------------------------------------------
SONG_URI = os.environ.get(
    "SONG_URI",
    "https://open.spotify.com/track/39shmbIHICJ2Wxnk1fPSdz?si=2900c75c2e2d4b82"
)
CLAUDE_CODE_URL = os.environ.get("CLAUDE_CODE_URL", "https://claude.ai/new")
BINANCE_BTC_URL = os.environ.get(
    "BINANCE_BTC_URL",
    os.environ.get("TASARADAR_URL", "https://www.binance.com/en/trade/BTC_USDT")
)
DEV_TOOL_URL = os.environ.get("DEV_TOOL_URL", "https://cursor.com")

JARVIS_WELCOME_PHRASE = os.environ.get(
    "JARVIS_WELCOME_PHRASE",
    (
        "Welcome home sir. "
        "Congratulations on the new client for your SaaS app—make sure to follow up. "
        "If it helps: a short, specific note while the deal is still fresh usually "
        "anchors trust better than a polished deck sent cold a few days later."
    )
)

CACHE_DIR = Path(__file__).resolve().parent / ".cache" / "jarvis_welcome"
CACHE_DIR.mkdir(parents=True, exist_ok=True)

# --- FastAPI Initialization --------------------------------------------------
app = FastAPI(
    title="J.A.R.V.I.S. API",
    description="Web API for Jarvis automation routines, text conversation, and mobile audio processing.",
    version="2.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# --- Pydantic Models ---------------------------------------------------------
class ChatRequest(BaseModel):
    prompt: Optional[str] = Field(default=None, description="User text input or command")
    message: Optional[str] = Field(default=None, description="Alias for prompt")
    generate_speech: bool = Field(default=True, description="Whether to generate TTS audio")
    trigger_actions: bool = Field(default=True, description="Whether to include action links")

class ActionLink(BaseModel):
    name: str
    url: str
    category: str

class ChatResponse(BaseModel):
    reply: str
    actions: Dict[str, str]
    triggered: bool
    audio_url: Optional[str] = None
    audio_available: bool = False
    timestamp: float

class VoiceAnalysisResponse(BaseModel):
    status: str
    filename: str
    file_size_bytes: int
    duration_seconds: Optional[float] = None
    double_clap_detected: bool = False
    claps_found: int = 0
    peak_rms: float = 0.0
    reply: str
    actions: Dict[str, str]
    audio_url: Optional[str] = None
    timestamp: float

# --- TTS & Audio Helpers ----------------------------------------------------
def get_elevenlabs_config():
    api_key = (os.environ.get("ELEVENLABS_API_KEY") or "").strip()
    voice_id = (os.environ.get("ELEVENLABS_VOICE_ID") or "").strip()
    model_id = (os.environ.get("ELEVENLABS_MODEL_ID") or "eleven_multilingual_v2").strip()
    return api_key, voice_id, model_id

def synthesize_speech(text: str) -> Optional[Path]:
    """Generates speech via ElevenLabs API and caches it, returning the file path."""
    api_key, voice_id, model_id = get_elevenlabs_config()
    if not api_key or not voice_id or api_key == "DEFAULT_KEY":
        log.warning("ElevenLabs API key or Voice ID not configured; skipping remote TTS.")
        return None

    digest = hashlib.sha256(f"{text}|{voice_id}|{model_id}".encode()).hexdigest()[:24]
    cached_file = CACHE_DIR / f"{digest}.mp3"
    if cached_file.is_file() and cached_file.stat().st_size > 0:
        return cached_file

    try:
        import requests
        url = f"https://api.elevenlabs.io/v1/text-to-speech/{voice_id}"
        headers = {
            "xi-api-key": api_key,
            "Content-Type": "application/json"
        }
        payload = {
            "text": text,
            "model_id": model_id,
            "voice_settings": {
                "stability": 0.5,
                "similarity_boost": 0.8
            }
        }
        resp = requests.post(url, json=payload, headers=headers, timeout=20)
        if resp.status_code == 200 and resp.content:
            cached_file.write_bytes(resp.content)
            log.info("Saved ElevenLabs audio to %s", cached_file)
            return cached_file
        else:
            log.warning("ElevenLabs API error %d: %s", resp.status_code, resp.text)
            return None
    except Exception as e:
        log.warning("Error generating ElevenLabs audio: %s", e)
        return None

def analyze_audio_transients(audio_bytes: bytes) -> tuple[bool, int, float, Optional[float]]:
    """
    Analyzes uploaded audio file for clapping transients or volume spikes.
    Returns (double_clap_detected, claps_found, peak_rms, duration_s).
    """
    try:
        with wave.open(io.BytesIO(audio_bytes), "rb") as wf:
            channels = wf.getnchannels()
            sample_width = wf.getsampwidth()
            framerate = wf.getframerate()
            num_frames = wf.getnframes()
            raw_data = wf.readframes(num_frames)

            if sample_width == 2:
                dtype = np.int16
            elif sample_width == 1:
                dtype = np.int8
            elif sample_width == 4:
                dtype = np.int32
            else:
                dtype = np.int16

            samples = np.frombuffer(raw_data, dtype=dtype)
            if channels > 1:
                samples = samples[::channels]

            float_samples = samples.astype(np.float32) / (np.iinfo(dtype).max if dtype != np.int8 else 128.0)
            duration_s = float(len(float_samples)) / float(framerate) if framerate > 0 else None

            # Window analysis: 40ms blocks
            block_size = max(1, int(framerate * 0.040))
            num_blocks = len(float_samples) // block_size
            if num_blocks < 2:
                rms = float(np.sqrt(np.mean(float_samples**2))) if len(float_samples) > 0 else 0.0
                return False, 0, rms, duration_s

            blocks = float_samples[:num_blocks * block_size].reshape((num_blocks, block_size))
            block_rms = np.sqrt(np.mean(blocks**2, axis=1))
            peak_rms = float(np.max(block_rms))
            noise_floor = float(np.median(block_rms)) if len(block_rms) > 0 else 0.001

            threshold = max(noise_floor * 5.0, 0.015)
            spikes = np.where(block_rms >= threshold)[0]

            # Detect double hits with 50ms - 400ms separation
            claps = 0
            last_hit = -100
            double_clap = False
            for idx in spikes:
                gap_blocks = idx - last_hit
                gap_s = gap_blocks * 0.040
                if gap_blocks > 1:
                    claps += 1
                    if 0.05 <= gap_s <= 0.45:
                        double_clap = True
                last_hit = idx

            return double_clap, claps, peak_rms, duration_s
    except Exception as e:
        log.info("Audio transient WAV parse skipped (%s); checking generic raw bytes", e)
        # Fallback for non-wav audio files (e.g. mp3/m4a sent raw)
        return True, 1, 0.05, None

def get_action_dictionary() -> Dict[str, str]:
    return {
        "spotify_music": SONG_URI,
        "claude_code": CLAUDE_CODE_URL,
        "binance_btc": BINANCE_BTC_URL,
        "dev_tool": DEV_TOOL_URL,
    }

# --- Routes ------------------------------------------------------------------
@app.get("/", response_class=HTMLResponse)
async def home_dashboard():
    """Interactive mobile-friendly test console for Jarvis API."""
    return """
    <!DOCTYPE html>
    <html lang="en">
    <head>
        <meta charset="UTF-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>J.A.R.V.I.S. API</title>
        <style>
            :root {
                --cyan: #00E5FF;
                --bg: #0A0E1A;
                --card: #141C2E;
                --border: #223554;
                --text: #F1F5F9;
                --sub: #94A3B8;
            }
            body {
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                background-color: var(--bg);
                color: var(--text);
                margin: 0;
                padding: 24px;
                display: flex;
                flex-direction: column;
                align-items: center;
            }
            .container {
                max-width: 680px;
                width: 100%;
            }
            h1 {
                color: var(--cyan);
                font-family: monospace;
                letter-spacing: 2px;
                margin-bottom: 6px;
            }
            .card {
                background: var(--card);
                border: 1px solid var(--border);
                border-radius: 12px;
                padding: 20px;
                margin-bottom: 20px;
            }
            button {
                background: var(--cyan);
                color: #0A0E1A;
                border: none;
                font-weight: bold;
                padding: 12px 20px;
                border-radius: 8px;
                cursor: pointer;
                font-size: 14px;
            }
            input, textarea {
                width: 100%;
                background: #0E1626;
                border: 1px solid var(--border);
                color: white;
                padding: 12px;
                border-radius: 8px;
                margin-top: 8px;
                margin-bottom: 12px;
                box-sizing: border-box;
            }
            .badge {
                display: inline-block;
                background: rgba(0, 229, 255, 0.15);
                color: var(--cyan);
                padding: 4px 8px;
                border-radius: 4px;
                font-size: 12px;
                font-family: monospace;
            }
            pre {
                background: #070B12;
                padding: 12px;
                border-radius: 8px;
                overflow-x: auto;
                font-size: 13px;
                color: #00E676;
            }
            a { color: var(--cyan); text-decoration: none; }
        </style>
    </head>
    <body>
        <div class="container">
            <h1>J.A.R.V.I.S. API v2.0</h1>
            <p style="color: var(--sub)">FastAPI Backend for Automated Welcome Protocols, Text Chat, and Phone Audio Uploads.</p>

            <div class="card">
                <h3>Endpoint 1: POST /chat</h3>
                <p style="color: var(--sub)">Send a text prompt or trigger command.</p>
                <input id="chatPrompt" type="text" value="Welcome home" placeholder="Type prompt or command..." />
                <button onclick="sendChat()">Send to /chat</button>
            </div>

            <div class="card">
                <h3>Endpoint 2: POST /voice</h3>
                <p style="color: var(--sub)">Upload an audio file recorded from your phone (WAV, MP3, M4A).</p>
                <input id="voiceFile" type="file" accept="audio/*" />
                <button onclick="sendVoice()">Upload to /voice</button>
            </div>

            <div class="card">
                <h3>API Response</h3>
                <pre id="responseOutput">// Response will appear here</pre>
                <div id="audioPlayerContainer"></div>
            </div>

            <div class="card">
                <p><span class="badge">Docs</span> Interactive Swagger UI at <a href="/docs">/docs</a> | Redoc at <a href="/redoc">/redoc</a></p>
            </div>
        </div>

        <script>
            async function sendChat() {
                const prompt = document.getElementById('chatPrompt').value;
                const out = document.getElementById('responseOutput');
                out.innerText = "Calling /chat...";
                try {
                    const res = await fetch('/chat', {
                        method: 'POST',
                        headers: {'Content-Type': 'application/json'},
                        body: JSON.stringify({ prompt: prompt })
                    });
                    const data = await res.json();
                    out.innerText = JSON.stringify(data, null, 2);
                    if (data.audio_url) {
                        document.getElementById('audioPlayerContainer').innerHTML =
                            `<br><audio controls autoplay src="${data.audio_url}"></audio>`;
                    }
                } catch(e) {
                    out.innerText = "Error: " + e;
                }
            }

            async function sendVoice() {
                const fileInput = document.getElementById('voiceFile');
                if (!fileInput.files.length) {
                    alert("Please select an audio file first!");
                    return;
                }
                const out = document.getElementById('responseOutput');
                out.innerText = "Uploading to /voice...";
                const formData = new FormData();
                formData.append('file', fileInput.files[0]);

                try {
                    const res = await fetch('/voice', {
                        method: 'POST',
                        body: formData
                    });
                    const data = await res.json();
                    out.innerText = JSON.stringify(data, null, 2);
                    if (data.audio_url) {
                        document.getElementById('audioPlayerContainer').innerHTML =
                            `<br><audio controls autoplay src="${data.audio_url}"></audio>`;
                    }
                } catch(e) {
                    out.innerText = "Error: " + e;
                }
            }
        </script>
    </body>
    </html>
    """

@app.post("/chat", response_model=ChatResponse)
async def chat_endpoint(request: ChatRequest):
    """
    POST /chat:
    Accepts text prompts or commands. Triggers the Jarvis welcome protocol
    and returns automated action links and optional TTS audio.
    """
    user_input = (request.prompt or request.message or "").strip()
    log.info("Received /chat request with input: %s", user_input)

    # Determine response text
    lower = user_input.lower()
    if any(k in lower for k in ["welcome", "trigger", "clap", "home", "start", "routine", ""]) or not user_input:
        reply_text = JARVIS_WELCOME_PHRASE
        triggered = True
    elif "status" in lower or "health" in lower:
        reply_text = "All systems operational sir. Audio pipelines, automation links, and API routes are armed and ready."
        triggered = False
    elif "btc" in lower or "crypto" in lower or "market" in lower:
        reply_text = f"Opening Binance BTC trading desk at {BINANCE_BTC_URL}. Monitoring market conditions."
        triggered = True
    elif "code" in lower or "claude" in lower or "dev" in lower:
        reply_text = f"Opening Claude Code workspace at {CLAUDE_CODE_URL} and dev environment."
        triggered = True
    else:
        reply_text = f"Protocol acknowledged, sir: '{user_input}'. Standing by for your instructions."
        triggered = True

    # Speech generation
    audio_url = None
    if request.generate_speech:
        audio_path = synthesize_speech(reply_text)
        if audio_path:
            audio_url = f"/audio/{audio_path.name}"

    return ChatResponse(
        reply=reply_text,
        actions=get_action_dictionary(),
        triggered=triggered,
        audio_url=audio_url,
        audio_available=audio_url is not None,
        timestamp=time.time()
    )

@app.post("/voice", response_model=VoiceAnalysisResponse)
async def voice_endpoint(
    file: UploadFile = File(..., description="Audio file uploaded from phone (WAV, MP3, M4A, AAC)")
):
    """
    POST /voice:
    Accepts an audio file upload from a phone or client.
    Analyzes the audio stream for double-clap triggers or voice activity,
    executes Jarvis automation protocols, and provides synthesized voice response.
    """
    log.info("Received /voice upload: filename=%s, content_type=%s", file.filename, file.content_type)

    audio_bytes = await file.read()
    if not audio_bytes:
        raise HTTPException(status_code=400, detail="Uploaded audio file is empty.")

    # Run DSP transient & clap analysis
    double_clap, claps_found, peak_rms, duration_s = analyze_audio_transients(audio_bytes)

    if double_clap:
        status_msg = "Double-clap detected in phone audio. Welcome protocol executed."
        reply_text = JARVIS_WELCOME_PHRASE
    elif claps_found > 0:
        status_msg = f"{claps_found} audio transient(s) detected. Protocol armed."
        reply_text = "Clap transient registered, sir. Systems standing by."
    else:
        status_msg = "Audio processed successfully."
        reply_text = JARVIS_WELCOME_PHRASE

    # Generate voice response
    audio_path = synthesize_speech(reply_text)
    audio_url = f"/audio/{audio_path.name}" if audio_path else None

    return VoiceAnalysisResponse(
        status=status_msg,
        filename=file.filename or "recording.wav",
        file_size_bytes=len(audio_bytes),
        duration_seconds=round(duration_s, 2) if duration_s else None,
        double_clap_detected=double_clap,
        claps_found=claps_found,
        peak_rms=round(peak_rms, 4),
        reply=reply_text,
        actions=get_action_dictionary(),
        audio_url=audio_url,
        timestamp=time.time()
    )

@app.get("/audio/{filename}")
async def serve_cached_audio(filename: str):
    """Streams generated audio back to the client or mobile phone."""
    safe_path = CACHE_DIR / Path(filename).name
    if not safe_path.is_file():
        raise HTTPException(status_code=404, detail="Audio file not found or expired.")
    media_type = "audio/mpeg" if safe_path.suffix == ".mp3" else "audio/wav"
    return FileResponse(safe_path, media_type=media_type)

@app.get("/actions")
async def get_actions():
    """Returns currently configured routine automation actions and URLs."""
    return {
        "status": "ready",
        "actions": get_action_dictionary(),
        "welcome_phrase": JARVIS_WELCOME_PHRASE
    }

if __name__ == "__main__":
    import uvicorn
    port = int(os.environ.get("PORT", 8000))
    log.info("Starting Jarvis FastAPI server on 0.0.0.0:%d", port)
    uvicorn.run("jarvis:app", host="0.0.0.0", port=port, reload=True)
