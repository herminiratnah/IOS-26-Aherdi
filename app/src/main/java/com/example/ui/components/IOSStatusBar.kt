package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.DynamicIslandData
import com.example.viewmodel.DynamicIslandMode

@Composable
fun IOSStatusBar(
    dynamicIslandData: DynamicIslandData,
    onDismissDynamicIsland: () -> Unit,
    onOpenControlCenter: () -> Unit,
    onOpenNotificationCenter: () -> Unit,
    currentTime: String = "9:41",
    batteryLevel: Int = 100,
    isBatteryCharging: Boolean = false,
    isWifiConnected: Boolean = true,
    wifiSignalLevel: Int = 4,
    cellularBars: Int = 4,
    networkType: String = "5G",
    isAirplaneMode: Boolean = false,
    isDarkIcons: Boolean = false,
    isMusicPlaying: Boolean = false,
    musicProgress: Float = 0f,
    musicCurrentTimeStr: String = "0:00",
    musicDurationStr: String = "3:45",
    focusMode: com.example.model.FocusMode = com.example.model.FocusMode.OFF,
    secondaryIslandData: DynamicIslandData? = null,
    onSecondaryIslandClick: (() -> Unit)? = null,
    networkSpeed: String = "",
    showNetworkSpeed: Boolean = true,
    showBatteryPercentage: Boolean = true,
    isLowPowerMode: Boolean = false,
    isBluetoothConnected: Boolean = false,
    isVpnConnected: Boolean = false,
    isLocationActive: Boolean = false,
    isAlarmSet: Boolean = false,
    onMusicPlayPause: () -> Unit = {},
    onMusicNext: () -> Unit = {},
    onMusicPrev: () -> Unit = {},
    onExpandMusic: () -> Unit = {},
    onOpenIslandAction: (() -> Unit)? = null,
    onAnswerCall: (() -> Unit)? = null,
    onDeclineCall: (() -> Unit)? = null,
    onToggleMuteCall: (() -> Unit)? = null,
    onToggleSpeakerCall: (() -> Unit)? = null,
    isCallMuted: Boolean = false,
    isCallSpeaker: Boolean = false,
    onAirPlaySelect: ((String) -> Unit)? = null,
    onSeekMusic: ((Float) -> Unit)? = null,
    onTimerPauseResume: (() -> Unit)? = null,
    onTimerCancel: (() -> Unit)? = null,
    onVoiceMemoStop: (() -> Unit)? = null,
    onLockDevice: (() -> Unit)? = null,
    onOpenClock: (() -> Unit)? = null,
    onOpenBatteryInfo: (() -> Unit)? = null,
    backgroundColor: Color = Color.Transparent,
    modifier: Modifier = Modifier
) {
    val iconColor = if (isDarkIcons) Color.Black else Color.White
    val isExpanded = dynamicIslandData.mode != DynamicIslandMode.COMPACT

    // Dynamic Island reactive animation: Status bar icons smoothly move away when island expands
    val leftTranslationX by animateFloatAsState(
        targetValue = if (isExpanded) -85f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "leftTranslationX"
    )
    val rightTranslationX by animateFloatAsState(
        targetValue = if (isExpanded) 85f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "rightTranslationX"
    )
    val sideAlpha by animateFloatAsState(
        targetValue = if (isExpanded) 0f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "sideAlpha"
    )
    val sideScale by animateFloatAsState(
        targetValue = if (isExpanded) 0.75f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium),
        label = "sideScale"
    )

    var dragStartX by remember { mutableFloatStateOf(0f) }
    var totalWidthPx by remember { mutableFloatStateOf(1080f) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(top = 0.dp, bottom = 0.dp)
            .onGloballyPositioned { coordinates ->
                if (coordinates.size.width > 0) {
                    totalWidthPx = coordinates.size.width.toFloat()
                }
            }
            .pointerInput(Unit) {
                // Double tap anywhere on status bar empty space to quickly lock screen!
                detectTapGestures(
                    onDoubleTap = {
                        onLockDevice?.invoke()
                    }
                )
            }
            .pointerInput(Unit) {
                // Swipe down gestures: right side -> Control Center, left side -> Notification Center
                var cumulativeDragY = 0f
                var triggered = false
                detectDragGestures(
                    onDragStart = { offset ->
                        dragStartX = offset.x
                        cumulativeDragY = 0f
                        triggered = false
                    },
                    onDragEnd = {
                        cumulativeDragY = 0f
                        triggered = false
                    },
                    onDragCancel = {
                        cumulativeDragY = 0f
                        triggered = false
                    },
                    onDrag = { change, dragAmount ->
                        cumulativeDragY += dragAmount.y
                        if (!triggered && cumulativeDragY > 20f) {
                            triggered = true
                            if (dragStartX > (totalWidthPx * 0.55f)) {
                                onOpenControlCenter()
                            } else {
                                onOpenNotificationCenter()
                            }
                            change.consume()
                        }
                    }
                )
            }
    ) {
        // Left Side: Real Device Time, Location, Focus, Alarm
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 22.dp, top = 4.dp)
                .graphicsLayer {
                    translationX = leftTranslationX
                    alpha = sideAlpha
                    scaleX = sideScale
                    scaleY = sideScale
                }
                .clickable {
                    if (onOpenClock != null) {
                        onOpenClock()
                    } else {
                        onOpenNotificationCenter()
                    }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = currentTime,
                    color = iconColor,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.2).sp
                )

                // Location GPS Active Indicator
                if (isLocationActive) {
                    Icon(
                        imageVector = Icons.Rounded.NearMe,
                        contentDescription = "GPS Active",
                        tint = iconColor,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Next Alarm Set Indicator
                if (isAlarmSet) {
                    Icon(
                        imageVector = Icons.Rounded.Alarm,
                        contentDescription = "Alarm Active",
                        tint = iconColor.copy(alpha = 0.85f),
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Focus Mode Badge
                if (focusMode != com.example.model.FocusMode.OFF) {
                    Icon(
                        imageVector = when (focusMode) {
                            com.example.model.FocusMode.DO_NOT_DISTURB -> Icons.Rounded.NightlightRound
                            com.example.model.FocusMode.WORK -> Icons.Rounded.Work
                            com.example.model.FocusMode.PERSONAL -> Icons.Rounded.Person
                            com.example.model.FocusMode.SLEEP -> Icons.Rounded.Bedtime
                            else -> Icons.Rounded.NightlightRound
                        },
                        contentDescription = "Focus",
                        tint = Color(focusMode.colorLong),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Center: Dynamic Island (Anchored at TopCenter so it expands downwards smoothly)
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 3.dp)
        ) {
            DynamicIsland(
                data = dynamicIslandData,
                onDismiss = onDismissDynamicIsland,
                isMusicPlaying = isMusicPlaying,
                musicProgress = musicProgress,
                musicCurrentTimeStr = musicCurrentTimeStr,
                musicDurationStr = musicDurationStr,
                onMusicPlayPause = onMusicPlayPause,
                onMusicNext = onMusicNext,
                onMusicPrev = onMusicPrev,
                onExpandMusic = onExpandMusic,
                onOpenAction = onOpenIslandAction,
                onAnswerCall = onAnswerCall,
                onDeclineCall = onDeclineCall,
                onToggleMuteCall = onToggleMuteCall,
                onToggleSpeakerCall = onToggleSpeakerCall,
                isCallMuted = isCallMuted,
                isCallSpeaker = isCallSpeaker,
                onAirPlaySelect = onAirPlaySelect,
                onSeekMusic = onSeekMusic,
                onTimerPauseResume = onTimerPauseResume,
                onTimerCancel = onTimerCancel,
                onVoiceMemoStop = onVoiceMemoStop,
                secondaryData = secondaryIslandData,
                onSecondaryClick = onSecondaryIslandClick
            )
        }

        // Right Side: Network Speed meter, VPN, Bluetooth, Cellular bars, Wi-Fi, Real Battery
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 18.dp, top = 4.dp)
                .graphicsLayer {
                    translationX = rightTranslationX
                    alpha = sideAlpha
                    scaleX = sideScale
                    scaleY = sideScale
                }
                .clickable { onOpenControlCenter() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Authentic iOS Network Activity Spinner (replaces raw number meter)
            if (showNetworkSpeed && networkSpeed.isNotBlank() && networkSpeed != "0 KB/s") {
                IOSNetworkActivitySpinner(
                    color = iconColor,
                    modifier = Modifier.padding(end = 2.dp)
                )
            }

            // VPN Active Badge
            if (isVpnConnected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(iconColor.copy(alpha = 0.2f))
                        .padding(horizontal = 2.5.dp, vertical = 0.5.dp)
                ) {
                    Text(
                        text = "VPN",
                        color = iconColor,
                        fontSize = 7.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Bluetooth Connected Audio Accessory Indicator
            if (isBluetoothConnected) {
                Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = "Audio Connected",
                    tint = iconColor.copy(alpha = 0.85f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Connectivity: Cellular & Wi-Fi OR Airplane Mode
            if (isAirplaneMode) {
                Icon(
                    imageVector = Icons.Rounded.AirplanemodeActive,
                    contentDescription = "Airplane Mode",
                    tint = iconColor,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                // Real Cellular Signal Bars (4 bars responsive to cellularBars, slightly enlarged & ultra-precise)
                val barHeights = listOf(4.dp, 7.2.dp, 10.5.dp, 14.dp)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(1.6.dp),
                    modifier = Modifier.height(14.dp)
                ) {
                    barHeights.forEachIndexed { index, barHeight ->
                        val isBarActive = (index + 1) <= cellularBars.coerceIn(0, 4)
                        Box(
                            modifier = Modifier
                                .width(3.2.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(1.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRoundRect(
                                    color = if (isBarActive) iconColor else iconColor.copy(alpha = 0.25f),
                                    cornerRadius = CornerRadius(1.6f, 1.6f)
                                )
                            }
                        }
                    }
                }

                // Wi-Fi or Cellular Data Indicator (slightly enlarged)
                if (isWifiConnected) {
                    Icon(
                        imageVector = Icons.Rounded.Wifi,
                        contentDescription = "Wi-Fi",
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                } else if (networkType.isNotBlank()) {
                    Text(
                        text = networkType,
                        color = iconColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                }
            }

            // Battery Capsule with In-Battery Percentage Text & Fill Level
            val clampedLevel = batteryLevel.coerceIn(0, 100)
            val outerWidth = 27.5.dp
            val outerHeight = 13.8.dp
            val batteryColor = when {
                isBatteryCharging -> Color(0xFF34C759) // Charging Green
                isLowPowerMode -> Color(0xFFFFCC00) // Low Power Yellow
                clampedLevel <= 20 -> Color(0xFFFF3B30) // Low Battery Red
                else -> iconColor
            }

            Box(
                modifier = Modifier
                    .width(outerWidth + 3.2.dp)
                    .height(outerHeight)
                    .clickable { onOpenBatteryInfo?.invoke() ?: onOpenControlCenter() },
                contentAlignment = Alignment.CenterStart
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val outerWidthPx = outerWidth.toPx()
                    val outerHeightPx = outerHeight.toPx()

                    // Outer battery body stroke
                    drawRoundRect(
                        color = iconColor.copy(alpha = 0.45f),
                        size = Size(outerWidthPx, outerHeightPx),
                        cornerRadius = CornerRadius(4.2.dp.toPx(), 4.2.dp.toPx()),
                        style = Stroke(width = 1.3.dp.toPx())
                    )

                    // Battery terminal bump on right
                    drawRoundRect(
                        color = iconColor.copy(alpha = 0.55f),
                        topLeft = Offset(outerWidthPx + 1.dp.toPx(), 4.5.dp.toPx()),
                        size = Size(1.6.dp.toPx(), 4.8.dp.toPx()),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )

                    // Battery fill level
                    val maxFillWidth = outerWidthPx - 3.2.dp.toPx()
                    val currentFillWidth = (maxFillWidth * (clampedLevel / 100f)).coerceAtLeast(2.dp.toPx())

                    drawRoundRect(
                        color = batteryColor.copy(alpha = if (showBatteryPercentage) 0.50f else 0.95f),
                        topLeft = Offset(1.6.dp.toPx(), 1.6.dp.toPx()),
                        size = Size(currentFillWidth, outerHeightPx - 3.2.dp.toPx()),
                        cornerRadius = CornerRadius(2.8.dp.toPx(), 2.8.dp.toPx())
                    )
                }

                // Percentage number or Lightning Bolt rendered STRICTLY INSIDE the battery pill
                Box(
                    modifier = Modifier
                        .width(outerWidth)
                        .height(outerHeight),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBatteryCharging) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FlashOn,
                                contentDescription = "Charging",
                                tint = if (batteryColor == Color(0xFF34C759)) Color.White else iconColor,
                                modifier = Modifier.size(if (showBatteryPercentage) 8.5.dp else 11.dp)
                            )
                            if (showBatteryPercentage) {
                                Text(
                                    text = "$clampedLevel",
                                    fontSize = if (clampedLevel == 100) 7.5.sp else 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (batteryColor == Color(0xFF34C759)) Color.White else iconColor,
                                    letterSpacing = (-0.4).sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else if (showBatteryPercentage) {
                        Text(
                            text = "$clampedLevel",
                            color = iconColor,
                            fontSize = if (clampedLevel == 100) 8.sp else 9.5.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = if (clampedLevel == 100) (-0.6).sp else (-0.2).sp,
                            textAlign = TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(
                                shadow = androidx.compose.ui.graphics.Shadow(
                                    color = if (iconColor == Color.White) Color.Black.copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f),
                                    offset = Offset(0f, 1f),
                                    blurRadius = 1.5f
                                )
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Authentic Apple iOS Network Activity Indicator (UIActivityIndicatorView).
 * Renders an 8-petal radial spinner that rotates smoothly when network requests or syncing occur.
 */
@Composable
fun IOSNetworkActivitySpinner(
    color: Color,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "NetworkSpinnerTransition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "NetworkSpinnerRotation"
    )

    Canvas(
        modifier = modifier
            .size(12.dp)
            .graphicsLayer { rotationZ = rotation }
    ) {
        val count = 8
        val radius = size.minDimension / 2f
        val innerRadius = radius * 0.45f
        val strokeW = radius * 0.32f
        for (i in 0 until count) {
            val angle = Math.toRadians((i * 360f / count).toDouble())
            val alpha = (i + 1f) / count.toFloat()
            val startX = (center.x + innerRadius * Math.cos(angle)).toFloat()
            val startY = (center.y + innerRadius * Math.sin(angle)).toFloat()
            val endX = (center.x + radius * Math.cos(angle)).toFloat()
            val endY = (center.y + radius * Math.sin(angle)).toFloat()

            drawLine(
                color = color.copy(alpha = alpha.coerceIn(0.2f, 1f)),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }
    }
}
