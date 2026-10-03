package com.example.jarvis.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.ui.theme.ArcAmber
import com.example.jarvis.ui.theme.ArcCyan
import com.example.jarvis.ui.theme.ArcGreen
import com.example.jarvis.ui.theme.ArcRed
import com.example.jarvis.ui.theme.DarkBorder
import com.example.jarvis.ui.theme.DarkSurface
import com.example.jarvis.ui.theme.DarkSurfaceElevated
import com.example.jarvis.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AudioLevelMeter(
    rms: Float,
    noiseFloor: Float,
    threshold: Float,
    modifier: Modifier = Modifier
) {
    val maxDisplay = 0.20f
    val animatedRms by animateFloatAsState(targetValue = (rms / maxDisplay).coerceIn(0f, 1f), label = "rms")
    val floorFraction = (noiseFloor / maxDisplay).coerceIn(0f, 1f)
    val threshFraction = (threshold / maxDisplay).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AUDIO SPECTRUM & SENSORS",
                color = ArcCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
            Text(
                text = String.format(Locale.US, "RMS: %.4f", rms),
                color = if (rms >= threshold) ArcRed else TextSecondary,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Meter Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurfaceElevated)
                .border(1.dp, DarkBorder, RoundedCornerShape(6.dp))
        ) {
            // Active level fill
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedRms)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                ArcCyan,
                                if (rms >= threshold) ArcRed else ArcGreen
                            )
                        )
                    )
            )

            // Noise floor indicator
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(floorFraction.coerceAtLeast(0.01f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(2.dp)
                        .background(ArcCyan)
                        .align(Alignment.CenterEnd)
                )
            }

            // Spike threshold indicator line
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(threshFraction.coerceAtLeast(0.02f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(3.dp)
                        .background(ArcAmber)
                        .align(Alignment.CenterEnd)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Telemetry Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(8.dp).height(8.dp).background(ArcCyan, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format(Locale.US, "Floor: %.4f", noiseFloor),
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.width(8.dp).height(8.dp).background(ArcAmber, RoundedCornerShape(2.dp)))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = String.format(Locale.US, "Threshold: %.4f", threshold),
                    color = ArcAmber,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
