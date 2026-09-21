package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.IOSViewModel

/**
 * Genuine iOS 17/18 StandBy Mode:
 * Activates when charging and placed horizontally/landscape, or triggered from Settings preview.
 * Features massive digital clock, live Bandung weather widget, battery status, and ambient Red Night Mode.
 */
@Composable
fun IOSStandByOverlay(
    viewModel: IOSViewModel,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val currentDate by viewModel.currentDateFormatted.collectAsState()
    val weatherData by viewModel.weatherData.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isCharging by viewModel.isBatteryCharging.collectAsState()
    val nightModeEnabled by viewModel.standByNightMode.collectAsState()

    var isRedModeActive by remember { mutableStateOf(nightModeEnabled) }

    val accentColor = if (isRedModeActive) Color(0xFFFF3B30) else Color.White
    val secondaryColor = if (isRedModeActive) Color(0xAAFF3B30) else Color(0xAAFFFFFF)
    val cardBg = if (isRedModeActive) Color(0x22FF3B30) else Color(0x22FFFFFF)

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn() + expandIn(),
        exit = fadeOut() + shrinkOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // LEFT PANE: Massive iOS Digital Clock Widget
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .background(cardBg)
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = currentDate.uppercase(),
                            color = secondaryColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentTime,
                            color = accentColor,
                            fontSize = 72.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-2).sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Alarm,
                                contentDescription = null,
                                tint = secondaryColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "07:00",
                                color = secondaryColor,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // RIGHT PANE: Bandung Weather & Battery Widget
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(28.dp))
                        .background(cardBg)
                        .padding(20.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Weather Section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column {
                                Text(
                                    text = weatherData.city,
                                    color = accentColor,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "${weatherData.currentTemp}°",
                                    color = accentColor,
                                    fontSize = 44.sp,
                                    fontWeight = FontWeight.Light
                                )
                                Text(
                                    text = weatherData.condition,
                                    color = secondaryColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Icon(
                                imageVector = when {
                                    weatherData.weatherCode in 61..82 -> Icons.Rounded.WaterDrop
                                    weatherData.weatherCode == 0 -> Icons.Rounded.WbSunny
                                    else -> Icons.Rounded.WbCloudy
                                },
                                contentDescription = null,
                                tint = if (isRedModeActive) Color(0xFFFF3B30) else Color(0xFFFFCC00),
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        // Battery & StandBy Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isCharging) Icons.Rounded.BatteryChargingFull else Icons.Rounded.BatteryStd,
                                    contentDescription = null,
                                    tint = if (isRedModeActive) Color(0xFFFF3B30) else Color(0xFF34C759),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "$batteryLevel% ${if (isCharging) "Charging" else ""}",
                                    color = accentColor,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Night Mode quick toggle
                            IconButton(
                                onClick = { isRedModeActive = !isRedModeActive },
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (isRedModeActive) Color(0x44FF3B30) else Color(0x33FFFFFF))
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.NightlightRound,
                                    contentDescription = "Night Mode",
                                    tint = if (isRedModeActive) Color(0xFFFF3B30) else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Minimal dismiss indicator
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close StandBy",
                        tint = secondaryColor,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}
