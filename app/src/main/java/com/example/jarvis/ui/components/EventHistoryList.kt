package com.example.jarvis.ui.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.model.ClapEvent
import com.example.jarvis.ui.theme.ArcCyan
import com.example.jarvis.ui.theme.ArcGreen
import com.example.jarvis.ui.theme.DarkBorder
import com.example.jarvis.ui.theme.DarkSurface
import com.example.jarvis.ui.theme.DarkSurfaceElevated
import com.example.jarvis.ui.theme.TextPrimary
import com.example.jarvis.ui.theme.TextSecondary
import com.example.jarvis.ui.theme.TextTertiary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventHistoryList(
    events: List<ClapEvent>,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = null,
                    tint = ArcCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "DETECTION EVENT LOG",
                    color = ArcCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }

            if (events.isNotEmpty()) {
                IconButton(onClick = onClearHistory) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear History",
                        tint = TextTertiary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (events.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TextTertiary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No clap events recorded yet.",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Double-clap near the mic to trigger.",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.height(260.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events, key = { it.id }) { event ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .border(1.dp, DarkBorder.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(ArcGreen)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "DOUBLE CLAP DETECTED",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "Gap: %d ms | Peak RMS: %.3f",
                                        (event.gapSeconds * 1000).toInt(),
                                        event.peakRms
                                    ),
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Text(
                            text = timeFormat.format(Date(event.timestamp)),
                            color = ArcCyan,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}
