package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppId
import com.example.model.ClockFontStyle
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

@Composable
fun IOSNotificationCenter(
    viewModel: IOSViewModel,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.lockScreenConfig.collectAsState()
    val isCustomizeOpen by viewModel.isLockScreenCustomizeOpen.collectAsState()
    val flashlightEnabled by viewModel.flashlightEnabled.collectAsState()
    val activeNotifications by viewModel.activeNotifications.collectAsState()
    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val currentDate by viewModel.currentDateFormatted.collectAsState()

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xEB000000))
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        // Swipe up to unlock / dismiss notification center
                        if (dragAmount < -20) onDismiss()
                    }
                }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Lock Icon & Padlock
                Spacer(modifier = Modifier.height(10.dp))
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = "Locked",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )

                // Date label
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentDate.ifBlank { "Tuesday, July 15" },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium
                )

                // Customizable Big Clock Typography
                Spacer(modifier = Modifier.height(4.dp))
                val (clockFontWeight, clockFontFamily) = when (config.fontStyle) {
                    ClockFontStyle.BOLD_MODERN -> Pair(FontWeight.Black, FontFamily.SansSerif)
                    ClockFontStyle.SERIF_CLASSIC -> Pair(FontWeight.SemiBold, FontFamily.Serif)
                    ClockFontStyle.ROUNDED_SLICK -> Pair(FontWeight.ExtraBold, FontFamily.SansSerif)
                    ClockFontStyle.TECH_STENCIL -> Pair(FontWeight.Bold, FontFamily.Monospace)
                    ClockFontStyle.MINIMAL_THIN -> Pair(FontWeight.Light, FontFamily.SansSerif)
                }

                Text(
                    text = currentTime.ifBlank { "09:41" },
                    color = Color(config.clockColorHex),
                    fontSize = 80.sp,
                    fontWeight = clockFontWeight,
                    fontFamily = clockFontFamily,
                    letterSpacing = (-2).sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clickable {
                        viewModel.openLockScreenCustomize()
                    }
                )

                // Lock Screen Widgets Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Weather Widget
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cloud,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("28° Mostly Cloudy", color = Color.White, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Battery Widget
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33FFFFFF))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.BatteryChargingFull,
                            contentDescription = null,
                            tint = IOSGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("85%", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notification Feed
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "NOTIFICATION CENTER (${activeNotifications.size})",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        if (activeNotifications.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0x33FFFFFF))
                                    .clickable { viewModel.clearAllNotifications() }
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Clear All",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    if (activeNotifications.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NotificationsNone,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "No Older Notifications",
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(activeNotifications, key = { it.id }) { notif ->
                                val iconVector = when (notif.iconKey.lowercase()) {
                                    "whatsapp", "message", "chat" -> Icons.Rounded.ChatBubble
                                    "instagram", "photo" -> Icons.Rounded.CameraAlt
                                    "phone", "call" -> Icons.Rounded.Phone
                                    "calendar" -> Icons.Rounded.CalendarToday
                                    "music" -> Icons.Rounded.MusicNote
                                    else -> Icons.Rounded.Notifications
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(Color(0x33FFFFFF))
                                        .clickable {
                                            when {
                                                notif.packageName == "com.whatsapp" -> {
                                                    try {
                                                        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.whatsapp")
                                                        if (launchIntent != null) context.startActivity(launchIntent)
                                                        else viewModel.openApp(AppId.MESSAGES)
                                                    } catch (e: Exception) {
                                                        viewModel.openApp(AppId.MESSAGES)
                                                    }
                                                }
                                                notif.packageName == "com.instagram.android" -> {
                                                    try {
                                                        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.instagram.android")
                                                        if (launchIntent != null) context.startActivity(launchIntent)
                                                    } catch (e: Exception) {}
                                                }
                                                notif.iconKey == "calendar" -> viewModel.openApp(AppId.CALENDAR)
                                                notif.iconKey == "phone" -> viewModel.openApp(AppId.PHONE)
                                                else -> viewModel.openApp(AppId.MESSAGES)
                                            }
                                            onDismiss()
                                        }
                                        .padding(12.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(20.dp)
                                                        .clip(RoundedCornerShape(5.dp))
                                                        .background(Color(notif.accentColor)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = iconVector,
                                                        contentDescription = null,
                                                        tint = Color.White,
                                                        modifier = Modifier.size(13.dp)
                                                    )
                                                }
                                                Text(
                                                    text = notif.appName.uppercase(),
                                                    color = Color.White.copy(alpha = 0.7f),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = notif.timestamp,
                                                    color = Color.White.copy(alpha = 0.5f),
                                                    fontSize = 11.sp
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0x33FFFFFF))
                                                        .clickable {
                                                            viewModel.dismissNotification(notif.id)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Dismiss",
                                                        tint = Color.White.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                }
                                            }
                                        }
                                        Text(
                                            text = notif.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = notif.message,
                                            color = Color.White.copy(alpha = 0.9f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Bottom Quick Action Buttons: Flashlight & Camera
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flashlight
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (flashlightEnabled) Color.White else Color(0x33FFFFFF))
                            .clickable { viewModel.toggleFlashlight(context) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlashlightOn,
                            contentDescription = "Flashlight",
                            tint = if (flashlightEnabled) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Swipe up to unlock indicator
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.clickable { onDismiss() }
                    ) {
                        Text(
                            text = "Swipe up to open",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .width(135.dp)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White)
                        )
                    }

                    // Camera quick button
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color(0x33FFFFFF))
                            .clickable {
                                viewModel.openApp(AppId.CAMERA)
                                onDismiss()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CameraAlt,
                            contentDescription = "Camera",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Lock Screen Customizer Bottom Sheet Modal
            if (isCustomizeOpen) {
                LockScreenCustomizerSheet(
                    config = config,
                    onUpdateConfig = { viewModel.updateLockScreenConfig(it) },
                    onClose = { viewModel.closeLockScreenCustomize() }
                )
            }
        }
    }
}

