package com.example.jarvis.ui

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.jarvis.audio.ClapDetector
import com.example.jarvis.model.ClapConfig
import com.example.jarvis.model.ClapEvent
import com.example.jarvis.routine.JarvisRoutineRunner
import com.example.jarvis.tts.JarvisVoiceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)

    private val _config = MutableStateFlow(loadConfig())
    val config: StateFlow<ClapConfig> = _config.asStateFlow()

    private val voiceManager = JarvisVoiceManager(application, viewModelScope)
    val routineRunner = JarvisRoutineRunner(application, voiceManager, viewModelScope)
    private val detector = ClapDetector(viewModelScope)

    val telemetry: StateFlow<ClapDetector.Telemetry> = detector.telemetry
    val routineExecutionState = routineRunner.executionState

    private val _history = MutableStateFlow<List<ClapEvent>>(emptyList())
    val history: StateFlow<List<ClapEvent>> = _history.asStateFlow()

    init {
        detector.onFirstClapListener = { rms ->
            if (_config.value.hapticEnabled) {
                blipHaptic()
            }
        }

        detector.onDoubleClapListener = { gap, rms, noiseFloor, threshold ->
            val event = ClapEvent(
                gapSeconds = gap,
                peakRms = rms,
                noiseFloor = noiseFloor,
                threshold = threshold,
                triggeredRoutine = true
            )
            _history.value = listOf(event) + _history.value.take(49)

            routineRunner.triggerRoutine(_config.value, "Double Clap (gap: ${(gap * 1000).toInt()}ms)")
        }
    }

    fun startListening(): Boolean {
        return detector.startListening(_config.value)
    }

    fun stopListening() {
        detector.stopListening()
    }

    fun triggerRoutineNow() {
        routineRunner.triggerRoutine(_config.value, "Manual UI Trigger")
    }

    fun updateConfig(newConfig: ClapConfig) {
        _config.value = newConfig
        saveConfig(newConfig)
        if (telemetry.value.isListening) {
            detector.stopListening()
            detector.startListening(newConfig)
        }
    }

    fun clearHistory() {
        _history.value = emptyList()
    }

    private fun blipHaptic() {
        try {
            val app = getApplication<Application>()
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = app.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                app.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                val effect = VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE)
                vibrator.vibrate(effect)
            }
        } catch (_: Exception) {}
    }

    private fun loadConfig(): ClapConfig {
        val defaultKey = if (com.example.jarvis.BuildConfig.ELEVENLABS_API_KEY != "DEFAULT_KEY") com.example.jarvis.BuildConfig.ELEVENLABS_API_KEY else ""
        val defaultVoice = if (com.example.jarvis.BuildConfig.ELEVENLABS_VOICE_ID != "DEFAULT_VOICE") com.example.jarvis.BuildConfig.ELEVENLABS_VOICE_ID else ""
        return ClapConfig(
            spikeRatio = prefs.getFloat("spike_ratio", 7.0f),
            minRms = prefs.getFloat("min_rms", 0.012f),
            cooldownS = prefs.getFloat("cooldown_s", 0.45f),
            minDoubleGapS = prefs.getFloat("min_double_gap_s", 0.05f),
            maxDoubleGapS = prefs.getFloat("max_double_gap_s", 0.35f),
            songUri = prefs.getString("song_uri", "https://open.spotify.com/track/39shmbIHICJ2Wxnk1fPSdz") ?: "",
            claudeUrl = prefs.getString("claude_url", "https://claude.ai/new") ?: "",
            binanceUrl = prefs.getString("binance_url", "https://www.binance.com/en/trade/BTC_USDT") ?: "",
            devToolUrl = prefs.getString("dev_tool_url", "https://cursor.com") ?: "",
            welcomePhrase = prefs.getString(
                "welcome_phrase",
                "Welcome home sir. Congratulations on the new client for your SaaS app—make sure to follow up. If it helps: a short, specific note while the deal is still fresh usually anchors trust better than a polished deck sent cold a few days later."
            ) ?: "",
            playSongEnabled = prefs.getBoolean("play_song_enabled", true),
            welcomeSpeechEnabled = prefs.getBoolean("welcome_speech_enabled", true),
            claudeEnabled = prefs.getBoolean("claude_enabled", true),
            binanceEnabled = prefs.getBoolean("binance_enabled", true),
            devToolEnabled = prefs.getBoolean("dev_tool_enabled", true),
            hapticEnabled = prefs.getBoolean("haptic_enabled", true),
            elevenLabsApiKey = prefs.getString("elevenlabs_api_key", defaultKey) ?: defaultKey,
            elevenLabsVoiceId = prefs.getString("elevenlabs_voice_id", defaultVoice) ?: defaultVoice
        )
    }

    private fun saveConfig(c: ClapConfig) {
        prefs.edit()
            .putFloat("spike_ratio", c.spikeRatio)
            .putFloat("min_rms", c.minRms)
            .putFloat("cooldown_s", c.cooldownS)
            .putFloat("min_double_gap_s", c.minDoubleGapS)
            .putFloat("max_double_gap_s", c.maxDoubleGapS)
            .putString("song_uri", c.songUri)
            .putString("claude_url", c.claudeUrl)
            .putString("binance_url", c.binanceUrl)
            .putString("dev_tool_url", c.devToolUrl)
            .putString("welcome_phrase", c.welcomePhrase)
            .putBoolean("play_song_enabled", c.playSongEnabled)
            .putBoolean("welcome_speech_enabled", c.welcomeSpeechEnabled)
            .putBoolean("claude_enabled", c.claudeEnabled)
            .putBoolean("binance_enabled", c.binanceEnabled)
            .putBoolean("dev_tool_enabled", c.devToolEnabled)
            .putBoolean("haptic_enabled", c.hapticEnabled)
            .putString("elevenlabs_api_key", c.elevenLabsApiKey)
            .putString("elevenlabs_voice_id", c.elevenLabsVoiceId)
            .apply()
    }

    override fun onCleared() {
        super.onCleared()
        detector.stopListening()
        voiceManager.release()
    }
}
