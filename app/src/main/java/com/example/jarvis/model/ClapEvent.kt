package com.example.jarvis.model

data class ClapEvent(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val gapSeconds: Float,
    val peakRms: Float,
    val noiseFloor: Float,
    val threshold: Float,
    val triggeredRoutine: Boolean
)
