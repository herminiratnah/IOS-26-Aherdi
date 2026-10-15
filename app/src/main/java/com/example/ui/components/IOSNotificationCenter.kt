package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.foundation.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
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
import com.example.model.ClockLayoutMode
import com.example.ui.theme.*
import com.example.util.IOSFeedbackHelper
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
    val flashlightEnabled by viewModel.flashlightEnabled.collectAsState()
    val activeNotifications by viewModel.activeNotifications.collectAsState()
    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val currentDate by viewModel.currentDateFormatted.collectAsState()
    val weatherData by viewModel.weatherData.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isCharging by viewModel.isBatteryCharging.collectAsState()
    val currentFocus by viewModel.currentFocusMode.collectAsState()

    val blurredWallpaper by com.example.util.IOSBlurHelper.blurredWallpaper.collectAsState()

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        // Swipe up to unlock / dismiss notification center
                        if (dragAmount < -20) onDismiss()
                    }
                }
        ) {
            // Authentic iOS Frosted Glass: Blurred Wallpaper + Translucent Frosting
            if (blurredWallpaper != null) {
                Image(
                    bitmap = blurredWallpaper!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            // iOS 18 Frosted Glass Translucent Specular Gradient Over Blur
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (blurredWallpaper != null) {
                            Color.Black.copy(alpha = 0.40f)
                        } else {
                            Color(0x99000000)
                        }
                    )
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x35000000),
                                Color(0x4C0A0E1A),
                                Color(0x5E080C16)
                            )
                        )
                    )
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x28FFFFFF),
                                Color(0x0CFFFFFF),
                                Color.Transparent
                            ),
                            radius = 1400f
                        )
                    )
            )
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
                    tint = Color.White,
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
                    ClockFontStyle.EDITORIAL_SERIF -> Pair(FontWeight.Normal, BodoniModaFontFamily)
                    ClockFontStyle.PLAYFAIR_DISPLAY -> Pair(FontWeight.Normal, PlayfairDisplayFontFamily)
                    ClockFontStyle.BOLD_MODERN -> Pair(FontWeight.Black, FontFamily.SansSerif)
                    ClockFontStyle.SERIF_CLASSIC -> Pair(FontWeight.SemiBold, CormorantGaramondFontFamily)
                    ClockFontStyle.ROUNDED_SLICK -> Pair(FontWeight.ExtraBold, FontFamily.SansSerif)
                    ClockFontStyle.TECH_STENCIL -> Pair(FontWeight.Bold, FontFamily.Monospace)
                    ClockFontStyle.MINIMAL_THIN -> Pair(FontWeight.Light, CormorantGaramondFontFamily)
                    ClockFontStyle.ELEGANT_SCRIPT -> Pair(FontWeight.Normal, FontFamily.Cursive)
                    ClockFontStyle.RETRO_HEAVY -> Pair(FontWeight.Black, FontFamily.SansSerif)
                    ClockFontStyle.CYBER_PUNK -> Pair(FontWeight.ExtraBold, FontFamily.Monospace)
                    else -> Pair(FontWeight.SemiBold, FontFamily.SansSerif)
                }

                val resolvedClockColor = if (config.clockColorHex == 0xFFFFFFFF.toLong()) {
                    Color.White
                } else {
                    Color(config.clockColorHex)
                }

                Text(
                    text = currentTime.ifBlank { "09:41" },
                    color = resolvedClockColor,
                    fontSize = 80.sp,
                    fontWeight = clockFontWeight,
                    fontFamily = clockFontFamily,
                    letterSpacing = (-2).sp,
                    textAlign = TextAlign.Center
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
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = when {
                                weatherData.weatherCode in 61..82 -> Icons.Rounded.WaterDrop
                                weatherData.weatherCode == 0 -> Icons.Rounded.WbSunny
                                else -> Icons.Rounded.Cloud
                            },
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("${weatherData.currentTemp}° ${weatherData.condition}", color = Color.White, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    // Battery Widget
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                            .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.25f)), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                            contentDescription = null,
                            tint = if (isCharging) IOSGreen else Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text("$batteryLevel%", color = Color.White, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notification Feed (Only shown when active notifications exist, NO placeholders!)
                if (activeNotifications.isNotEmpty()) {
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
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.22f))
                                    .clickable { viewModel.clearAllNotifications() }
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Clear All",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

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
                                        .background(Color.White.copy(alpha = 0.22f))
                                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.26f)), RoundedCornerShape(18.dp))
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
                                                    color = Color.White.copy(alpha = 0.8f),
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
                                                    color = Color.White.copy(alpha = 0.75f),
                                                    fontSize = 11.sp
                                                )
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .clip(CircleShape)
                                                        .background(Color.White.copy(alpha = 0.25f))
                                                        .clickable {
                                                            viewModel.dismissNotification(notif.id)
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Dismiss",
                                                        tint = Color.White,
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
                                            color = Color.White.copy(alpha = 0.88f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                // Lock Screen Focus Pill
                val isFocusActive = currentFocus != com.example.model.FocusMode.OFF
                Box(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (isFocusActive) Color(currentFocus.colorLong).copy(alpha = 0.85f)
                            else Color.White.copy(alpha = 0.2f)
                        )
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)), RoundedCornerShape(20.dp))
                        .clickable {
                            IOSFeedbackHelper.vibrateLight(context)
                            viewModel.toggleFocusMode(context)
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = when (currentFocus) {
                                com.example.model.FocusMode.DO_NOT_DISTURB -> Icons.Rounded.NightlightRound
                                com.example.model.FocusMode.WORK -> Icons.Rounded.Work
                                com.example.model.FocusMode.PERSONAL -> Icons.Rounded.Person
                                com.example.model.FocusMode.SLEEP -> Icons.Rounded.Bedtime
                                else -> Icons.Rounded.NightlightRound
                            },
                            contentDescription = "Focus",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isFocusActive) currentFocus.label else "Fokus",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Bottom Quick Action Buttons: Flashlight, Home Bar Pill (swipe up), & Camera - perfectly aligned
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Flashlight
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (flashlightEnabled) Color(0xFFFFCC00) else Color.White.copy(alpha = 0.22f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)), CircleShape)
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

                    // Home bar pill indicator
                    Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .clickable { onDismiss() }
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount < -15f) onDismiss()
                                }
                            }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .width(135.dp)
                                .height(4.5.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color.White.copy(alpha = 0.75f))
                        )
                    }

                    // Camera quick button
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.22f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)), CircleShape)
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
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lock Screen & Depth Effect",
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

            // 1. DEPTH EFFECT TOGGLE
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x22FFFFFF))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (config.depthEffectEnabled) IOSBlue else Color.Gray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Layers,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Depth Effect (Jam di Balik Objek)",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (config.depthEffectEnabled) "Jam tersembunyi di balik subjek foto 3D" else "Jam standar di atas wallpaper",
                            color = Color.Gray,
                            fontSize = 11.sp
                        )
                    }
                }
                Switch(
                    checked = config.depthEffectEnabled,
                    onCheckedChange = { isChecked ->
                        onUpdateConfig { it.copy(depthEffectEnabled = isChecked) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF34C759)
                    )
                )
            }

            // 2. CLOCK LAYOUT MODE (Stacked 2-Baris Editorial vs 1-Baris Horizontal)
            Text("FORMAT TATA LETAK JAM", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val isStacked = config.clockLayoutMode == ClockLayoutMode.STACKED_EDITORIAL
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isStacked) IOSBlue else Color(0x22FFFFFF))
                        .clickable { onUpdateConfig { it.copy(clockLayoutMode = ClockLayoutMode.STACKED_EDITORIAL) } }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("2-Baris Stacked", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Format Majalah Vogue/Didot", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (!isStacked) IOSBlue else Color(0x22FFFFFF))
                        .clickable { onUpdateConfig { it.copy(clockLayoutMode = ClockLayoutMode.HORIZONTAL_STANDARD) } }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("1-Baris Standar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Format Klasik 09:41", color = Color.White.copy(alpha = 0.7f), fontSize = 10.sp)
                    }
                }
            }

            // 3. DYNAMIC CLOCK VERTICAL POSITION (Posisi Jam Atas-Bawah)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x22FFFFFF))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("POSISI VERTIKAL JAM (Y)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${(config.clockTopPaddingPercent * 100).toInt()}% Layar", color = IOSBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = config.clockTopPaddingPercent,
                    onValueChange = { newPct ->
                        onUpdateConfig { it.copy(clockTopPaddingPercent = newPct) }
                    },
                    valueRange = 0.04f..0.26f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = IOSBlue,
                        inactiveTrackColor = Color(0x44FFFFFF)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair("Tinggi (6%)", 0.06f),
                        Pair("Optimal (10%)", 0.10f),
                        Pair("Rendah (16%)", 0.16f)
                    ).forEach { (label, presetVal) ->
                        val isPreset = kotlin.math.abs(config.clockTopPaddingPercent - presetVal) < 0.015f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPreset) IOSBlue.copy(alpha = 0.35f) else Color(0x15FFFFFF))
                                .clickable { onUpdateConfig { it.copy(clockTopPaddingPercent = presetVal) } }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isPreset) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // 4. DYNAMIC CLOCK HORIZONTAL OFFSET (Geser Jam Kiri / Kanan)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x22FFFFFF))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("POSISI HORIZONTAL (X)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(if (config.clockHorizontalOffset == 0f) "Center" else "${config.clockHorizontalOffset.toInt()} dp", color = IOSBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = config.clockHorizontalOffset,
                    onValueChange = { newOffset ->
                        onUpdateConfig { it.copy(clockHorizontalOffset = newOffset) }
                    },
                    valueRange = -60f..60f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = IOSBlue,
                        inactiveTrackColor = Color(0x44FFFFFF)
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Pair("Rata Kiri (-40)", -40f),
                        Pair("Tengah (0)", 0f),
                        Pair("Rata Kanan (+40)", 40f)
                    ).forEach { (label, presetVal) ->
                        val isPreset = kotlin.math.abs(config.clockHorizontalOffset - presetVal) < 5f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isPreset) IOSBlue.copy(alpha = 0.35f) else Color(0x15FFFFFF))
                                .clickable { onUpdateConfig { it.copy(clockHorizontalOffset = presetVal) } }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(label, color = if (isPreset) Color.White else Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // 5. DYNAMIC CLOCK FONT SIZE (Ukuran Font Dinamis)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x22FFFFFF))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("UKURAN FONT JAM", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text("${config.clockFontSize.toInt()} sp", color = IOSBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = config.clockFontSize,
                    onValueChange = { newSize ->
                        onUpdateConfig { it.copy(clockFontSize = newSize) }
                    },
                    valueRange = 72f..132f,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = IOSBlue,
                        inactiveTrackColor = Color(0x44FFFFFF)
                    )
                )
            }

            // 6. FONT STYLE SELECTOR (7 Koleksi Font)
            Text("KOLEKSI FONT EDITORIAL & KLASIK", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            val fontOptions = listOf(
                Pair(ClockFontStyle.EDITORIAL_SERIF, "Bodoni Moda\n(Editorial Serif)"),
                Pair(ClockFontStyle.SERIF_CLASSIC, "Cormorant\n(Classic Serif)"),
                Pair(ClockFontStyle.PLAYFAIR_DISPLAY, "Playfair\n(Display Serif)"),
                Pair(ClockFontStyle.BOLD_MODERN, "iOS Modern\n(Bold Sans)"),
                Pair(ClockFontStyle.ROUNDED_SLICK, "iOS Rounded\n(Curved)"),
                Pair(ClockFontStyle.TECH_STENCIL, "Monospace\n(Tech Stencil)"),
                Pair(ClockFontStyle.MINIMAL_THIN, "Minimal\n(Thin Luxury)")
            )
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(fontOptions) { (style, label) ->
                    val isSelected = config.fontStyle == style
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) IOSBlue else Color(0x22FFFFFF))
                            .clickable {
                                onUpdateConfig { it.copy(fontStyle = style) }
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            // 7. COLOR PALETTE PICKER (Harmoni Otomatis & Pilihan Manual)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("WARNA JAM", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                // Auto Harmony Theme Color Toggle
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (config.useAutoThemeColor) Color(0x3334C759) else Color(0x22FFFFFF))
                        .clickable { onUpdateConfig { it.copy(useAutoThemeColor = !it.useAutoThemeColor) } }
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (config.useAutoThemeColor) "✓ Auto Theme Color" else "Manual Color",
                        color = if (config.useAutoThemeColor) Color(0xFF34C759) else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val colors = listOf(
                0xFFFFFFFF, // Pure Silver White
                0xFFFFF7EA, // Warm Golden Sunset Ivory
                0xFF32ADE6, // iOS Cyan
                0xFFFFCC00, // iOS Gold Yellow
                0xFFFF9500, // Sunset Orange
                0xFFFF2D55, // Rose Pink
                0xFF34C759, // Emerald Green
                0xFFAF52DE  // Royal Purple
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                colors.forEach { hex ->
                    val isSelected = !config.useAutoThemeColor && config.clockColorHex == hex
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(hex))
                            .clickable {
                                onUpdateConfig { it.copy(clockColorHex = hex, useAutoThemeColor = false) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = if (hex == 0xFFFFFFFF || hex == 0xFFFFF7EA) Color.Black else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 8. 3D PARALLAX & ATMOSPHERIC EFFECTS
            if (config.depthEffectEnabled) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0x22FFFFFF))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("INTENSITAS PARALLAX 3D", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text("${String.format(java.util.Locale.US, "%.1f", config.parallaxIntensity)}x", color = IOSBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = config.parallaxIntensity,
                        onValueChange = { newIntensity ->
                            onUpdateConfig { it.copy(parallaxIntensity = newIntensity) }
                        },
                        valueRange = 0f..2.0f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = IOSBlue,
                            inactiveTrackColor = Color(0x44FFFFFF)
                        )
                    )

                    // Atmosphere Toggles: Sun and Living Atmosphere
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Matahari / Cahaya Horizon", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = config.showBackgroundSun,
                            onCheckedChange = { isChecked ->
                                onUpdateConfig { it.copy(showBackgroundSun = isChecked) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759))
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Atmosfer Partikel & Specular Glint", color = Color.White, fontSize = 13.sp)
                        Switch(
                            checked = config.showAmbientAtmosphere,
                            onCheckedChange = { isChecked ->
                                onUpdateConfig { it.copy(showAmbientAtmosphere = isChecked) }
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF34C759))
                        )
                    }
                }
            }

            // 9. DEPTH THEMES SELECTOR (10 Pilihan Foto & Subjek 3D Asli)
            Text("PILIHAN SUBJEK WALLPAPER 3D (10 TEMA)", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            val themes = listOf(
                Pair("golden_dunes", "Golden Dunes"),
                Pair("monochrome_wave", "Monochrome Wave"),
                Pair("twilight_clouds", "Twilight Clouds"),
                Pair("coastal_flora", "Coastal Flora"),
                Pair("daisy_meadow", "Daisy Meadow"),
                Pair("sunset_reeds", "Sunset Reeds"),
                Pair("crystal_glass", "Crystal Glass"),
                Pair("glass_sanctuary", "Glass Sanctuary"),
                Pair("glass_still_life", "Glass Still Life"),
                Pair("mountain", "Alpine Mount")
            )
            androidx.compose.foundation.lazy.LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(themes) { (themeKey, label) ->
                    val isSelected = config.depthTheme == themeKey
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) IOSBlue else Color(0x22FFFFFF))
                            .clickable {
                                onUpdateConfig {
                                    it.copy(
                                        depthTheme = themeKey,
                                        useAutoThemeColor = true
                                    )
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 10. WIDGETS QUICK TOGGLES
            Text("LOCK SCREEN WIDGETS", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Weather
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (config.showWeatherWidget) Color(0x3334C759) else Color(0x22FFFFFF))
                        .clickable { onUpdateConfig { it.copy(showWeatherWidget = !it.showWeatherWidget) } }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (config.showWeatherWidget) "✓ Weather" else "Weather",
                        color = if (config.showWeatherWidget) Color(0xFF34C759) else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Battery
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (config.showBatteryWidget) Color(0x3334C759) else Color(0x22FFFFFF))
                        .clickable { onUpdateConfig { it.copy(showBatteryWidget = !it.showBatteryWidget) } }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (config.showBatteryWidget) "✓ Battery" else "Battery",
                        color = if (config.showBatteryWidget) Color(0xFF34C759) else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Calendar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (config.showCalendarWidget) Color(0x3334C759) else Color(0x22FFFFFF))
                        .clickable { onUpdateConfig { it.copy(showCalendarWidget = !it.showCalendarWidget) } }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (config.showCalendarWidget) "✓ Calendar" else "Calendar",
                        color = if (config.showCalendarWidget) Color(0xFF34C759) else Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // 11. RESET TO OPTIMAL DEFAULTS BUTTON
            OutlinedButton(
                onClick = {
                    onUpdateConfig {
                        it.copy(
                            clockTopPaddingPercent = 0.10f,
                            clockHorizontalOffset = 0f,
                            clockFontSize = 108f,
                            fontStyle = ClockFontStyle.EDITORIAL_SERIF,
                            clockLayoutMode = ClockLayoutMode.STACKED_EDITORIAL,
                            useAutoThemeColor = true,
                            parallaxIntensity = 1.0f,
                            showBackgroundSun = true,
                            showAmbientAtmosphere = true,
                            depthEffectEnabled = true
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White.copy(alpha = 0.8f))
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Atur Ulang ke Rekomendasi Tema Asli", fontSize = 12.sp)
            }
        }
    }
}

