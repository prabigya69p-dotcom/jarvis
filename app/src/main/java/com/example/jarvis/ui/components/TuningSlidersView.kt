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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import java.util.Locale

@Composable
fun TuningSlidersView(
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
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DSP CLAP DETECTION TUNING",
                color = ArcCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )

            TextButton(
                onClick = { onConfigChange(ClapConfig()) },
                modifier = Modifier.testTag("reset_tuning_button")
            ) {
                Text(
                    text = "RESET",
                    color = ArcAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Quick Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PresetButton(
                label = "Quiet Room",
                isSelected = config.spikeRatio <= 5.5f,
                onClick = {
                    onConfigChange(
                        config.copy(
                            spikeRatio = 5.0f,
                            minRms = 0.008f,
                            cooldownS = 0.40f
                        )
                    )
                }
            )

            PresetButton(
                label = "Balanced (Default)",
                isSelected = config.spikeRatio == 7.0f,
                onClick = {
                    onConfigChange(
                        config.copy(
                            spikeRatio = 7.0f,
                            minRms = 0.012f,
                            cooldownS = 0.45f
                        )
                    )
                }
            )

            PresetButton(
                label = "Noisy Room",
                isSelected = config.spikeRatio >= 9.0f,
                onClick = {
                    onConfigChange(
                        config.copy(
                            spikeRatio = 9.5f,
                            minRms = 0.022f,
                            cooldownS = 0.50f
                        )
                    )
                }
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Spike Ratio Slider
        TuningSlider(
            title = "SPIKE RATIO (Multiplier over noise floor)",
            valueText = String.format(Locale.US, "%.1fx", config.spikeRatio),
            currentValue = config.spikeRatio,
            range = 2f..15f,
            onValueChange = { onConfigChange(config.copy(spikeRatio = it)) }
        )

        // Min RMS Slider
        TuningSlider(
            title = "MIN RMS ABSOLUTE FLOOR",
            valueText = String.format(Locale.US, "%.4f", config.minRms),
            currentValue = config.minRms,
            range = 0.003f..0.040f,
            onValueChange = { onConfigChange(config.copy(minRms = it)) }
        )

        // Max Double Gap Slider
        TuningSlider(
            title = "MAX DOUBLE CLAP WINDOW",
            valueText = String.format(Locale.US, "%d ms", (config.maxDoubleGapS * 1000).toInt()),
            currentValue = config.maxDoubleGapS,
            range = 0.15f..0.60f,
            onValueChange = { onConfigChange(config.copy(maxDoubleGapS = it)) }
        )

        // Min Double Gap Slider
        TuningSlider(
            title = "MIN DOUBLE CLAP GAP",
            valueText = String.format(Locale.US, "%d ms", (config.minDoubleGapS * 1000).toInt()),
            currentValue = config.minDoubleGapS,
            range = 0.02f..0.15f,
            onValueChange = { onConfigChange(config.copy(minDoubleGapS = it)) }
        )

        // Cooldown Slider
        TuningSlider(
            title = "COOLDOWN DEBOUNCE",
            valueText = String.format(Locale.US, "%.2f s", config.cooldownS),
            currentValue = config.cooldownS,
            range = 0.20f..1.20f,
            onValueChange = { onConfigChange(config.copy(cooldownS = it)) }
        )
    }
}

@Composable
private fun PresetButton(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (isSelected) ArcCyan.copy(alpha = 0.15f) else DarkSurfaceElevated,
            contentColor = if (isSelected) ArcCyan else TextSecondary
        ),
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (isSelected) ArcCyan else DarkBorder
            )
        ),
        shape = RoundedCornerShape(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun TuningSlider(
    title: String,
    valueText: String,
    currentValue: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = valueText,
                color = ArcCyan,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = currentValue,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = ArcCyan,
                activeTrackColor = ArcCyan,
                inactiveTrackColor = DarkSurfaceElevated
            )
        )
    }
}
