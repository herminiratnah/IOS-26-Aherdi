package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.components.IOSWebEngineView
import com.example.viewmodel.IOSViewModel

@Composable
fun WeatherApp(
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
    val airplaneMode by viewModel.airplaneMode.collectAsState()

    var activeTab by remember { mutableStateOf("Forecast") } // "Forecast" or "Live Radar (Safari)"

    val hourlyForecast = remember {
        listOf(
            Triple("Now", Icons.Rounded.WbCloudy, "28°"),
            Triple("10 AM", Icons.Rounded.WbSunny, "29°"),
            Triple("11 AM", Icons.Rounded.WbSunny, "30°"),
            Triple("12 PM", Icons.Rounded.WbSunny, "31°"),
            Triple("1 PM", Icons.Rounded.WbCloudy, "31°"),
            Triple("2 PM", Icons.Rounded.Thunderstorm, "29°"),
            Triple("3 PM", Icons.Rounded.WaterDrop, "28°")
        )
    }

    val dailyForecast = remember {
        listOf(
            Triple("Today", "24°", "32°"),
            Triple("Wed", "23°", "31°"),
            Triple("Thu", "24°", "33°"),
            Triple("Fri", "24°", "32°"),
            Triple("Sat", "25°", "34°"),
            Triple("Sun", "24°", "31°")
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF2980B9), Color(0xFF6DD5FA), Color(0xFFB0E0E6))
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status bar (white icons)
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
                isAirplaneMode = airplaneMode,
                isDarkIcons = false
            )

            // Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (activeTab == "Forecast") Color.White else Color.Transparent)
                            .clickable { activeTab = "Forecast" }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            "Forecast",
                            color = if (activeTab == "Forecast") Color(0xFF2980B9) else Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (activeTab == "Live Radar (Safari)") Color.White else Color.Transparent)
                            .clickable { activeTab = "Live Radar (Safari)" }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Rounded.Language,
                                contentDescription = null,
                                tint = if (activeTab == "Live Radar (Safari)") Color(0xFF2980B9) else Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Live Radar (Safari)",
                                color = if (activeTab == "Live Radar (Safari)") Color(0xFF2980B9) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (activeTab == "Live Radar (Safari)") {
                IOSWebEngineView(
                    initialUrl = "https://weather.com",
                    title = "The Weather Channel Live Radar",
                    modifier = Modifier.weight(1f),
                    showHeaderBar = true
                )
            } else {
                LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header matching Image 13
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Jakarta",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White
                    )
                    Text(
                        text = "28°",
                        fontSize = 86.sp,
                        fontWeight = FontWeight.Thin,
                        color = Color.White
                    )
                    Text(
                        text = "Mostly Cloudy",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Text(
                        text = "H:32°  L:24°",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Hourly Forecast Card matching Image 13
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x33000000))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Mostly cloudy conditions expected around 14:00.",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                            HorizontalDivider(color = Color(0x33FFFFFF), thickness = 0.5.dp)

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                                items(hourlyForecast) { item ->
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(item.first, color = Color.White, fontSize = 13.sp)
                                        Icon(item.second, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                        Text(item.third, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }
                }

                // 10-Day Forecast Card
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Color(0x33000000))
                            .padding(14.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Rounded.CalendarToday, contentDescription = null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(14.dp))
                                Text("10-DAY FORECAST", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            HorizontalDivider(color = Color(0x33FFFFFF), thickness = 0.5.dp)

                            dailyForecast.forEach { f ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(f.first, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(60.dp))
                                    Icon(Icons.Rounded.WbSunny, contentDescription = null, tint = Color(0xFFFFCC00), modifier = Modifier.size(20.dp))
                                    Text(f.second, color = Color.White.copy(alpha = 0.7f), fontSize = 15.sp)
                                    Box(
                                        modifier = Modifier
                                            .width(90.dp)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(
                                                Brush.horizontalGradient(
                                                    listOf(Color(0xFF34C759), Color(0xFFFFCC00), Color(0xFFFF9500))
                                                )
                                            )
                                    )
                                    Text(f.third, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
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