@Composable
fun NotificationCard(
    appName: String,
    time: String,
    title: String,
    content: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x33FFFFFF))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Text(
                        text = appName.uppercase(),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
                Text(
                    text = time,
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
            Text(
                text = title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = content,
                color = Color.White.copy(alpha = 0.9f),
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun LockScreenCustomizerSheet(
    config: com.example.model.LockScreenConfig,
    onUpdateConfig: ((com.example.model.LockScreenConfig) -> com.example.model.LockScreenConfig) -> Unit,
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x88000000))
            .clickable { onClose() },
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Color(0xFF1C1C1E))
                .clickable(enabled = false) {}
                .padding(20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Font & Color",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            // Font Style Picker
            Text("FONT STYLE", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ClockFontStyle.values().forEach { style ->
                    val isSelected = config.fontStyle == style
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) IOSBlue else Color(0x33FFFFFF))
                            .clickable {
                                onUpdateConfig { it.copy(fontStyle = style) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val fontF = when (style) {
                            ClockFontStyle.SERIF_CLASSIC -> FontFamily.Serif
                            ClockFontStyle.TECH_STENCIL -> FontFamily.Monospace
                            else -> FontFamily.SansSerif
                        }
                        Text(
                            text = "12",
                            color = Color.White,
                            fontFamily = fontF,
                            fontSize = 18.sp,
                            fontWeight = if (style == ClockFontStyle.BOLD_MODERN) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            // Color Palette Picker
            Text("CLOCK COLOR", color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            val colors = listOf(
                0xFFFFFFFF,
                0xFF32ADE6, // iOS Cyan
                0xFFFFCC00, // iOS Yellow
                0xFFFF2D55, // iOS Pink
                0xFF34C759, // iOS Green
                0xFFAF52DE, // iOS Purple
                0xFFFF9500  // iOS Orange
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                colors.forEach { hex ->
                    val isSelected = config.clockColorHex == hex
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .clickable {
                                onUpdateConfig { it.copy(clockColorHex = hex) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = if (hex == 0xFFFFFFFF) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