/**
 * Standalone Notification Center for WindowManager Overlay (over third-party apps).
 */
@Composable
fun StandaloneOverlayNotificationCenter(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentTime by remember { mutableStateOf("09:41") }
    var currentDate by remember { mutableStateOf("Minggu, 11 Oktober") }
    var batteryLevel by remember { mutableIntStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    val notificationsList = remember { mutableStateListOf<com.example.service.AppNotificationData>() }

    LaunchedEffect(Unit) {
        com.example.service.IOSNotificationListenerService.notificationFlow.collect { notif ->
            notificationsList.add(0, notif)
            if (notificationsList.size > 20) {
                notificationsList.removeAt(notificationsList.lastIndex)
            }
        }
    }

    LaunchedEffect(Unit) {
        val sdfTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        val sdfDate = java.text.SimpleDateFormat("EEEE, d MMMM", java.util.Locale("id", "ID"))
        val bm = context.getSystemService(android.content.Context.BATTERY_SERVICE) as? android.os.BatteryManager
        while (true) {
            val now = java.util.Date()
            currentTime = sdfTime.format(now)
            currentDate = sdfDate.format(now)
            batteryLevel = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
            kotlinx.coroutines.delay(10000)
        }
    }

    val blurredWallpaper by com.example.util.IOSBlurHelper.blurredWallpaper.collectAsState()

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount < -20) onDismiss()
                    }
                }
        ) {
            // Authentic iOS Frosted Glass: Blurred Wallpaper + Translucent Frosting
            if (blurredWallpaper != null) {
                Image(
                    bitmap = blurredWallpaper!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop
                )
            }
            // iOS 18 Frosted Glass Translucent Specular Gradient Over Blur
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (blurredWallpaper != null) {
                            Color.Black.copy(alpha = 0.40f)
                        } else {
                            Color(0x99000000)
                        }
                    )
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0x35000000),
                                Color(0x4C0A0E1A),
                                Color(0x5E080C16)
                            )
                        )
                    )
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0x28FFFFFF),
                                Color(0x0CFFFFFF),
                                Color.Transparent
                            ),
                            radius = 1400f
                        )
                    )
            )
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
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )

                // Date label
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = currentDate,
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Large iOS Clock
                Text(
                    text = currentTime,
                    color = Color.White,
                    fontSize = 76.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-2).sp
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Notifications Header / List
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pusat Pemberitahuan",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    if (notificationsList.isNotEmpty()) {
                        Text(
                            text = "${notificationsList.size} pemberitahuan",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 13.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (notificationsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tidak Ada Pemberitahuan Terbaru",
                            color = Color.White.copy(alpha = 0.45f),
                            fontSize = 14.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notificationsList) { notif ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(Color.White.copy(alpha = 0.14f))
                                    .border(BorderStroke(0.8.dp, Color.White.copy(alpha = 0.20f)), RoundedCornerShape(18.dp))
                                    .clickable {
                                        try {
                                            notif.contentIntent?.send()
                                            onDismiss()
                                        } catch (_: Exception) {}
                                    }
                                    .padding(14.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(notif.accentColor)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Notifications,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = notif.appName,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "Sekarang",
                                                color = Color.White.copy(alpha = 0.5f),
                                                fontSize = 11.sp
                                            )
                                        }
                                        if (notif.title.isNotBlank()) {
                                            Text(
                                                text = notif.title,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = notif.message,
                                            color = Color.White.copy(alpha = 0.8f),
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Bottom Quick Action Buttons (Flashlight & Camera)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp, start = 12.dp, end = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    var isFlashlightOn by remember { mutableStateOf(false) }
                    IconButton(
                        onClick = {
                            IOSFeedbackHelper.vibrateLight(context)
                            isFlashlightOn = !isFlashlightOn
                            try {
                                val cam = context.getSystemService(android.content.Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                                val camId = cam?.cameraIdList?.firstOrNull()
                                if (cam != null && camId != null) {
                                    cam.setTorchMode(camId, isFlashlightOn)
                                }
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isFlashlightOn) Color.White else Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlashlightOn,
                            contentDescription = "Flashlight",
                            tint = if (isFlashlightOn) Color.Black else Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Swipe Up Indicator Home Bar
                    Box(
                        modifier = Modifier
                            .width(130.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.6f))
                            .clickable { onDismiss() }
                    )

                    IconButton(
                        onClick = {
                            IOSFeedbackHelper.vibrateLight(context)
                            onDismiss()
                            try {
                                val camIntent = Intent(android.provider.MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
                                camIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(camIntent)
                            } catch (_: Exception) {}
                        },
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PhotoCamera,
                            contentDescription = "Camera",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
