package com.example.jarvis.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.ui.theme.ArcAmber
import com.example.jarvis.ui.theme.ArcBlue
import com.example.jarvis.ui.theme.ArcCyan
import com.example.jarvis.ui.theme.ArcGreen
import com.example.jarvis.ui.theme.DarkSurfaceVariant
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorVisualizer(
    rms: Float,
    isListening: Boolean,
    isFirstClapDetected: Boolean,
    firstClapCountdown: Float,
    isExecutingRoutine: Boolean,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "arc_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 10000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    val activeColor = when {
        isExecutingRoutine -> ArcGreen
        isFirstClapDetected -> ArcAmber
        isListening -> ArcCyan
        else -> ArcBlue.copy(alpha = 0.4f)
    }

    Box(
        modifier = modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val outerRadius = size.minDimension / 2f * 0.92f
            val midRadius = outerRadius * 0.72f
            val innerRadius = outerRadius * 0.48f

            val rmsScale = (rms * 12f).coerceIn(0f, 0.4f)
            val dynamicMidRadius = midRadius * (1f + rmsScale)

            // Outer segmented ring
            val segmentCount = 24
            for (i in 0 until segmentCount) {
                val angleDeg = (i * (360f / segmentCount)) + rotation
                val angleRad = Math.toRadians(angleDeg.toDouble())
                val start = Offset(
                    (center.x + (outerRadius - 10f) * cos(angleRad)).toFloat(),
                    (center.y + (outerRadius - 10f) * sin(angleRad)).toFloat()
                )
                val end = Offset(
                    (center.x + outerRadius * cos(angleRad)).toFloat(),
                    (center.y + outerRadius * sin(angleRad)).toFloat()
                )
                val strokeColor = if (i % 3 == 0) activeColor else activeColor.copy(alpha = 0.35f)
                drawLine(
                    color = strokeColor,
                    start = start,
                    end = end,
                    strokeWidth = 3f,
                    cap = StrokeCap.Round
                )
            }

            // Outer boundary circle
            drawCircle(
                color = activeColor.copy(alpha = 0.25f),
                radius = outerRadius,
                center = center,
                style = Stroke(width = 2f)
            )

            // Middle pulsing energy ring
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.35f * pulseGlow),
                        Color.Transparent
                    ),
                    center = center,
                    radius = dynamicMidRadius * 1.3f
                ),
                radius = dynamicMidRadius,
                center = center
            )

            drawCircle(
                color = activeColor,
                radius = dynamicMidRadius,
                center = center,
                style = Stroke(width = 3.5f)
            )

            // Inner core reactor chamber
            drawCircle(
                color = DarkSurfaceVariant,
                radius = innerRadius,
                center = center
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        activeColor.copy(alpha = 0.8f),
                        activeColor.copy(alpha = 0.1f)
                    ),
                    center = center,
                    radius = innerRadius
                ),
                radius = innerRadius,
                center = center
            )

            drawCircle(
                color = activeColor,
                radius = innerRadius,
                center = center,
                style = Stroke(width = 4f)
            )

            // If first clap is detected, draw remaining countdown window sweep
            if (isFirstClapDetected && firstClapCountdown > 0f) {
                val sweep = 360f * firstClapCountdown
                drawArc(
                    color = ArcAmber,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - midRadius, center.y - midRadius),
                    size = Size(midRadius * 2, midRadius * 2),
                    style = Stroke(width = 6f, cap = StrokeCap.Round)
                )
            }
        }

        // Inner status label
        Text(
            text = when {
                isExecutingRoutine -> "ACTIVE"
                isFirstClapDetected -> "CLAP 2?"
                isListening -> "ARMED"
                else -> "STANDBY"
            },
            color = activeColor,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 2.sp
        )
    }
}
