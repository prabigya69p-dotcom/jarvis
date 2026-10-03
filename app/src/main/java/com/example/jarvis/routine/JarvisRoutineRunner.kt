package com.example.jarvis.routine

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.example.jarvis.model.ClapConfig
import com.example.jarvis.tts.JarvisVoiceManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class JarvisRoutineRunner(
    private val context: Context,
    private val voiceManager: JarvisVoiceManager,
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "JarvisRoutineRunner"

        fun defaultSteps(): List<RoutineStep> = listOf(
            RoutineStep("music", "Play Music", "Open Spotify/YouTube track"),
            RoutineStep("voice", "Jarvis Greeting", "Voice announcement"),
            RoutineStep("claude", "Claude Code", "Open AI coding workspace"),
            RoutineStep("crypto", "Binance BTC", "Open live crypto market tracker"),
            RoutineStep("dev", "Cursor Dev Tool", "Launch developer workspace")
        )
    }

    enum class StepStatus {
        PENDING, RUNNING, COMPLETED, SKIPPED, FAILED
    }

    data class RoutineStep(
        val id: String,
        val title: String,
        val description: String,
        val status: StepStatus = StepStatus.PENDING,
        val extraInfo: String? = null
    )

    data class RoutineExecutionState(
        val isRunning: Boolean = false,
        val currentStepTitle: String = "Standby",
        val steps: List<RoutineStep> = defaultSteps(),
        val completionMessage: String? = null
    )

    private val _executionState = MutableStateFlow(RoutineExecutionState())
    val executionState: StateFlow<RoutineExecutionState> = _executionState.asStateFlow()

    fun triggerRoutine(config: ClapConfig, reason: String = "Double Clap Detected") {
        if (_executionState.value.isRunning) {
            Log.w(TAG, "Routine is already in progress.")
            return
        }

        scope.launch {
            runExecution(config, reason)
        }
    }

    private suspend fun runExecution(config: ClapConfig, reason: String) {
        val initialSteps = listOf(
            RoutineStep("music", "Play Music", config.songUri, if (config.playSongEnabled) StepStatus.PENDING else StepStatus.SKIPPED),
            RoutineStep("voice", "Jarvis Greeting", "Voice announcement", if (config.welcomeSpeechEnabled) StepStatus.PENDING else StepStatus.SKIPPED),
            RoutineStep("claude", "Claude Code", config.claudeUrl, if (config.claudeEnabled) StepStatus.PENDING else StepStatus.SKIPPED),
            RoutineStep("crypto", "Binance BTC", config.binanceUrl, if (config.binanceEnabled) StepStatus.PENDING else StepStatus.SKIPPED),
            RoutineStep("dev", "Cursor Dev Tool", config.devToolUrl, if (config.devToolEnabled) StepStatus.PENDING else StepStatus.SKIPPED)
        )

        _executionState.value = RoutineExecutionState(
            isRunning = true,
            currentStepTitle = "Initiating: $reason",
            steps = initialSteps,
            completionMessage = null
        )

        if (config.hapticEnabled) {
            triggerDoubleClapHaptic()
        }

        // Step 1: Open Spotify / Music
        if (config.playSongEnabled && config.songUri.isNotBlank()) {
            updateStepStatus("music", StepStatus.RUNNING)
            _executionState.value = _executionState.value.copy(currentStepTitle = "Launching Spotify Track...")
            val success = openUrl(config.songUri)
            updateStepStatus("music", if (success) StepStatus.COMPLETED else StepStatus.FAILED)
        }

        // Delay after song launch
        val delayMs = (config.afterSongDelayS * 1000).toLong().coerceAtLeast(100L)
        delay(delayMs)

        // Step 2: Voice welcome
        if (config.welcomeSpeechEnabled && config.welcomePhrase.isNotBlank()) {
            updateStepStatus("voice", StepStatus.RUNNING)
            _executionState.value = _executionState.value.copy(currentStepTitle = "Jarvis Speaking...")

            withContext(Dispatchers.Main) {
                voiceManager.speak(
                    text = config.welcomePhrase,
                    config = config,
                    onStart = {
                        // spoken
                    },
                    onFinished = {
                        scope.launch {
                            updateStepStatus("voice", StepStatus.COMPLETED)
                        }
                    }
                )
            }
        }

        // Step 3: Claude Code
        if (config.claudeEnabled && config.claudeUrl.isNotBlank()) {
            delay(400)
            updateStepStatus("claude", StepStatus.RUNNING)
            _executionState.value = _executionState.value.copy(currentStepTitle = "Opening Claude Code...")
            val success = openUrl(config.claudeUrl)
            updateStepStatus("claude", if (success) StepStatus.COMPLETED else StepStatus.FAILED)
        }

        // Step 4: Binance BTC
        if (config.binanceEnabled && config.binanceUrl.isNotBlank()) {
            delay(400)
            updateStepStatus("crypto", StepStatus.RUNNING)
            _executionState.value = _executionState.value.copy(currentStepTitle = "Opening Binance BTC...")
            val success = openUrl(config.binanceUrl)
            updateStepStatus("crypto", if (success) StepStatus.COMPLETED else StepStatus.FAILED)
        }

        // Step 5: Dev Workspace / Cursor
        if (config.devToolEnabled && config.devToolUrl.isNotBlank()) {
            delay(400)
            updateStepStatus("dev", StepStatus.RUNNING)
            _executionState.value = _executionState.value.copy(currentStepTitle = "Launching Workspace...")
            val success = openUrl(config.devToolUrl)
            updateStepStatus("dev", if (success) StepStatus.COMPLETED else StepStatus.FAILED)
        }

        delay(600)
        _executionState.value = _executionState.value.copy(
            isRunning = false,
            currentStepTitle = "All Actions Executed",
            completionMessage = "Jarvis Welcome Protocol Completed Successfully"
        )
    }

    private fun updateStepStatus(id: String, newStatus: StepStatus) {
        val updated = _executionState.value.steps.map { step ->
            if (step.id == id) step.copy(status = newStatus) else step
        }
        _executionState.value = _executionState.value.copy(steps = updated)
    }

    private fun openUrl(url: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open url: $url", e)
            false
        }
    }

    private fun triggerDoubleClapHaptic() {
        try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            if (vibrator != null && vibrator.hasVibrator()) {
                val pattern = longArrayOf(0, 70, 80, 120)
                val effect = VibrationEffect.createWaveform(pattern, -1)
                vibrator.vibrate(effect)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Haptic vibration failed", e)
        }
    }
}
