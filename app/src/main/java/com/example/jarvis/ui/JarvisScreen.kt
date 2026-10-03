package com.example.jarvis.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.jarvis.ui.components.ArcReactorVisualizer
import com.example.jarvis.ui.components.AudioLevelMeter
import com.example.jarvis.ui.components.EventHistoryList
import com.example.jarvis.ui.components.RoutineConfigView
import com.example.jarvis.ui.components.RoutineExecutionView
import com.example.jarvis.ui.components.TuningSlidersView
import com.example.jarvis.ui.theme.ArcAmber
import com.example.jarvis.ui.theme.ArcBlue
import com.example.jarvis.ui.theme.ArcCyan
import com.example.jarvis.ui.theme.ArcGreen
import com.example.jarvis.ui.theme.ArcRed
import com.example.jarvis.ui.theme.DarkBackground
import com.example.jarvis.ui.theme.DarkBorder
import com.example.jarvis.ui.theme.DarkSurface
import com.example.jarvis.ui.theme.DarkSurfaceElevated
import com.example.jarvis.ui.theme.DarkSurfaceVariant
import com.example.jarvis.ui.theme.TextPrimary
import com.example.jarvis.ui.theme.TextSecondary
import com.example.jarvis.ui.theme.TextTertiary

sealed class ScreenTab(val title: String, val icon: ImageVector) {
    object Hud : ScreenTab("HUD", Icons.Default.Home)
    object Routine : ScreenTab("Actions", Icons.Default.PlayArrow)
    object Tuning : ScreenTab("Tuning", Icons.Default.Settings)
    object Settings : ScreenTab("Settings", Icons.Default.Edit)
}

