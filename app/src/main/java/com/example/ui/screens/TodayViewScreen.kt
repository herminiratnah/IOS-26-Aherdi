package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppId
import com.example.viewmodel.IOSViewModel

@Composable
fun TodayViewScreen(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isCharging by viewModel.isBatteryCharging.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 18.dp)
    ) {
        // Top Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White.copy(alpha = 0.25f))
                .clickable { viewModel.openApp(AppId.SAFARI) }
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "Search",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. Batteries 4-Quadrant Widget
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Batteries",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // iPhone
                            BatteryDeviceItem(
                                name = "iPhone",
                                level = batteryLevel,
                                isCharging = isCharging,
                                icon = Icons.Rounded.PhoneIphone
                            )
                            // Apple Watch
                            BatteryDeviceItem(
                                name = "Apple Watch",
                                level = 92,
                                isCharging = false,
                                icon = Icons.Rounded.Watch
                            )
                            // AirPods Pro
                            BatteryDeviceItem(
                                name = "AirPods Pro",
                                level = 85,
                                isCharging = true,
                                icon = Icons.Rounded.Headphones
                            )
                            // Case
                            BatteryDeviceItem(
                                name = "Case",
                                level = 74,
                                isCharging = false,
                                icon = Icons.Rounded.BatteryChargingFull
                            )
                        }
                    }
                }
            }

            // 2. Calendar Up Next Widget
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                        .clickable { viewModel.openApp(AppId.CALENDAR) }
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFFF3B30))
                                )
                                Text(
                                    text = "CALENDAR",
                                    color = Color(0xFFFF3B30),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                            Text(
                                text = "Team Standup",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "10:30 AM – 11:15 AM • Zoom",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 13.sp
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("WED", color = Color(0xFFFF3B30), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                Text("15", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // 3. Stocks Widget
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Stocks", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Text("Markets Open", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                        }

                        StockRow(symbol = "AAPL", name = "Apple Inc.", price = "$234.50", change = "+2.4%", isUp = true)
                        StockRow(symbol = "NVDA", name = "NVIDIA Corp.", price = "$128.40", change = "+3.1%", isUp = true)
                        StockRow(symbol = "SPY", name = "S&P 500 ETF", price = "$582.10", change = "+0.8%", isUp = true)
                    }
                }
            }

            // 4. Screen Time Widget
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color.White.copy(alpha = 0.3f))
                        .clickable { viewModel.openApp(AppId.SETTINGS) }
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Screen Time", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Icon(Icons.Rounded.HourglassBottom, contentDescription = null, tint = Color(0xFF5856D6), modifier = Modifier.size(16.dp))
                        }
                        Text("2h 45m", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("18% less than yesterday", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)

                        // Multi-color category bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                        ) {
                            Box(modifier = Modifier.weight(0.45f).fillMaxHeight().background(Color(0xFF007AFF))) // Social
                            Box(modifier = Modifier.weight(0.35f).fillMaxHeight().background(Color(0xFF5856D6))) // Productivity
                            Box(modifier = Modifier.weight(0.20f).fillMaxHeight().background(Color(0xFFFF9500))) // Entertainment
                        }
                    }
                }
            }

            // Edit button
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.25f))
                            .padding(horizontal = 24.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Edit",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun BatteryDeviceItem(
    name: String,
    level: Int,
    isCharging: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Text(
            text = "$level%",
            color = if (isCharging) Color(0xFF34C759) else Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = name,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp
        )
    }
}

@Composable
fun StockRow(
    symbol: String,
    name: String,
    price: String,
    change: String,
    isUp: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(symbol, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(name, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(price, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isUp) Color(0xFF34C759) else Color(0xFFFF3B30))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(change, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
