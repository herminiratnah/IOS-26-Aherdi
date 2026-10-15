package com.example.ui.screens

import androidx.activity.compose.BackHandler
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
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

    // State flows from ViewModel
    val isImmersiveLocked by viewModel.isImmersiveLocked.collectAsState()
    val isSystemUIDisabled by viewModel.isSystemUIDisabled.collectAsState()
    val isNotificationListenerGranted by viewModel.isNotificationListenerGranted.collectAsState()
    val cpuBoostEnabled by viewModel.cpuBoostEnabled.collectAsState()
    val is120HzUnlocked by viewModel.is120HzUnlocked.collectAsState()
    val isThermalDisabled by viewModel.isThermalDisabled.collectAsState()
    val isSELinuxPermissive by viewModel.isSELinuxPermissive.collectAsState()
    val retinaDpi by viewModel.retinaDpi.collectAsState()
    val spoofedModel by viewModel.spoofedModel.collectAsState()
    val isTopDisplayInsetActive by viewModel.isTopDisplayInsetActive.collectAsState()
    val isVolumeInterceptionActive by viewModel.isVolumeInterceptionActive.collectAsState()
    val rootLogs by viewModel.rootLogs.collectAsState()

    // Sync saved root states immediately on open so toggles never revert
    LaunchedEffect(Unit) {
        viewModel.syncRootStates(context)
    }

    var customShellCommand by remember { mutableStateOf("") }
    var showRebootDialog by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = showRebootDialog != null) {
        showRebootDialog = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Spacer for Single Global Status Bar
            Spacer(modifier = Modifier.height(44.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "iRoot Center",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "J2 Prime & Root Control Suite • iOS 26",
                                fontSize = 13.sp,
                                color = Color.Gray
                            )
                        }

                        // Root Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isRootAvailable) Color(0x3334C759) else Color(0x33FF9500))
                                .border(
                                    width = 1.dp,
                                    color = if (isRootAvailable) Color(0xFF34C759) else Color(0xFFFF9500),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(if (isRootAvailable) IOSGreen else IOSOrange)
                                )
                                Text(
                                    text = if (isRootAvailable) "ROOT ACTIVE" else "SHELL MODE",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 1-Tap Quick Optimization Banner
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0xFF1C1C1E))
                            .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(IOSPurple.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = IOSPurple, modifier = Modifier.size(20.dp))
                                }
                                Text("1-Tap Full iOS Mode", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "Otomatis aktifkan Immersive Status & Nav Bar, bersihkan RAM cache J2 Prime, dan optimalkan CPU Governor.",
                                color = Color(0xFFAAAAAA),
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Button(
                                onClick = {
                                    if (!isImmersiveLocked) viewModel.toggleImmersiveRoot(context)
                                    if (!isTopDisplayInsetActive) viewModel.toggleTopDisplayInsetRoot(context)
                                    if (!isVolumeInterceptionActive) viewModel.toggleVolumeInterceptionRoot(context)
                                    viewModel.cleanJ2PrimeRamRoot()
                                    if (!cpuBoostEnabled) viewModel.toggleCpuBoostRoot(context)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Terapkan Semua Sekaligus (Instan)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                        }
                    }
                }

                // SECTION: SYSTEM INTERFACE & BARS
                item {
                    Text(
                        text = "TAMPILAN & SYSTEM BARS",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Ruang Mandiri Status Bar (Dedicated Top Space)
                item {
                    RootToggleCard(
                        icon = Icons.Rounded.VerticalAlignBottom,
                        iconColor = IOSGreen,
                        title = "Ruang Mandiri Status Bar (Y=0 Terkunci)",
                        subtitle = "Status Bar & Dynamic Island terkunci kokoh di ujung atas layar (Y=0). Menghilangkan bilah tombol bawah dan mereset batas layar agar konten tidak bertindih.",
                        isActive = isTopDisplayInsetActive,
                        badgeType = RestartBadgeType.INSTANT,
                        onToggle = { viewModel.toggleTopDisplayInsetRoot(context) }
                    )
                }

                // Sembunyikan Bar Android (icon_blacklist + safe immersive)
                item {
                    RootToggleCard(
                        icon = Icons.Rounded.Fullscreen,
                        iconColor = IOSBlue,
                        title = "Sembunyikan Bar Android (Dialog USB Aman)",
                        subtitle = "Menghapus jam/baterai/sinyal bawaan Android via icon_blacklist di semua app (WA, IG) tanpa mematikan dialog USB & izin sistem",
                        isActive = isImmersiveLocked,
                        badgeType = RestartBadgeType.INSTANT,
                        onToggle = { viewModel.toggleImmersiveRoot(context) }
                    )
                }

                // Disable SystemUI
                item {
                    RootToggleCard(
                        icon = Icons.Rounded.Security,
                        iconColor = IOSRed,
                        title = "Nonaktifkan SystemUI Android",
                        subtitle = "Mematikan com.android.systemui secara permanen & tahan restart via Magisk boot hook",
                        isActive = isSystemUIDisabled,
                        badgeType = RestartBadgeType.RESTART_UI,
                        onToggle = { viewModel.toggleSystemUIDisabledRoot(context) }
                    )
                }

                // Dynamic Island Notification Access
                item {
                    RootToggleCard(
                        icon = Icons.Rounded.NotificationsActive,
                        iconColor = IOSGreen,
                        title = "Akses Notifikasi Dynamic Island",
                        subtitle = "Memberikan izin intersepsi notifikasi ke Dynamic Island",
                        isActive = isNotificationListenerGranted,
                        badgeType = RestartBadgeType.INSTANT,
                        onToggle = { viewModel.grantNotificationListenerViaRoot(context) }
                    )
                }

                // Blokir Dialog Volume Android (Dynamic Island Only)
                item {
                    RootToggleCard(
                        icon = Icons.Rounded.VolumeUp,
                        iconColor = IOSBlue,
                        title = "Blokir Dialog Volume Android (Dynamic Island Saja)",
                        subtitle = "Cegah dialog/slider volume bawaan Android muncul saat menekan tombol volume fisik di WhatsApp, Setelan, dll. 100% dialihkan ke pil Dynamic Island.",
                        isActive = isVolumeInterceptionActive,
                        badgeType = RestartBadgeType.INSTANT,
                        onToggle = { viewModel.toggleVolumeInterceptionRoot(context) }
                    )
                }

                // Open System Notification Access
                item {
                    RootActionCard(
                        icon = Icons.Rounded.SettingsApplications,
                        iconColor = IOSPurple,
                        title = "Buka Izin Notifikasi Sistem",
                        subtitle = "Buka pengaturan Notification Listener bawaan Android",
                        badgeType = RestartBadgeType.NONE,
                        buttonLabel = "Buka",
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

                // SECTION: HARDWARE & PERFORMANCE
                item {
                    Text(
                        text = "PERFORMA & HARDWARE J2 PRIME",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // RAM Cache Cleaner
                item {
                    RootActionCard(
                        icon = Icons.Rounded.Memory,
                        iconColor = IOSMint,
                        title = "Bersihkan RAM & ZRAM Cache",
                        subtitle = "Drop cache & bersihkan memory (+420MB RAM Lega)",
                        badgeType = RestartBadgeType.INSTANT,
                        buttonLabel = "Bersihkan",
                        onClick = { viewModel.cleanJ2PrimeRamRoot() }
                    )
                }

                // CPU Boost Governor
                item {
                    RootToggleCard(
                        icon = Icons.Rounded.Speed,
                        iconColor = IOSOrange,
                        title = "CPU Governor Performance Boost",
                        subtitle = "Kunci frekuensi CPU pada performa maksimum (FPS stabil)",
                        isActive = cpuBoostEnabled,
                        badgeType = RestartBadgeType.INSTANT,
                        onToggle = { viewModel.toggleCpuBoostRoot(context) }
                    )
                }

                // 120Hz ProMotion
                item {
                    RootToggleCard(
                        icon = Icons.Rounded.Sync,
                        iconColor = IOSGreen,
                        title = "120Hz ProMotion Unlock",
                        subtitle = "Bypass batas 60Hz ke refresh rate tertinggi layar",
                        isActive = is120HzUnlocked,
                        badgeType = RestartBadgeType.INSTANT,
                        onToggle = { viewModel.toggle120HzRoot(context) }
                    )
                }

                // Thermal Throttling
                item {
                    RootToggleCard(
                        icon = Icons.Rounded.Whatshot,
                        iconColor = IOSRed,
                        title = "Matikan Thermal Throttling",
                        subtitle = "Cegah penurunan performa karena limit suhu (Gaming Lancar)",
                        isActive = isThermalDisabled,
                        badgeType = RestartBadgeType.INSTANT,
                        onToggle = { viewModel.toggleThermalThrottlingRoot(context) }
                    )
                }

                // SELinux Mode
                item {
                    RootToggleCard(
                        icon = Icons.Rounded.AdminPanelSettings,
                        iconColor = IOSYellow,
                        title = "SELinux Permissive Mode",
                        subtitle = "Bebaskan izin kernel untuk modifikasi sistem tanpa blokir",
                        isActive = isSELinuxPermissive,
                        badgeType = RestartBadgeType.INSTANT,
                        onToggle = { viewModel.toggleSELinuxRoot(context) }
                    )
                }

                // SECTION: RESOLUTION & SPOOFING
                item {
                    Text(
                        text = "RESOLUSI RETINA & IDENTITAS DEVICE",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Retina DPI Selector
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(IOSPurple.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.HighQuality, contentDescription = null, tint = IOSPurple, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text("Skala Retina DPI Layar", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        Text("Saat ini: $retinaDpi DPI", color = Color.Gray, fontSize = 12.sp)
                                    }
                                }
                                RestartBadge(RestartBadgeType.RESTART_UI)
                            }

                            // DPI Options Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(240 to "Default", 280 to "Kecil", 320 to "Retina", 360 to "Pro Max").forEach { (dpi, label) ->
                                    val isSelected = retinaDpi == dpi
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) IOSPurple else Color(0xFF2C2C2E))
                                            .clickable { viewModel.setRetinaDpiRoot(dpi, context) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = "$dpi",
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = label,
                                                color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Gray,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Spoofed Model Selector
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(IOSTeal.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.PhoneIphone, contentDescription = null, tint = IOSTeal, modifier = Modifier.size(20.dp))
                                    }
                                    Column {
                                        Text("Samarkan Model iPhone", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                        Text(spoofedModel, color = Color.Gray, fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                                RestartBadge(RestartBadgeType.RESTART_DEVICE)
                            }

                            // Spoof Models Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    "iPhone SE 2" to "iPhone SE (2nd generation)",
                                    "iPhone 16 Pro" to "iPhone 16 Pro Max",
                                    "iPhone 17" to "iPhone 17 Pro"
                                ).forEach { (shortLabel, fullModel) ->
                                    val isSelected = spoofedModel == fullModel
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) IOSTeal else Color(0xFF2C2C2E))
                                            .clickable { viewModel.spoofDeviceRoot(fullModel, context) }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = shortLabel,
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION: POWER & REBOOT CONTROLS
                item {
                    Text(
                        text = "KONTROL POWER & REBOOT",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Power Controls Grid
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Soft Reboot SystemUI
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1C1C1E))
                                .clickable { viewModel.softRestartSystemUI() }
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, tint = IOSBlue, modifier = Modifier.size(24.dp))
                                Text("Restart SystemUI", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Muat ulang statusbar tanpa reboot", color = Color.Gray, fontSize = 10.sp)
                            }
                        }

                        // Full Reboot
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1C1C1E))
                                .clickable { viewModel.rebootRoot("normal") }
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.RestartAlt, contentDescription = null, tint = IOSOrange, modifier = Modifier.size(24.dp))
                                Text("Reboot HP", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Nyalakan ulang sistem", color = Color.Gray, fontSize = 10.sp)
                            }
                        }

                        // Shutdown
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF1C1C1E))
                                .clickable { viewModel.powerOffRoot() }
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, tint = IOSRed, modifier = Modifier.size(24.dp))
                                Text("Matikan HP", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text("Power off total", color = Color.Gray, fontSize = 10.sp)
                            }
                        }
                    }
                }

                // Terminal Console
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("TERMINAL SHELL ROOT (#SU)", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF0F0F11))
                            .border(1.dp, Color(0xFF2C2C2E), RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(rootLogs) { log ->
                                Text(
                                    text = "[${log.timestamp}] # ${log.command}\n${log.output}",
                                    color = if (log.isSuccess) IOSMint else Color(0xFFFF453A),
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    lineHeight = 15.sp
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
                            placeholder = { Text("Ketik command root (misal: uname -a)", color = Color.Gray, fontSize = 12.sp) },
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
                            Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Jalankan", tint = Color.Black)
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
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

enum class RestartBadgeType {
    INSTANT,
    RESTART_UI,
    RESTART_DEVICE,
    NONE
}

@Composable
fun RestartBadge(type: RestartBadgeType) {
    val (label, bg, textCol) = when (type) {
        RestartBadgeType.INSTANT -> Triple("Instan", Color(0x2634C759), IOSGreen)
        RestartBadgeType.RESTART_UI -> Triple("Perlu Restart UI", Color(0x33FF9500), IOSOrange)
        RestartBadgeType.RESTART_DEVICE -> Triple("Perlu Restart HP", Color(0x33FF3B30), IOSRed)
        RestartBadgeType.NONE -> return
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = textCol,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RootToggleCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    isActive: Boolean,
    badgeType: RestartBadgeType,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF1C1C1E))
            .clickable { onToggle() }
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(subtitle, color = Color.Gray, fontSize = 11.sp, lineHeight = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Status Badge (AKTIF / NONAKTIF)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActive) Color(0x3334C759) else Color(0x228E8E93))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isActive) "AKTIF" else "NONAKTIF",
                            color = if (isActive) IOSGreen else Color.LightGray,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    RestartBadge(badgeType)
                }
            }
        }

        Switch(
            checked = isActive,
            onCheckedChange = { onToggle() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = IOSGreen,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF3A3A3C)
            )
        )
    }
}

@Composable
fun RootActionCard(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    badgeType: RestartBadgeType,
    buttonLabel: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
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
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color.Gray, fontSize = 11.sp, lineHeight = 14.sp)
                if (badgeType != RestartBadgeType.NONE) {
                    Spacer(modifier = Modifier.height(2.dp))
                    RestartBadge(badgeType)
                }
            }
        }

        Button(
            onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = iconColor),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            modifier = Modifier.height(34.dp)
        ) {
            Text(buttonLabel, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}