@Composable
fun JarvisScreen(
    viewModel: JarvisViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var hasMicPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasMicPermission = isGranted
        if (isGranted) {
            viewModel.startListening()
        }
    }

    val telemetry by viewModel.telemetry.collectAsState()
    val routineState by viewModel.routineExecutionState.collectAsState()
    val config by viewModel.config.collectAsState()
    val history by viewModel.history.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(ScreenTab.Hud, ScreenTab.Routine, ScreenTab.Tuning, ScreenTab.Settings)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            JarvisTopHeader(
                isListening = telemetry.isListening,
                isArmed = telemetry.isArmed,
                isExecuting = routineState.isRunning
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                tonalElevation = 8.dp
            ) {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = tab.icon,
                                contentDescription = tab.title
                            )
                        },
                        label = {
                            Text(
                                text = tab.title,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DarkBackground,
                            selectedTextColor = ArcCyan,
                            indicatorColor = ArcCyan,
                            unselectedIconColor = TextTertiary,
                            unselectedTextColor = TextSecondary
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTabIndex) {
                0 -> HudTabContent(
                    telemetry = telemetry,
                    routineState = routineState,
                    hasMicPermission = hasMicPermission,
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onToggleListening = {
                        if (telemetry.isListening) {
                            viewModel.stopListening()
                        } else {
                            if (hasMicPermission) {
                                viewModel.startListening()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    onTriggerRoutineNow = { viewModel.triggerRoutineNow() }
                )
                1 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RoutineExecutionView(
                        state = routineState,
                        onTriggerNow = { viewModel.triggerRoutineNow() }
                    )
                    EventHistoryList(
                        events = history,
                        onClearHistory = { viewModel.clearHistory() }
                    )
                }
                2 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TuningSlidersView(
                        config = config,
                        onConfigChange = { viewModel.updateConfig(it) }
                    )
                    AudioLevelMeter(
                        rms = telemetry.rms,
                        noiseFloor = telemetry.noiseFloor,
                        threshold = telemetry.threshold
                    )
                }
                3 -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    RoutineConfigView(
                        config = config,
                        onConfigChange = { viewModel.updateConfig(it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun JarvisTopHeader(
    isListening: Boolean,
    isArmed: Boolean,
    isExecuting: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .border(1.dp, DarkBorder.copy(alpha = 0.5f))
            .padding(horizontal = 20.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isExecuting -> ArcGreen
                            isListening -> ArcCyan
                            else -> ArcRed
                        }
                    )
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "J.A.R.V.I.S.",
                    color = ArcCyan,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 2.sp
                )
                Text(
                    text = "CLAP AUTOMATION SYSTEM",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
        }

        // Status Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    when {
                        isExecuting -> ArcGreen.copy(alpha = 0.2f)
                        isListening -> ArcCyan.copy(alpha = 0.2f)
                        else -> DarkSurfaceElevated
                    }
                )
                .border(
                    1.dp,
                    when {
                        isExecuting -> ArcGreen
                        isListening -> ArcCyan
                        else -> DarkBorder
                    },
                    RoundedCornerShape(20.dp)
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = when {
                    isExecuting -> "RUNNING"
                    isListening -> "ARMED"
                    else -> "STANDBY"
                },
                color = when {
                    isExecuting -> ArcGreen
                    isListening -> ArcCyan
                    else -> TextTertiary
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun HudTabContent(
    telemetry: com.example.jarvis.audio.ClapDetector.Telemetry,
    routineState: com.example.jarvis.routine.JarvisRoutineRunner.RoutineExecutionState,
    hasMicPermission: Boolean,
    onRequestPermission: () -> Unit,
    onToggleListening: () -> Unit,
    onTriggerRoutineNow: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(
                    when {
                        routineState.isRunning -> ArcGreen.copy(alpha = 0.15f)
                        telemetry.isFirstClapDetected -> ArcAmber.copy(alpha = 0.2f)
                        telemetry.isListening -> ArcCyan.copy(alpha = 0.12f)
                        else -> DarkSurfaceElevated
                    }
                )
                .border(
                    1.dp,
                    when {
                        routineState.isRunning -> ArcGreen.copy(alpha = 0.5f)
                        telemetry.isFirstClapDetected -> ArcAmber
                        telemetry.isListening -> ArcCyan.copy(alpha = 0.3f)
                        else -> DarkBorder
                    },
                    RoundedCornerShape(12.dp)
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    routineState.isRunning -> "PROTOCOLS ACTIVE: EXECUTING"
                    telemetry.isFirstClapDetected -> "FIRST CLAP DETECTED! AWAITING SECOND CLAP..."
                    telemetry.isListening -> "LISTENING FOR DOUBLE CLAP"
                    else -> "MICROPHONE STANDBY (CLICK START TO LISTEN)"
                },
                color = when {
                    routineState.isRunning -> ArcGreen
                    telemetry.isFirstClapDetected -> ArcAmber
                    telemetry.isListening -> ArcCyan
                    else -> TextSecondary
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 1.sp
            )
        }

        // Arc Reactor HUD Core
        ArcReactorVisualizer(
            rms = telemetry.rms,
            isListening = telemetry.isListening,
            isFirstClapDetected = telemetry.isFirstClapDetected,
            firstClapCountdown = telemetry.firstClapCountdown,
            isExecutingRoutine = routineState.isRunning
        )

        // Live Audio Spectrum Meter
        AudioLevelMeter(
            rms = telemetry.rms,
            noiseFloor = telemetry.noiseFloor,
            threshold = telemetry.threshold
        )

        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasMicPermission) {
                Button(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ArcAmber,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("grant_permission_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("GRANT MIC ACCESS", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onToggleListening,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (telemetry.isListening) ArcRed else ArcCyan,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("toggle_listening_button")
                ) {
                    Icon(
                        imageVector = if (telemetry.isListening) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (telemetry.isListening) "STOP LISTENING" else "START LISTENING",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Button(
                onClick = onTriggerRoutineNow,
                enabled = !routineState.isRunning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkSurfaceElevated,
                    contentColor = ArcCyan,
                    disabledContainerColor = DarkSurfaceVariant,
                    disabledContentColor = TextTertiary
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(0.9f)
                    .height(52.dp)
                    .border(1.dp, ArcCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .testTag("sim_double_clap_button")
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = ArcCyan
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RUN SEQUENCE",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Mini Routine preview
        RoutineExecutionView(
            state = routineState,
            onTriggerNow = onTriggerRoutineNow
        )
    }
}
