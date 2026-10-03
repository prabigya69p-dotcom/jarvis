package com.example.jarvis.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.model.ClapConfig
import com.example.jarvis.ui.theme.ArcAmber
import com.example.jarvis.ui.theme.ArcCyan
import com.example.jarvis.ui.theme.DarkBorder
import com.example.jarvis.ui.theme.DarkSurface
import com.example.jarvis.ui.theme.DarkSurfaceElevated
import com.example.jarvis.ui.theme.TextPrimary
import com.example.jarvis.ui.theme.TextSecondary

@Composable
fun RoutineConfigView(
    config: ClapConfig,
    onConfigChange: (ClapConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "ACTION PROTOCOL CONFIGURATION",
            color = ArcCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Music Action
        ActionConfigCard(
            title = "Spotify / Song Action",
            icon = Icons.Default.PlayArrow,
            enabled = config.playSongEnabled,
            onEnabledChange = { onConfigChange(config.copy(playSongEnabled = it)) },
            fieldLabel = "Song URI / Spotify Track URL",
            fieldValue = config.songUri,
            onFieldValueChange = { onConfigChange(config.copy(songUri = it)) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Voice Action
        ActionConfigCard(
            title = "Jarvis Welcome Announcement",
            icon = Icons.Default.Notifications,
            enabled = config.welcomeSpeechEnabled,
            onEnabledChange = { onConfigChange(config.copy(welcomeSpeechEnabled = it)) },
            fieldLabel = "Spoken Phrase",
            fieldValue = config.welcomePhrase,
            onFieldValueChange = { onConfigChange(config.copy(welcomePhrase = it)) },
            singleLine = false
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Claude Action
        ActionConfigCard(
            title = "Claude Code AI",
            icon = Icons.Default.Star,
            enabled = config.claudeEnabled,
            onEnabledChange = { onConfigChange(config.copy(claudeEnabled = it)) },
            fieldLabel = "Claude URL",
            fieldValue = config.claudeUrl,
            onFieldValueChange = { onConfigChange(config.copy(claudeUrl = it)) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Binance Action
        ActionConfigCard(
            title = "Crypto Market Tracker",
            icon = Icons.Default.Share,
            enabled = config.binanceEnabled,
            onEnabledChange = { onConfigChange(config.copy(binanceEnabled = it)) },
            fieldLabel = "Binance / Market URL",
            fieldValue = config.binanceUrl,
            onFieldValueChange = { onConfigChange(config.copy(binanceUrl = it)) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Dev Tool Action
        ActionConfigCard(
            title = "Cursor / Dev Workspace",
            icon = Icons.Default.Settings,
            enabled = config.devToolEnabled,
            onEnabledChange = { onConfigChange(config.copy(devToolEnabled = it)) },
            fieldLabel = "Workspace URL",
            fieldValue = config.devToolUrl,
            onFieldValueChange = { onConfigChange(config.copy(devToolUrl = it)) }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Haptic feedback toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = ArcCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.padding(start = 10.dp))
                Column {
                    Text("Dual-Clap Haptics", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("Vibrate on clap detection", color = TextSecondary, fontSize = 11.sp)
                }
            }
            Switch(
                checked = config.hapticEnabled,
                onCheckedChange = { onConfigChange(config.copy(hapticEnabled = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = ArcCyan,
                    checkedTrackColor = DarkSurface,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = DarkSurfaceElevated
                )
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ElevenLabs API Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "ELEVENLABS TTS API (OPTIONAL)",
                    color = ArcAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "If left blank, Jarvis uses the built-in high performance Android speech synthesizer.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = config.elevenLabsApiKey,
                    onValueChange = { onConfigChange(config.copy(elevenLabsApiKey = it)) },
                    label = { Text("ElevenLabs API Key") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArcCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("elevenlabs_api_key_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = config.elevenLabsVoiceId,
                    onValueChange = { onConfigChange(config.copy(elevenLabsVoiceId = it)) },
                    label = { Text("Voice ID (e.g. 21m00Tcm4TlvDq8ikWAM)") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArcCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("elevenlabs_voice_id_input")
                )
            }
        }
    }
}

@Composable
private fun ActionConfigCard(
    title: String,
    icon: ImageVector,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    fieldLabel: String,
    fieldValue: String,
    onFieldValueChange: (String) -> Unit,
    singleLine: Boolean = true
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (enabled) ArcCyan else TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.padding(start = 10.dp))
                    Text(
                        text = title,
                        color = if (enabled) TextPrimary else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = ArcCyan,
                        checkedTrackColor = DarkSurface,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkSurfaceElevated
                    )
                )
            }

            if (enabled) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fieldValue,
                    onValueChange = onFieldValueChange,
                    label = { Text(fieldLabel) },
                    singleLine = singleLine,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArcCyan,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
