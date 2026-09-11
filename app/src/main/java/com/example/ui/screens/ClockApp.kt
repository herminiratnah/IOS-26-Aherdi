package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AlarmItem
import com.example.model.WorldClockCity
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

enum class ClockTab {
    WORLD_CLOCK,
    ALARM,
    STOPWATCH,
    TIMER
}

@Composable
fun ClockApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadAlarms(context)
    }

    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val alarms by viewModel.alarms.collectAsState()
    val isStopwatchRunning by viewModel.isStopwatchRunning.collectAsState()
    val stopwatchTimeMs by viewModel.stopwatchTimeMs.collectAsState()
    val stopwatchLaps by viewModel.stopwatchLaps.collectAsState()
    val timerRemainingSeconds by viewModel.timerRemainingSeconds.collectAsState()
    val isTimerRunning by viewModel.isTimerRunning.collectAsState()

    val currentTimeFormatted by viewModel.currentTimeFormatted.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
    val cellularBars by viewModel.cellularSignalBars.collectAsState()
    val networkType by viewModel.networkType.collectAsState()

    var selectedTab by remember { mutableStateOf(ClockTab.WORLD_CLOCK) }

    // Real-time ticking clock
    var currentTime by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = Calendar.getInstance()
            delay(1000)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status bar (Light icons on black background)
            IOSStatusBar(
                dynamicIslandData = dynamicIslandData,
                onDismissDynamicIsland = { viewModel.dismissDynamicIsland() },
                onOpenControlCenter = { viewModel.toggleControlCenter() },
                onOpenNotificationCenter = { viewModel.toggleNotificationCenter() },
                currentTime = currentTimeFormatted,
                batteryLevel = batteryLevel,
                isBatteryCharging = isBatteryCharging,
                isWifiConnected = isWifiConnected,
                wifiSignalLevel = wifiSignalLevel,
                cellularBars = cellularBars,
                networkType = networkType,
                isDarkIcons = false
            )

            // Main Content based on Selected Tab
            Box(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    ClockTab.WORLD_CLOCK -> {
                        WorldClockView(currentTime = currentTime)
                    }
                    ClockTab.ALARM -> {
                        AlarmView(
                            alarms = alarms,
                            onToggleAlarm = { viewModel.toggleAlarm(it, context) },
                            onAddAlarm = { time, period, label -> viewModel.addAlarm(time, period, label, context) },
                            onDeleteAlarm = { viewModel.deleteAlarm(it, context) }
                        )
                    }
                    ClockTab.STOPWATCH -> {
                        StopwatchView(
                            isRunning = isStopwatchRunning,
                            timeMs = stopwatchTimeMs,
                            laps = stopwatchLaps,
                            onStart = { viewModel.startStopwatch() },
                            onStop = { viewModel.stopStopwatch() },
                            onReset = { viewModel.resetStopwatch() },
                            onLap = { viewModel.lapStopwatch() }
                        )
                    }
                    ClockTab.TIMER -> {
                        TimerView(
                            remainingSeconds = timerRemainingSeconds,
                            isRunning = isTimerRunning,
                            onStart = { viewModel.startTimer(it) },
                            onStop = { viewModel.stopTimer() }
                        )
                    }
                }
            }

            // Bottom Navigation Bar (World Clock, Alarm, Stopwatch, Timer)
            HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF161618))
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ClockBottomTabItem(
                    icon = Icons.Rounded.Public,
                    label = "World Clock",
                    isSelected = selectedTab == ClockTab.WORLD_CLOCK,
                    onClick = { selectedTab = ClockTab.WORLD_CLOCK }
                )
                ClockBottomTabItem(
                    icon = Icons.Rounded.Alarm,
                    label = "Alarm",
                    isSelected = selectedTab == ClockTab.ALARM,
                    onClick = { selectedTab = ClockTab.ALARM }
                )
                ClockBottomTabItem(
                    icon = Icons.Rounded.Timer,
                    label = "Stopwatch",
                    isSelected = selectedTab == ClockTab.STOPWATCH,
                    onClick = { selectedTab = ClockTab.STOPWATCH }
                )
                ClockBottomTabItem(
                    icon = Icons.Rounded.HourglassBottom,
                    label = "Timer",
                    isSelected = selectedTab == ClockTab.TIMER,
                    onClick = { selectedTab = ClockTab.TIMER }
                )
            }

            // Home indicator
            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = false
            )
        }
    }
}

