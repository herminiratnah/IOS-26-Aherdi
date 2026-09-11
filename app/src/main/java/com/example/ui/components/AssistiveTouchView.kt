package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppId
import com.example.viewmodel.IOSViewModel
import kotlin.math.roundToInt

@Composable
fun AssistiveTouchView(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val isEnabled by viewModel.isAssistiveTouchEnabled.collectAsState()
    if (!isEnabled) return

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current

    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    var offsetX by remember { mutableFloatStateOf(screenWidthPx - with(density) { 72.dp.toPx() }) }
    var offsetY by remember { mutableFloatStateOf(screenHeightPx * 0.5f) }
    var isMenuOpen by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        // Full screen scrim when AssistiveTouch radial menu is open
        if (isMenuOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { isMenuOpen = false }
                    )
            ) {
                // Centered Radial 8-Button Menu
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(280.dp)
                        .clip(RoundedCornerShape(36.dp))
                        .background(Color(0xFF2C2C2E).copy(alpha = 0.94f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(36.dp))
                        .padding(16.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* prevent dismiss */ }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Top Row: Notification Center
                        AssistiveMenuItem(
                            icon = Icons.Rounded.Notifications,
                            label = "Notifications",
                            onClick = {
                                isMenuOpen = false
                                viewModel.toggleNotificationCenter()
                            }
                        )

                        // Middle Row: Siri, Home, Control Center
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistiveMenuItem(
                                icon = Icons.Rounded.AutoAwesome,
                                label = "Siri",
                                onClick = {
                                    isMenuOpen = false
                                    viewModel.openSiri()
                                }
                            )

                            // Center Circle: Home (SpringBoard)
                            AssistiveMenuItem(
                                icon = Icons.Rounded.Home,
                                label = "Home",
                                isPrimary = true,
                                onClick = {
                                    isMenuOpen = false
                                    viewModel.closeApp()
                                }
                            )

                            AssistiveMenuItem(
                                icon = Icons.Rounded.Tune,
                                label = "Control Center",
                                onClick = {
                                    isMenuOpen = false
                                    viewModel.toggleControlCenter()
                                }
                            )
                        }

                        // Bottom Row: Flashlight, Camera, Lock
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AssistiveMenuItem(
                                icon = Icons.Rounded.FlashlightOn,
                                label = "Flashlight",
                                onClick = {
                                    isMenuOpen = false
                                    viewModel.toggleFlashlight(context)
                                }
                            )

                            AssistiveMenuItem(
                                icon = Icons.Rounded.PhotoCamera,
                                label = "Camera",
                                onClick = {
                                    isMenuOpen = false
                                    viewModel.openApp(AppId.CAMERA)
                                }
                            )

                            AssistiveMenuItem(
                                icon = Icons.Rounded.Lock,
                                label = "Lock",
                                onClick = {
                                    isMenuOpen = false
                                    viewModel.toggleNotificationCenter()
                                }
                            )
                        }
                    }
                }
            }
        }

        // Draggable Floating AssistiveTouch Circle Button
        if (!isMenuOpen) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                    .size(56.dp)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                // Snap smoothly to nearest screen edge (left or right)
                                val snapPadding = with(density) { 12.dp.toPx() }
                                val iconWidth = with(density) { 56.dp.toPx() }
                                offsetX = if (offsetX < screenWidthPx / 2) {
                                    snapPadding
                                } else {
                                    screenWidthPx - iconWidth - snapPadding
                                }
                            }
                        ) { change, dragAmount ->
                            change.consume()
                            offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidthPx - with(density) { 56.dp.toPx() })
                            offsetY = (offsetY + dragAmount.y).coerceIn(with(density) { 60.dp.toPx() }, screenHeightPx - with(density) { 100.dp.toPx() })
                        }
                    }
                    .clickable {
                        isMenuOpen = true
                    },
                contentAlignment = Alignment.Center
            ) {
                // Outer Translucent Pill
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .border(1.5.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    // Concentric Inner Circles
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.85f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AssistiveMenuItem(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick
        )
    ) {
        Box(
            modifier = Modifier
                .size(if (isPrimary) 52.dp else 46.dp)
                .clip(CircleShape)
                .background(if (isPrimary) Color(0xFF007AFF) else Color.White.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(if (isPrimary) 26.dp else 22.dp)
            )
        }
        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
