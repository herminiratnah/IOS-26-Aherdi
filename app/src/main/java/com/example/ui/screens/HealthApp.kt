package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

@Composable
fun HealthApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
    val cellularBars by viewModel.cellularSignalBars.collectAsState()
    val networkType by viewModel.networkType.collectAsState()

    var selectedTab by remember { mutableStateOf("Summary") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            IOSStatusBar(
                dynamicIslandData = dynamicIslandData,
                onDismissDynamicIsland = { viewModel.dismissDynamicIsland() },
                onOpenControlCenter = { viewModel.toggleControlCenter() },
                onOpenNotificationCenter = { viewModel.toggleNotificationCenter() },
                currentTime = currentTime,
                batteryLevel = batteryLevel,
                isBatteryCharging = isBatteryCharging,
                isWifiConnected = isWifiConnected,
                wifiSignalLevel = wifiSignalLevel,
                cellularBars = cellularBars,
                networkType = networkType,
                isDarkIcons = false
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WEDNESDAY, 15 JUL",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Summary",
                        color = Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2C2E)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Activity Rings Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF1C1C1E))
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Activity",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF2D55)))
                                    Text("Move: 512 / 600 kcal", color = Color.White, fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF34C759)))
                                    Text("Exercise: 34 / 30 min", color = Color.White, fontSize = 13.sp)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF007AFF)))
                                    Text("Stand: 11 / 12 hr", color = Color.White, fontSize = 13.sp)
                                }
                            }

                            // Radial Activity Rings Canvas
                            Canvas(modifier = Modifier.size(100.dp)) {
                                val strokeWidth = 10.dp.toPx()
                                val center = Offset(size.width / 2, size.height / 2)

                                // Move Ring (Red)
                                drawArc(
                                    color = Color(0x33FF2D55),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = Color(0xFFFF2D55),
                                    startAngle = -90f,
                                    sweepAngle = 310f,
                                    useCenter = false,
                                    topLeft = Offset(strokeWidth / 2, strokeWidth / 2),
                                    size = Size(size.width - strokeWidth, size.height - strokeWidth),
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )

                                // Exercise Ring (Green)
                                val pad1 = strokeWidth + 3.dp.toPx()
                                drawArc(
                                    color = Color(0x3334C759),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(pad1, pad1),
                                    size = Size(size.width - pad1 * 2, size.height - pad1 * 2),
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = Color(0xFF34C759),
                                    startAngle = -90f,
                                    sweepAngle = 380f,
                                    useCenter = false,
                                    topLeft = Offset(pad1, pad1),
                                    size = Size(size.width - pad1 * 2, size.height - pad1 * 2),
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )

                                // Stand Ring (Blue)
                                val pad2 = strokeWidth * 2 + 6.dp.toPx()
                                drawArc(
                                    color = Color(0x33007AFF),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    topLeft = Offset(pad2, pad2),
                                    size = Size(size.width - pad2 * 2, size.height - pad2 * 2),
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = Color(0xFF007AFF),
                                    startAngle = -90f,
                                    sweepAngle = 330f,
                                    useCenter = false,
                                    topLeft = Offset(pad2, pad2),
                                    size = Size(size.width - pad2 * 2, size.height - pad2 * 2),
                                    style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                        }
                    }
                }

                // Steps Card
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Steps
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Steps", color = Color(0xFFFF9500), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Rounded.DirectionsWalk, contentDescription = null, tint = Color(0xFFFF9500), modifier = Modifier.size(16.dp))
                                }
                                Text("8,421", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text("steps today", color = Color.Gray, fontSize = 11.sp)
                            }
                        }

                        // Heart Rate
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Heart Rate", color = Color(0xFFFF2D55), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color(0xFFFF2D55), modifier = Modifier.size(16.dp))
                                }
                                Text("68 BPM", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text("Resting: 62 BPM", color = Color.Gray, fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Sleep Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF1C1C1E))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Sleep", color = Color(0xFF32ADE6), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = Color(0xFF32ADE6), modifier = Modifier.size(18.dp))
                            }
                            Text("7 hr 42 min", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                            Text("Time asleep in target zone • 11:15 PM – 7:02 AM", color = Color.Gray, fontSize = 12.sp)

                            // Sleep Stages Bar
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(RoundedCornerShape(5.dp))
                            ) {
                                Box(modifier = Modifier.weight(0.2f).fillMaxHeight().background(Color(0xFF5856D6))) // Deep
                                Box(modifier = Modifier.weight(0.45f).fillMaxHeight().background(Color(0xFF32ADE6))) // Core
                                Box(modifier = Modifier.weight(0.25f).fillMaxHeight().background(Color(0xFF007AFF))) // REM
                                Box(modifier = Modifier.weight(0.1f).fillMaxHeight().background(Color(0xFFFF9500))) // Awake
                            }
                        }
                    }
                }

                // Trends
                item {
                    Text(
                        text = "TRENDS",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1C1C1E))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.TrendingUp, contentDescription = null, tint = Color(0xFF34C759))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Walking Distance", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                            Text("You're averaging 5.8 km a day, up from 4.9 km last month.", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = false
            )
        }
    }
}
