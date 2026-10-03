package com.example.jarvis.model

data class ClapConfig(
    val sampleRate: Int = 44100,
    val blockMs: Int = 40,
    val spikeRatio: Float = 7.0f,
    val cooldownS: Float = 0.45f,
    val minDoubleGapS: Float = 0.05f,
    val maxDoubleGapS: Float = 0.35f,
    val retriggerRatio: Float = 0.55f,
    val noiseFloorAlpha: Float = 0.992f,
    val minRms: Float = 0.012f,
    val quietGateMult: Float = 2.2f,

    // Routine actions
    val songUri: String = "https://open.spotify.com/track/39shmbIHICJ2Wxnk1fPSdz",
    val claudeUrl: String = "https://claude.ai/new",
    val binanceUrl: String = "https://www.binance.com/en/trade/BTC_USDT",
    val devToolUrl: String = "https://cursor.com",
    val welcomePhrase: String = "Welcome home sir. Congratulations on the new client for your SaaS app—make sure to follow up. If it helps: a short, specific note while the deal is still fresh usually anchors trust better than a polished deck sent cold a few days later.",
    val afterSongDelayS: Float = 1.0f,

    // Action toggles
    val playSongEnabled: Boolean = true,
    val welcomeSpeechEnabled: Boolean = true,
    val claudeEnabled: Boolean = true,
    val binanceEnabled: Boolean = true,
    val devToolEnabled: Boolean = true,
    val hapticEnabled: Boolean = true,

    // ElevenLabs optional
    val elevenLabsApiKey: String = "",
    val elevenLabsVoiceId: String = "",
    val elevenLabsModelId: String = "eleven_multilingual_v2"
)
