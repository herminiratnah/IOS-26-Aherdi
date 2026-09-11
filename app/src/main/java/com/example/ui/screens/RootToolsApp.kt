package com.example.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

@Composable
fun RootToolsApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val isRootAvailable by viewModel.isRootAvailable.collectAsState()
    val isImmersiveLocked by viewModel.isImmersiveLocked.collectAsState()
    val cpuBoostEnabled by viewModel.cpuBoostEnabled.collectAsState()
    val spoofedModel by viewModel.spoofedModel.collectAsState()
    val rootLogs by viewModel.rootLogs.collectAsState()

    var customShellCommand by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0C0C0E))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status bar
            IOSStatusBar(
                dynamicIslandData = dynamicIslandData,
                onDismissDynamicIsland = { viewModel.dismissDynamicIsland() },
                onOpenControlCenter = { viewModel.toggleControlCenter() },
                onOpenNotificationCenter = { viewModel.toggleNotificationCenter() },
                isDarkIcons = false
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "iRoot SE Powerhouse",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Samsung Galaxy J2 Prime -> iPhone SE 2 iOS 26",
                                fontSize = 13.sp,
                                color = IOSMint
                            )
                        }

                        // Root Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isRootAvailable) Color(0x3334C759) else Color(0x33FF3B30))
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isRootAvailable) "ROOT #SU ACTIVE" else "ROOT SIMULATED",
                                color = if (isRootAvailable) IOSGreen else IOSRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 1-Tap Transformation Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF2C1052))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = IOSYellow, modifier = Modifier.size(24.dp))
                                Text("1-Tap iPhone KW Transformation", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Kills Android Status & Nav bars, clears J2 Prime RAM, sets 320 Retina DPI, and spoofs iPhone SE 2nd Gen.",
                                color = Color.LightGray,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Button(
                                onClick = {
                                    viewModel.toggleImmersiveRoot()
                                    viewModel.cleanJ2PrimeRamRoot()
                                    viewModel.setRetinaDpiRoot(320)
                                    viewModel.spoofDeviceRoot("iPhone SE (2nd generation)")
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IOSPurple),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Execute Full Transform", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Power Tools Cards
                item {
                    Text("SYSTEM MODIFICATIONS", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                // Immersive Mode
                item {
                    RootActionTile(
                        icon = Icons.Rounded.Fullscreen,
                        iconColor = IOSBlue,
                        title = "Kill Android Status & Nav Bars",
                        subtitle = "policy_control immersive.full=*",
                        buttonText = if (isImmersiveLocked) "Active" else "Enable",
                        buttonBg = if (isImmersiveLocked) IOSGreen else IOSBlue,
                        onClick = { viewModel.toggleImmersiveRoot() }
                    )
                }

                // Total SystemUI Kill & Disable
                item {
                    RootActionTile(
                        icon = Icons.Rounded.Security,
                        iconColor = IOSRed,
                        title = "Nonaktifkan SystemUI Total (Magisk Root)",
                        subtitle = "pm disable com.android.systemui (Hapus statusbar bawaan total)",
                        buttonText = "Kill Total",
                        buttonBg = IOSRed,
                        onClick = { viewModel.disableSystemUICompletelyRoot() }
                    )
                }

                // Dynamic Island Notification Intercept Root Grant
                item {
                    RootActionTile(
                        icon = Icons.Rounded.NotificationsActive,
                        iconColor = IOSGreen,
                        title = "Izinkan Dynamic Island Notif via Root",
                        subtitle = "Beri akses notifikasi ke WhatsApp & pihak ke-3 tanpa pop-up",
                        buttonText = "Grant #su",
                        buttonBg = IOSGreen,
                        onClick = { viewModel.grantNotificationListenerViaRoot(context) }
                    )
                }

                // Open System Notification Access Settings
                item {
                    RootActionTile(
                        icon = Icons.Rounded.SettingsApplications,
                        iconColor = IOSPurple,
                        title = "Buka Pengaturan Izin Notifikasi HP",
                        subtitle = "Menu Akses Notifikasi Android bawaan untuk verifikasi",
                        buttonText = "Buka",
                        buttonBg = IOSPurple,
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                viewModel.showDynamicIslandNotification("Pengaturan", "Buka Pengaturan -> Notifikasi", "bell", 0xFFFF9500)
                            }
                        }
                    )
                }

                // RAM Cache Drop
                item {
                    RootActionTile(
                        icon = Icons.Rounded.Memory,
                        iconColor = IOSMint,
                        title = "J2 Prime RAM Cache Cleaner",
                        subtitle = "echo 3 > /proc/sys/vm/drop_caches",
                        buttonText = "Free RAM",
                        buttonBg = IOSMint,
                        onClick = { viewModel.cleanJ2PrimeRamRoot() }
                    )
                }

                // CPU Boost
                item {
                    RootActionTile(
                        icon = Icons.Rounded.Speed,
                        iconColor = IOSOrange,
                        title = "CPU Governor Performance Mode",
                        subtitle = "cpufreq/scaling_governor -> performance",
                        buttonText = if (cpuBoostEnabled) "Active" else "Boost",
                        buttonBg = if (cpuBoostEnabled) IOSGreen else IOSOrange,
                        onClick = { viewModel.boostCpuRoot() }
                    )
                }

                // Retina DPI
                item {
                    RootActionTile(
                        icon = Icons.Rounded.HighQuality,
                        iconColor = IOSPurple,
                        title = "Set iPhone Retina DPI (320)",
                        subtitle = "wm density 320",
                        buttonText = "Apply DPI",
                        buttonBg = IOSPurple,
                        onClick = { viewModel.setRetinaDpiRoot(320) }
                    )
                }

                // Spoof Model
                item {
                    RootActionTile(
                        icon = Icons.Rounded.PhoneIphone,
                        iconColor = IOSTeal,
                        title = "Spoof iPhone SE 2 Model",
                        subtitle = "Current: $spoofedModel",
                        buttonText = "Spoof",
                        buttonBg = IOSTeal,
                        onClick = { viewModel.spoofDeviceRoot("iPhone SE (2nd generation)") }
                    )
                }

                // Terminal Console
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("LIVE ROOT SHELL TERMINAL", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF000000))
                            .padding(10.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(rootLogs) { log ->
                                Text(
                                    text = "[${log.timestamp}] # ${log.command}\n${log.output}",
                                    color = if (log.isSuccess) IOSMint else Color(0xFFFF453A),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }
                    }
                }

                // Custom Command Input
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customShellCommand,
                            onValueChange = { customShellCommand = it },
                            placeholder = { Text("Enter root shell command (e.g. uname -a)", color = Color.Gray, fontSize = 13.sp) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1C1C1E),
                                unfocusedContainerColor = Color(0xFF1C1C1E),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        IconButton(
                            onClick = {
                                if (customShellCommand.isNotBlank()) {
                                    viewModel.executeCustomRootCommand(customShellCommand)
                                    customShellCommand = ""
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(IOSGreen)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Run", tint = Color.Black)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
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
fun RootActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    buttonText: String,
    buttonBg: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF1C1C1E))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(22.dp))
            }
            Column {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
            }
        }

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = buttonBg),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(buttonText, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
