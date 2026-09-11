package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
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
    onMusicPlayPause: () -> Unit = {},
    onMusicNext: () -> Unit = {},
    onMusicPrev: () -> Unit = {},
    onExpandMusic: () -> Unit = {},
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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp)
    ) {
        // Left Side: Real Device Time (Tap or drag down to open Notification Center)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 24.dp)
                .graphicsLayer {
                    translationX = leftTranslationX
                    alpha = sideAlpha
                    scaleX = sideScale
                    scaleY = sideScale
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 20) onOpenNotificationCenter()
                    }
                }
                .clickable { onOpenNotificationCenter() }
        ) {
            Text(
                text = currentTime,
                color = iconColor,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.2).sp
            )
        }

        // Center: Dynamic Island
        Box(
            modifier = Modifier.align(Alignment.Center)
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
                onExpandMusic = onExpandMusic
            )
        }

        // Right Side: Real Signal bars, Network/Wifi, Real Battery (Tap or drag down to open Control Center)
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 20.dp)
                .graphicsLayer {
                    translationX = rightTranslationX
                    alpha = sideAlpha
                    scaleX = sideScale
                    scaleY = sideScale
                }
                .pointerInput(Unit) {
                    detectVerticalDragGestures { _, dragAmount ->
                        if (dragAmount > 20) onOpenControlCenter()
                    }
                }
                .clickable { onOpenControlCenter() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            // Connectivity: Cellular & Wi-Fi OR Airplane Mode
            if (isAirplaneMode) {
                Icon(
                    imageVector = Icons.Rounded.AirplanemodeActive,
                    contentDescription = "Airplane Mode",
                    tint = iconColor,
                    modifier = Modifier.size(15.dp)
                )
            } else {
                // Real Cellular Signal Bars (4 bars responsive to cellularBars)
                val barHeights = listOf(3.dp, 5.5.dp, 8.dp, 10.5.dp)
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(1.5.dp),
                    modifier = Modifier.height(11.dp)
                ) {
                    barHeights.forEachIndexed { index, barHeight ->
                        val isBarActive = (index + 1) <= cellularBars.coerceIn(0, 4)
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(barHeight)
                                .clip(RoundedCornerShape(0.8.dp))
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRoundRect(
                                    color = if (isBarActive) iconColor else iconColor.copy(alpha = 0.25f),
                                    cornerRadius = CornerRadius(2f, 2f)
                                )
                            }
                        }
                    }
                }

                // Wi-Fi or Cellular Data Indicator
                if (isWifiConnected) {
                    Icon(
                        imageVector = Icons.Rounded.Wifi,
                        contentDescription = "Wi-Fi",
                        tint = iconColor,
                        modifier = Modifier.size(15.dp)
                    )
                } else if (networkType.isNotBlank()) {
                    Text(
                        text = networkType,
                        color = iconColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.3).sp
                    )
                }
            }

            // Real Battery Pill with Fill & Charging State
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (isBatteryCharging) {
                    Text(
                        text = "⚡",
                        fontSize = 10.sp,
                        color = Color(0xFF34C759)
                    )
                }

                Canvas(modifier = Modifier.size(width = 25.dp, height = 12.dp)) {
                    val outerWidth = 21.dp.toPx()
                    val outerHeight = 11.5.dp.toPx()

                    // Outer battery body
                    drawRoundRect(
                        color = iconColor.copy(alpha = 0.4f),
                        size = Size(outerWidth, outerHeight),
                        cornerRadius = CornerRadius(3.5.dp.toPx(), 3.5.dp.toPx()),
                        style = Stroke(width = 1.2.dp.toPx())
                    )

                    // Battery terminal bump
                    drawRoundRect(
                        color = iconColor.copy(alpha = 0.5f),
                        topLeft = Offset(outerWidth + 1.2.dp.toPx(), 4.dp.toPx()),
                        size = Size(1.8.dp.toPx(), 3.5.dp.toPx()),
                        cornerRadius = CornerRadius(1.dp.toPx(), 1.dp.toPx())
                    )

                    // Battery fill level
                    val clampedLevel = batteryLevel.coerceIn(0, 100)
                    val maxFillWidth = outerWidth - 3.5.dp.toPx()
                    val currentFillWidth = (maxFillWidth * (clampedLevel / 100f)).coerceAtLeast(1.5.dp.toPx())

                    val batteryColor = when {
                        isBatteryCharging -> Color(0xFF34C759) // Charging Green
                        clampedLevel <= 20 -> Color(0xFFFF3B30) // Low Battery Red
                        else -> iconColor
                    }

                    drawRoundRect(
                        color = batteryColor,
                        topLeft = Offset(1.8.dp.toPx(), 1.8.dp.toPx()),
                        size = Size(currentFillWidth, outerHeight - 3.6.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )
                }
            }
        }
    }
}
