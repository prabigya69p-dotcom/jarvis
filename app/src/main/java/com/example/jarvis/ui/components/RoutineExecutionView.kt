package com.example.jarvis.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.routine.JarvisRoutineRunner
import com.example.jarvis.ui.theme.ArcAmber
import com.example.jarvis.ui.theme.ArcBlue
import com.example.jarvis.ui.theme.ArcCyan
import com.example.jarvis.ui.theme.ArcGreen
import com.example.jarvis.ui.theme.ArcRed
import com.example.jarvis.ui.theme.DarkBackground
import com.example.jarvis.ui.theme.DarkBorder
import com.example.jarvis.ui.theme.DarkSurface
import com.example.jarvis.ui.theme.DarkSurfaceElevated
import com.example.jarvis.ui.theme.TextPrimary
import com.example.jarvis.ui.theme.TextSecondary
import com.example.jarvis.ui.theme.TextTertiary

@Composable
fun RoutineExecutionView(
    state: JarvisRoutineRunner.RoutineExecutionState,
    onTriggerNow: () -> Unit,
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
            Column {
                Text(
                    text = "JARVIS AUTOMATION PROTOCOL",
                    color = ArcCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Text(
                    text = state.currentStepTitle,
                    color = if (state.isRunning) ArcAmber else TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = onTriggerNow,
                enabled = !state.isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArcCyan,
                    contentColor = DarkBackground,
                    disabledContainerColor = DarkSurfaceElevated,
                    disabledContentColor = TextTertiary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("trigger_routine_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Trigger Routine",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (state.isRunning) "RUNNING" else "EXECUTE",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Steps list
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.steps.forEach { step ->
                RoutineStepRow(step = step)
            }
        }

        AnimatedVisibility(
            visible = state.completionMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(ArcGreen.copy(alpha = 0.15f))
                    .border(1.dp, ArcGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.completionMessage ?: "",
                    color = ArcGreen,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun RoutineStepRow(step: JarvisRoutineRunner.RoutineStep) {
    val icon = when (step.id) {
        "music" -> Icons.Default.PlayArrow
        "voice" -> Icons.Default.Notifications
        "claude" -> Icons.Default.Star
        "crypto" -> Icons.Default.Share
        "dev" -> Icons.Default.Settings
        else -> Icons.Default.Info
    }

    val (statusColor, statusText) = when (step.status) {
        JarvisRoutineRunner.StepStatus.PENDING -> Pair(TextTertiary, "STANDBY")
        JarvisRoutineRunner.StepStatus.RUNNING -> Pair(ArcAmber, "RUNNING")
        JarvisRoutineRunner.StepStatus.COMPLETED -> Pair(ArcGreen, "DONE")
        JarvisRoutineRunner.StepStatus.SKIPPED -> Pair(TextTertiary, "SKIPPED")
        JarvisRoutineRunner.StepStatus.FAILED -> Pair(ArcRed, "FAILED")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceElevated)
            .border(1.dp, DarkBorder.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = 0.18f))
                .border(1.dp, statusColor.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = step.title,
                tint = statusColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = step.description,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (step.status == JarvisRoutineRunner.StepStatus.RUNNING) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = ArcAmber,
                strokeWidth = 2.dp
            )
        } else if (step.status == JarvisRoutineRunner.StepStatus.COMPLETED) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Completed",
                tint = ArcGreen,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Text(
                text = statusText,
                color = statusColor,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
