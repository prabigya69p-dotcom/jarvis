package com.example.jarvis.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import com.example.jarvis.model.ClapConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class ClapDetector(
    private val scope: CoroutineScope
) {
    companion object {
        private const val TAG = "ClapDetector"
    }

    data class Telemetry(
        val rms: Float = 0f,
        val noiseFloor: Float = 0.0001f,
        val threshold: Float = 0.012f,
        val isFirstClapDetected: Boolean = false,
        val firstClapCountdown: Float = 0f,
        val isArmed: Boolean = true,
        val isListening: Boolean = false,
        val errorMessage: String? = null
    )

    private val _telemetry = MutableStateFlow(Telemetry())
    val telemetry: StateFlow<Telemetry> = _telemetry.asStateFlow()

    private var recordingJob: Job? = null
    private var audioRecord: AudioRecord? = null

    var onFirstClapListener: ((rms: Float) -> Unit)? = null
    var onDoubleClapListener: ((gap: Float, rms: Float, noiseFloor: Float, threshold: Float) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun startListening(config: ClapConfig): Boolean {
        if (_telemetry.value.isListening) return true

        val sampleRate = config.sampleRate
        val blockSamples = max(1, (sampleRate * config.blockMs) / 1000)
        val minBufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) {
            _telemetry.value = _telemetry.value.copy(
                errorMessage = "Audio hardware initialization failed for $sampleRate Hz."
            )
            return false
        }

        val bufferSize = max(minBufferSize, blockSamples * 2 * 4)

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                audioRecord?.release()
                audioRecord = null
                _telemetry.value = _telemetry.value.copy(
                    errorMessage = "Cannot access microphone. Check permissions."
                )
                return false
            }

            audioRecord?.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AudioRecord", e)
            _telemetry.value = _telemetry.value.copy(
                errorMessage = "Audio record error: ${e.localizedMessage}"
            )
            return false
        }

        _telemetry.value = Telemetry(isListening = true)

        recordingJob = scope.launch(Dispatchers.Default) {
            val buffer = ShortArray(blockSamples)
            var noiseFloor = 0.0001f
            var lastLoggedDouble = 0.0
            var firstClapTime: Double? = null
            var spikeArmed = true

            while (isActive && audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val readSamples = audioRecord?.read(buffer, 0, blockSamples) ?: 0
                if (readSamples <= 0) continue

                // Calculate RMS mono float [-1, 1]
                var sumSq = 0.0
                for (i in 0 until readSamples) {
                    val sample = buffer[i].toDouble() / 32768.0
                    sumSq += sample * sample
                }
                val level = sqrt(sumSq / readSamples).toFloat()

                // Noise floor adaptation
                val quietGate = noiseFloor * config.quietGateMult
                if (level < quietGate) {
                    noiseFloor = config.noiseFloorAlpha * noiseFloor + (1.0f - config.noiseFloorAlpha) * level
                    noiseFloor = max(noiseFloor, 1e-7f)
                }

                val threshold = max(noiseFloor * config.spikeRatio, config.minRms)
                val now = SystemClock.elapsedRealtime() / 1000.0
                val retriggerLevel = threshold * config.retriggerRatio

                if (level < retriggerLevel) {
                    spikeArmed = true
                }

                // Check first clap timeout
                var countdown = 0f
                if (firstClapTime != null) {
                    val elapsed = (now - firstClapTime!!).toFloat()
                    if (elapsed > config.maxDoubleGapS) {
                        firstClapTime = null
                    } else {
                        countdown = 1.0f - (elapsed / config.maxDoubleGapS)
                    }
                }

                // Double clap trigger logic
                if (spikeArmed && level >= threshold && (now - lastLoggedDouble) >= config.cooldownS) {
                    spikeArmed = false

                    if (firstClapTime == null) {
                        firstClapTime = now
                        countdown = 1.0f
                        onFirstClapListener?.invoke(level)
                    } else {
                        val gap = (now - firstClapTime!!).toFloat()
                        if (gap >= config.minDoubleGapS && gap <= config.maxDoubleGapS) {
                            firstClapTime = null
                            countdown = 0f
                            lastLoggedDouble = now
                            onDoubleClapListener?.invoke(gap, level, noiseFloor, threshold)
                        } else if (gap > config.maxDoubleGapS) {
                            firstClapTime = now
                            countdown = 1.0f
                            onFirstClapListener?.invoke(level)
                        }
                    }
                }

                _telemetry.value = Telemetry(
                    rms = level,
                    noiseFloor = noiseFloor,
                    threshold = threshold,
                    isFirstClapDetected = (firstClapTime != null),
                    firstClapCountdown = countdown,
                    isArmed = spikeArmed,
                    isListening = true,
                    errorMessage = null
                )
            }
        }

        return true
    }

    fun stopListening() {
        recordingJob?.cancel()
        recordingJob = null
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping audioRecord", e)
        }
        audioRecord = null
        _telemetry.value = _telemetry.value.copy(
            isListening = false,
            isFirstClapDetected = false,
            firstClapCountdown = 0f
        )
    }
}