@Composable
fun WorldClockView(currentTime: Calendar) {
    val cities = remember {
        listOf(
            WorldClockCity("Jakarta", "Today, +0HRS", "9:41", 0),
            WorldClockCity("Tokyo", "Today, +2HRS", "11:41", 2),
            WorldClockCity("New York", "Today, -11HRS", "22:41", -11),
            WorldClockCity("London", "Today, -6HRS", "03:41", -6)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Edit", color = IOSOrange, fontSize = 17.sp)
            Text("Clock", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.Rounded.Add, contentDescription = "Add", tint = IOSOrange, modifier = Modifier.size(24.dp))
        }

        // Live Analog Clock Face matching Image 5
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Analog Watch Dial
                Box(
                    modifier = Modifier.size(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val radius = size.minDimension / 2
                        val center = Offset(size.width / 2, size.height / 2)

                        // Outer dial border
                        drawCircle(
                            color = Color(0xFF2C2C2E),
                            radius = radius,
                            center = center,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                        )

                        // 12 hour ticks
                        for (i in 0 until 12) {
                            val angle = (i * 30.0 - 90.0) * Math.PI / 180.0
                            val tickStart = Offset(
                                (center.x + (radius - 12.dp.toPx()) * cos(angle)).toFloat(),
                                (center.y + (radius - 12.dp.toPx()) * sin(angle)).toFloat()
                            )
                            val tickEnd = Offset(
                                (center.x + (radius - 4.dp.toPx()) * cos(angle)).toFloat(),
                                (center.y + (radius - 4.dp.toPx()) * sin(angle)).toFloat()
                            )
                            drawLine(
                                color = if (i % 3 == 0) Color.White else Color.Gray,
                                start = tickStart,
                                end = tickEnd,
                                strokeWidth = if (i % 3 == 0) 2.5f else 1.5f,
                                cap = StrokeCap.Round
                            )
                        }

                        val hours = currentTime.get(Calendar.HOUR)
                        val minutes = currentTime.get(Calendar.MINUTE)
                        val seconds = currentTime.get(Calendar.SECOND)

                        // Hour Hand
                        val hourAngle = ((hours + minutes / 60.0) * 30.0 - 90.0) * Math.PI / 180.0
                        val hourEnd = Offset(
                            (center.x + (radius * 0.5f) * cos(hourAngle)).toFloat(),
                            (center.y + (radius * 0.5f) * sin(hourAngle)).toFloat()
                        )
                        drawLine(
                            color = Color.White,
                            start = center,
                            end = hourEnd,
                            strokeWidth = 4.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Minute Hand
                        val minAngle = (minutes * 6.0 - 90.0) * Math.PI / 180.0
                        val minEnd = Offset(
                            (center.x + (radius * 0.72f) * cos(minAngle)).toFloat(),
                            (center.y + (radius * 0.72f) * sin(minAngle)).toFloat()
                        )
                        drawLine(
                            color = Color.White,
                            start = center,
                            end = minEnd,
                            strokeWidth = 2.8.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Second Hand (Orange)
                        val secAngle = (seconds * 6.0 - 90.0) * Math.PI / 180.0
                        val secEnd = Offset(
                            (center.x + (radius * 0.82f) * cos(secAngle)).toFloat(),
                            (center.y + (radius * 0.82f) * sin(secAngle)).toFloat()
                        )
                        drawLine(
                            color = Color(0xFFFF9500),
                            start = center,
                            end = secEnd,
                            strokeWidth = 1.6.dp.toPx(),
                            cap = StrokeCap.Round
                        )

                        // Center pin dot
                        drawCircle(color = Color(0xFFFF9500), radius = 3.dp.toPx(), center = center)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Jakarta",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Today, +0HRS",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }
        }

        // City World Clocks List matching Image 5
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(cities) { city ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(city.timeDiff, color = Color.Gray, fontSize = 13.sp)
                        Text(city.name, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Normal)
                    }
                    Text(
                        text = city.timeFormatted,
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1).sp
                    )
                }
                HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun AlarmView(
    alarms: List<AlarmItem>,
    onToggleAlarm: (String) -> Unit,
    onAddAlarm: (String, String, String) -> Unit,
    onDeleteAlarm: (String) -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var newHour by remember { mutableStateOf("07") }
    var newMinute by remember { mutableStateOf("00") }
    var newPeriod by remember { mutableStateOf("AM") }
    var newLabel by remember { mutableStateOf("Alarm") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isEditing) "Done" else "Edit",
                color = IOSOrange,
                fontSize = 17.sp,
                modifier = Modifier.clickable { isEditing = !isEditing }
            )
            Text("Alarm", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Icon(
                Icons.Rounded.Add,
                contentDescription = "Add Alarm",
                tint = IOSOrange,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { showAddDialog = true }
            )
        }

        if (alarms.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text("No Alarms", color = Color.Gray, fontSize = 17.sp)
            }
        } else {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(alarms, key = { it.id }) { alarm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isEditing) {
                            IconButton(
                                onClick = { onDeleteAlarm(alarm.id) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.RemoveCircle,
                                    contentDescription = "Delete Alarm",
                                    tint = Color(0xFFFF3B30)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = alarm.timeLabel,
                                    color = if (alarm.isEnabled) Color.White else Color.Gray,
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Light
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = alarm.period,
                                    color = if (alarm.isEnabled) Color.White else Color.Gray,
                                    fontSize = 22.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Text(alarm.label, color = Color.Gray, fontSize = 14.sp)
                        }

                        Switch(
                            checked = alarm.isEnabled,
                            onCheckedChange = { onToggleAlarm(alarm.id) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = IOSGreen,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Color(0xFF39393D)
                            )
                        )
                    }
                    HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
                }
            }
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = Color(0xFF1C1C1E),
            title = {
                Text("Add Alarm", color = Color.White, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newHour,
                            onValueChange = { if (it.length <= 2) newHour = it },
                            label = { Text("Hour", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = IOSOrange
                            ),
                            modifier = Modifier.width(80.dp)
                        )
                        Text(":", color = Color.White, fontSize = 28.sp)
                        OutlinedTextField(
                            value = newMinute,
                            onValueChange = { if (it.length <= 2) newMinute = it },
                            label = { Text("Min", color = Color.Gray) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = IOSOrange
                            ),
                            modifier = Modifier.width(80.dp)
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2C2C2E))
                        ) {
                            Text(
                                text = "AM",
                                color = if (newPeriod == "AM") Color.Black else Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (newPeriod == "AM") IOSOrange else Color.Transparent)
                                    .clickable { newPeriod = "AM" }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "PM",
                                color = if (newPeriod == "PM") Color.Black else Color.White,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (newPeriod == "PM") IOSOrange else Color.Transparent)
                                    .clickable { newPeriod = "PM" }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    OutlinedTextField(
                        value = newLabel,
                        onValueChange = { newLabel = it },
                        label = { Text("Label", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = IOSOrange
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val h = newHour.padStart(2, '0')
                        val m = newMinute.padStart(2, '0')
                        onAddAlarm("$h:$m", newPeriod, newLabel.ifBlank { "Alarm" })
                        showAddDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = IOSOrange)
                ) {
                    Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
    }
}

@Composable
fun StopwatchView(
    isRunning: Boolean,
    timeMs: Long,
    laps: List<String>,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onReset: () -> Unit,
    onLap: () -> Unit
) {
    val minutes = (timeMs / 1000) / 60
    val seconds = (timeMs / 1000) % 60
    val centis = (timeMs % 1000) / 10
    val formattedTime = String.format("%02d:%02d.%02d", minutes, seconds, centis)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = formattedTime,
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Thin,
            letterSpacing = (-1).sp
        )

        Spacer(modifier = Modifier.height(30.dp))

        // Start/Stop & Lap/Reset Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Lap / Reset Button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333336))
                    .clickable {
                        if (isRunning) onLap() else onReset()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRunning) "Lap" else "Reset",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            // Start / Stop Button
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) Color(0x33FF3B30) else Color(0x3334C759))
                    .clickable {
                        if (isRunning) onStop() else onStart()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRunning) "Stop" else "Start",
                    color = if (isRunning) IOSRed else IOSGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)

        // Laps List
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(laps.size) { index ->
                val lapTime = laps[index]
                val lapNum = laps.size - index
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Lap $lapNum", color = Color.LightGray, fontSize = 16.sp)
                    Text(lapTime, color = Color.White, fontSize = 16.sp)
                }
                HorizontalDivider(color = Color(0xFF2C2C2E), thickness = 0.5.dp)
            }
        }
    }
}

@Composable
fun TimerView(
    remainingSeconds: Int,
    isRunning: Boolean,
    onStart: (Int) -> Unit,
    onStop: () -> Unit
) {
    val m = remainingSeconds / 60
    val s = remainingSeconds % 60
    val formatted = String.format("%02d:%02d", m, s)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isRunning) formatted else "05:00",
            color = Color.White,
            fontSize = 72.sp,
            fontWeight = FontWeight.Thin
        )

        Spacer(modifier = Modifier.height(40.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF333336))
                    .clickable { onStop() },
                contentAlignment = Alignment.Center
            ) {
                Text("Cancel", color = Color.White, fontSize = 16.sp)
            }

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(if (isRunning) Color(0x33FF9500) else Color(0x3334C759))
                    .clickable {
                        if (isRunning) onStop() else onStart(300) // 5 mins
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isRunning) "Pause" else "Start",
                    color = if (isRunning) IOSOrange else IOSGreen,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun ClockBottomTabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) IOSOrange else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) IOSOrange else Color.Gray
        )
    }
}
