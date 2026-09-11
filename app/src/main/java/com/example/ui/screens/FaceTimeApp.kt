package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

data class FaceTimeContact(
    val id: String,
    val name: String,
    val time: String,
    val isVideo: Boolean = true,
    val avatarBg: Long = 0xFF34C759
)

@Composable
fun FaceTimeApp(
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

    var isInVideoCall by remember { mutableStateOf(false) }
    var activeCallName by remember { mutableStateOf("") }
    var isMuted by remember { mutableStateOf(false) }
    var isCameraOff by remember { mutableStateOf(false) }
    var showNewCallDialog by remember { mutableStateOf(false) }
    var newCallRecipient by remember { mutableStateOf("") }

    val recentCalls = remember {
        mutableStateListOf(
            FaceTimeContact("1", "Sarah Jenkins", "Yesterday", true, 0xFF5856D6),
            FaceTimeContact("2", "Alex Rivera", "Monday", true, 0xFF007AFF),
            FaceTimeContact("3", "David Kim", "14/09/2026", false, 0xFFFF9500),
            FaceTimeContact("4", "Family Group", "10/09/2026", true, 0xFFFF2D55),
            FaceTimeContact("5", "Elena Rostova", "08/09/2026", true, 0xFF34C759)
        )
    }

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

            if (isInVideoCall) {
                // Active FaceTime Video Call Screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color(0xFF1C1C1E), Color(0xFF000000))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeCallName.take(1),
                                color = Color.White,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = activeCallName,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = if (isCameraOff) "Camera Paused" else "FaceTime Video (HD 60fps)",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )

                        Spacer(modifier = Modifier.height(40.dp))

                        // Controls Row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(24.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Mute
                            IconButton(
                                onClick = { isMuted = !isMuted },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (isMuted) Color.White else Color(0x4DFFFFFF))
                            ) {
                                Icon(
                                    imageVector = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                                    contentDescription = "Mute",
                                    tint = if (isMuted) Color.Black else Color.White
                                )
                            }

                            // Camera toggle
                            IconButton(
                                onClick = { isCameraOff = !isCameraOff },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(if (isCameraOff) Color.White else Color(0x4DFFFFFF))
                            ) {
                                Icon(
                                    imageVector = if (isCameraOff) Icons.Rounded.VideocamOff else Icons.Rounded.Videocam,
                                    contentDescription = "Camera",
                                    tint = if (isCameraOff) Color.Black else Color.White
                                )
                            }

                            // End Call
                            IconButton(
                                onClick = {
                                    isInVideoCall = false
                                    viewModel.dismissDynamicIsland()
                                },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFF3B30))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.CallEnd,
                                    contentDescription = "End Call",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            } else {
                // FaceTime Main Screen
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    // Top header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Edit",
                            color = Color(0xFF34C759),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "FaceTime",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = { showNewCallDialog = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = "New Call",
                                tint = Color(0xFF34C759)
                            )
                        }
                    }

                    // Action Buttons: Create Link & New FaceTime
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Create Link
                        Button(
                            onClick = {
                                viewModel.showDynamicIslandNotification("FaceTime Link Created", "facetime.apple.com/join/91823", "link")
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Link,
                                    contentDescription = null,
                                    tint = Color(0xFF34C759),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("Create Link", color = Color(0xFF34C759), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // New FaceTime
                        Button(
                            onClick = { showNewCallDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Videocam,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text("New FaceTime", color = Color.White, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "RECENTS",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(recentCalls) { call ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF1C1C1E))
                                    .clickable {
                                        activeCallName = call.name
                                        isInVideoCall = true
                                        viewModel.showDynamicIslandNotification("FaceTime Call", "Connecting to ${call.name}...", "video")
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(Color(call.avatarBg)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = call.name.take(1),
                                        color = Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = call.name,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "${if (call.isVideo) "FaceTime Video" else "FaceTime Audio"} • ${call.time}",
                                        color = Color.Gray,
                                        fontSize = 13.sp
                                    )
                                }

                                Icon(
                                    imageVector = if (call.isVideo) Icons.Rounded.Videocam else Icons.Rounded.Call,
                                    contentDescription = "Call",
                                    tint = Color(0xFF34C759),
                                    modifier = Modifier.size(22.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = "Info",
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Dialog for new call
            if (showNewCallDialog) {
                AlertDialog(
                    onDismissRequest = { showNewCallDialog = false },
                    title = { Text("New FaceTime", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Enter name, email, or phone number", color = Color.Gray, fontSize = 13.sp)
                            OutlinedTextField(
                                value = newCallRecipient,
                                onValueChange = { newCallRecipient = it },
                                placeholder = { Text("Name or Phone") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newCallRecipient.isNotBlank()) {
                                    activeCallName = newCallRecipient
                                    isInVideoCall = true
                                    showNewCallDialog = false
                                    viewModel.showDynamicIslandNotification("FaceTime Call", "Calling $newCallRecipient...", "video")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759))
                        ) {
                            Text("FaceTime")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showNewCallDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    },
                    containerColor = Color(0xFF1C1C1E)
                )
            }

            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = false
            )
        }
    }
}
