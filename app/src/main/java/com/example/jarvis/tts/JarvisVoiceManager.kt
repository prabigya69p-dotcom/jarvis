package com.example.jarvis.tts

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.jarvis.model.ClapConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

class JarvisVoiceManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "JarvisVoiceManager"
    }

    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var mediaPlayer: MediaPlayer? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                val ukResult = tts?.setLanguage(Locale.UK)
                if (ukResult == TextToSpeech.LANG_MISSING_DATA || ukResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.US)
                }
                tts?.setPitch(0.92f)
                tts?.setSpeechRate(1.02f)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                    }
                })
            } else {
                Log.w(TAG, "TTS Initialization failed with status: $status")
            }
        }
    }

    fun speak(
        text: String,
        config: ClapConfig,
        onStart: (() -> Unit)? = null,
        onFinished: (() -> Unit)? = null
    ) {
        val apiKey = config.elevenLabsApiKey.trim()
        val voiceId = config.elevenLabsVoiceId.trim()

        if (apiKey.isNotEmpty() && voiceId.isNotEmpty()) {
            scope.launch(Dispatchers.IO) {
                val success = fetchAndPlayElevenLabs(text, apiKey, voiceId, config.elevenLabsModelId, onStart, onFinished)
                if (!success) {
                    // Fallback to built-in Android TextToSpeech
                    withContext(Dispatchers.Main) {
                        speakNativeTts(text, onStart, onFinished)
                    }
                }
            }
        } else {
            speakNativeTts(text, onStart, onFinished)
        }
    }

    private fun speakNativeTts(
        text: String,
        onStart: (() -> Unit)? = null,
        onFinished: (() -> Unit)? = null
    ) {
        if (!isTtsReady || tts == null) {
            Log.w(TAG, "Native TTS not ready yet.")
            onFinished?.invoke()
            return
        }

        val utteranceId = "jarvis_phrase_${System.currentTimeMillis()}"
        val params = Bundle()
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        _isSpeaking.value = true
        onStart?.invoke()

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {
                _isSpeaking.value = true
                onStart?.invoke()
            }

            override fun onDone(id: String?) {
                _isSpeaking.value = false
                onFinished?.invoke()
            }

            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) {
                _isSpeaking.value = false
                onFinished?.invoke()
            }
        })
    }

    private suspend fun fetchAndPlayElevenLabs(
        text: String,
        apiKey: String,
        voiceId: String,
        modelId: String,
        onStart: (() -> Unit)?,
        onFinished: (() -> Unit)?
    ): Boolean {
        return try {
            val url = "https://api.elevenlabs.io/v1/text-to-speech/$voiceId"
            val jsonPayload = """
                {
                    "text": ${escapeJson(text)},
                    "model_id": "$modelId",
                    "voice_settings": {
                        "stability": 0.5,
                        "similarity_boost": 0.8
                    }
                }
            """.trimIndent()

            val request = Request.Builder()
                .url(url)
                .addHeader("xi-api-key", apiKey)
                .addHeader("Content-Type", "application/json")
                .post(jsonPayload.toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "ElevenLabs API responded with code: ${response.code}")
                return false
            }

            val body = response.body ?: return false
            val audioBytes = body.bytes()
            if (audioBytes.isEmpty()) return false

            val cacheFile = File(context.cacheDir, "jarvis_welcome_${System.currentTimeMillis()}.mp3")
            FileOutputStream(cacheFile).use { it.write(audioBytes) }

            withContext(Dispatchers.Main) {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(cacheFile.absolutePath)
                    prepare()
                    setOnCompletionListener {
                        _isSpeaking.value = false
                        onFinished?.invoke()
                        cacheFile.delete()
                    }
                    setOnErrorListener { _, _, _ ->
                        _isSpeaking.value = false
                        onFinished?.invoke()
                        cacheFile.delete()
                        true
                    }
                    start()
                }
                _isSpeaking.value = true
                onStart?.invoke()
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "ElevenLabs TTS failed: ${e.localizedMessage}", e)
            false
        }
    }

    private fun escapeJson(str: String): String {
        val escaped = str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    fun stop() {
        tts?.stop()
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.stop()
            }
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping media player", e)
        }
        _isSpeaking.value = false
    }

    fun release() {
        stop()
        tts?.shutdown()
        tts = null
    }
}
