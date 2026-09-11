package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.components.IOSWebEngineView
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

data class TVShowItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val tag: String,
    val gradientColors: List<Color>
)

@Composable
fun TVApp(
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

    var isPlayingVideo by remember { mutableStateOf(false) }
    var activeTitle by remember { mutableStateOf("") }
    var isPaused by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Watch Now") } // "Watch Now" or "Live Apple TV+ (Safari)"

    val featuredShows = listOf(
        TVShowItem("1", "Severance", "Season 2 Now Streaming", "APPLE TV+ ORIGINAL", listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))),
        TVShowItem("2", "Ted Lasso", "Kindness makes a comeback", "COMEDY", listOf(Color(0xFF2E7D32), Color(0xFF1B5E20), Color(0xFF111111))),
        TVShowItem("3", "The Morning Show", "Chaos behind the camera", "DRAMA", listOf(Color(0xFF4A148C), Color(0xFF311B92), Color(0xFF0D47A1))),
        TVShowItem("4", "Foundation", "An empire in collapse", "SCI-FI", listOf(Color(0xFFBF360C), Color(0xFF4E342E), Color(0xFF000000)))
    )

    val upNext = listOf(
        Pair("Severance S2:E1", "Hello Ms. Cobel"),
        Pair("Ted Lasso S3:E12", "So Long, Farewell"),
        Pair("Slow Horses S4:E3", "Penny for Your Thoughts"),
        Pair("Silo S2:E2", "Order")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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

            if (isPlayingVideo) {
                // Video Player Screen
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "Now Playing",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                        Text(
                            text = activeTitle,
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Fake Video Screen Frame
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFF1E3C72), Color(0xFF2A5298))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier
                                    .size(64.dp)
                                    .clickable { isPaused = !isPaused }
                            )
                        }

                        // Progress slider mockup
                        LinearProgressIndicator(
                            progress = { 0.35f },
                            modifier = Modifier.fillMaxWidth().height(6.dp),
                            color = Color.White,
                            trackColor = Color.DarkGray
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("18:42", color = Color.Gray, fontSize = 12.sp)
                            Text("-32:18", color = Color.Gray, fontSize = 12.sp)
                        }

                        // Controls
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(30.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = {}) {
                                Icon(Icons.Rounded.Replay10, contentDescription = "Back 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                            IconButton(
                                onClick = { isPaused = !isPaused },
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                            ) {
                                Icon(
                                    imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            IconButton(onClick = {}) {
                                Icon(Icons.Rounded.Forward10, contentDescription = "Forward 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                            }
                        }

                        Button(
                            onClick = {
                                isPlayingVideo = false
                                viewModel.dismissDynamicIsland()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C2E)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Done", color = Color.White)
                        }
                    }
                }
            } else if (selectedTab == "Live Apple TV+ (Safari)") {
                // Real-time Safari Web Engine Integration for Apple TV+
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    // Switcher Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Apple TV+",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selectedTab == "Watch Now") Color(0xFF333333) else Color.Transparent)
                                    .clickable { selectedTab = "Watch Now" }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text("Watch Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (selectedTab == "Live Apple TV+ (Safari)") Color(0xFF007AFF) else Color.Transparent)
                                    .clickable { selectedTab = "Live Apple TV+ (Safari)" }
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text("Live TV+ (Safari)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    IOSWebEngineView(
                        initialUrl = "https://tv.apple.com",
                        title = "Apple TV+",
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            } else {
                // TV Home Screen
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    item {
                        // Header with Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tv,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Apple TV+",
                                    color = Color.White,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF1C1C1E))
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selectedTab == "Watch Now") Color(0xFF333333) else Color.Transparent)
                                        .clickable { selectedTab = "Watch Now" }
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text("Watch Now", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (selectedTab == "Live Apple TV+ (Safari)") Color(0xFF007AFF) else Color.Transparent)
                                        .clickable { selectedTab = "Live Apple TV+ (Safari)" }
                                        .padding(horizontal = 12.dp, vertical = 5.dp)
                                ) {
                                    Text("Live TV+ (Safari)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    // Featured Hero Banner Carousel
                    item {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            items(featuredShows) { show ->
                                Box(
                                    modifier = Modifier
                                        .width(300.dp)
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Brush.verticalGradient(show.gradientColors))
                                        .clickable {
                                            activeTitle = show.title
                                            isPlayingVideo = true
                                            viewModel.showDynamicIslandNotification("Streaming on Apple TV", show.title, "tv", 0xFFFFFFFF)
                                        }
                                        .padding(16.dp),
                                    contentAlignment = Alignment.BottomStart
                                ) {
                                    Column {
                                        Text(
                                            text = show.tag,
                                            color = Color.Cyan,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                        Text(
                                            text = show.title,
                                            color = Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = show.subtitle,
                                            color = Color.LightGray,
                                            fontSize = 13.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Up Next Section
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Up Next",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(upNext) { (show, ep) ->
                                Column(
                                    modifier = Modifier
                                        .width(180.dp)
                                        .clickable {
                                            activeTitle = show
                                            isPlayingVideo = true
                                            viewModel.showDynamicIslandNotification("Apple TV+", show, "tv")
                                        }
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(105.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFF2C2C2E)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.PlayCircleFilled,
                                            contentDescription = "Play",
                                            tint = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.size(40.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = show,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = ep,
                                        color = Color.Gray,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }
                    }

                    // Channels & Services
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Popular Channels",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("Apple TV+", "HBO Max", "Disney+", "Paramount+").forEach { channel ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(70.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF1C1C1E)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = channel,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }

            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = false
            )
        }
    }
}
