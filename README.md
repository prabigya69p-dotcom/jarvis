# Jarvis: Automation Assistant & FastAPI Web Service

A modern Jarvis automation assistant providing both a **FastAPI web backend** for remote clients/mobile phones and an **Android application**.

## FastAPI Web Service (`jarvis.py`)

Converted from the local desktop script into a FastAPI web application without local desktop hardware/soundcard dependencies.

### Installation

```bash
pip install -r requirements.txt
```

### Running the Server

```bash
uvicorn jarvis:app --host 0.0.0.0 --port 8000 --reload
# Or directly:
python jarvis.py
```

### HTTP Endpoints

1. **`POST /chat`**: Accepts JSON payload for text prompts and commands.
   - **Request**:
     ```json
     {
       "prompt": "Welcome home sir",
       "generate_speech": true
     }
     ```
   - **Response**:
     ```json
     {
       "reply": "Welcome home sir...",
       "actions": {
         "spotify_music": "https://open.spotify.com/track/...",
         "claude_code": "https://claude.ai/new",
         "binance_btc": "https://www.binance.com/en/trade/BTC_USDT",
         "dev_tool": "https://cursor.com"
       },
       "triggered": true,
       "audio_url": "/audio/hash.mp3",
       "timestamp": 1696320000.0
     }
     ```

2. **`POST /voice`**: Multipart form upload for audio files recorded from mobile phones.
   - **Form Field**: `file` (WAV, MP3, M4A, AAC)
   - Performs DSP transient analysis to detect double-claps or voice triggers.
   - Executes the welcome protocol and returns action links with synthesized voice audio.

3. **`GET /`**: Mobile-friendly interactive web console to test `/chat` and `/voice` directly in the browser.
4. **`GET /actions`**: Returns the active routine action links.
5. **`GET /docs`**: Interactive Swagger documentation.

## Android Client

The native Android app provides a live DSP double-clap detector with an Arc Reactor HUD, real-time audio spectrum visualization, and protocol automation triggers.
