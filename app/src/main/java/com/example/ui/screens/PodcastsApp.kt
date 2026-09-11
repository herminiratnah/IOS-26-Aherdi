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
import androidx.compose.ui.draw.shadow
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

data class PodcastShow(
    val id: String,
    val title: String,
    val host: String,
    val episodeTitle: String,
    val duration: String,
    val gradientColors: List<Color>
)

@Composable
fun PodcastsApp(
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

    var activeTab by remember { mutableStateOf("Featured") } // "Featured" or "Online Podcasts (Safari)"
    var isPlaying by remember { mutableStateOf(false) }
    var activeEpisode by remember { mutableStateOf("Huberman Lab: Optimize Your Brain") }
    var activeHost by remember { mutableStateOf("Andrew Huberman, Ph.D.") }
    var playbackSpeed by remember { mutableStateOf("1x") }

    val shows = listOf(
        PodcastShow(
            "1", "Huberman Lab", "Andrew Huberman",
            "Optimize Your Sleep & Energy System", "1 hr 45 min",
            listOf(Color(0xFF4A148C), Color(0xFF7B1FA2))
        ),
        PodcastShow(
            "2", "The Daily", "The New York Times",
            "Inside the Tech Breakthrough of 2026", "28 min",
            listOf(Color(0xFF1A237E), Color(0xFF283593))
        ),
        PodcastShow(
            "3", "Lex Fridman Podcast", "Lex Fridman",
            "Future of AI, Robotics & Consciousness", "3 hr 10 min",
            listOf(Color(0xFF212121), Color(0xFF424242))
        ),
        PodcastShow(
            "4", "Stuff You Should Know", "iHeartPodcasts",
            "How Champagne Actually Works", "48 min",
            listOf(Color(0xFFE65100), Color(0xFFF57C00))
        )
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

            // Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Podcasts",
                    color = Color.White,
                    fontSize = 28.sp,
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
                            .background(if (activeTab == "Featured") Color(0xFF8E44AD) else Color.Transparent)
                            .clickable { activeTab = "Featured" }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text("Featured", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (activeTab == "Online Podcasts (Safari)") Color(0xFF8E44AD) else Color.Transparent)
                            .clickable { activeTab = "Online Podcasts (Safari)" }
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Language, contentDescription = null, tint = Color.White, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Safari Live", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (activeTab == "Online Podcasts (Safari)") {
                IOSWebEngineView(
                    initialUrl = "https://podcasts.apple.com",
                    title = "Apple Podcasts Live",
                    modifier = Modifier.weight(1f),
                    showHeaderBar = true
                )
            } else {
                LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "UP NEXT",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                items(shows) { show ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1C1C1E))
                            .clickable {
                                activeEpisode = show.episodeTitle
                                activeHost = show.host
                                isPlaying = true
                                viewModel.showDynamicIslandNotification("Apple Podcasts", show.title, "podcasts", 0xFF9C27B0)
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Brush.linearGradient(show.gradientColors)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Podcasts,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = show.title,
                                color = Color(0xFFAF52DE),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = show.episodeTitle,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "${show.host} • ${show.duration}",
                                color = Color.Gray,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(
                            onClick = {
                                activeEpisode = show.episodeTitle
                                activeHost = show.host
                                isPlaying = !isPlaying
                                if (isPlaying) {
                                    viewModel.showDynamicIslandNotification("Apple Podcasts", show.title, "podcasts", 0xFF9C27B0)
                                } else {
                                    viewModel.dismissDynamicIsland()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isPlaying && activeEpisode == show.episodeTitle) Icons.Rounded.PauseCircle else Icons.Rounded.PlayCircle,
                                contentDescription = "Play",
                                tint = Color(0xFFAF52DE),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }
            }

            // Mini Player Bar at bottom
            if (isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .shadow(8.dp, RoundedCornerShape(14.dp))
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2C2C2E))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF9C27B0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Podcasts, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = activeEpisode,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = activeHost,
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }

                        // Speed button
                        Text(
                            text = playbackSpeed,
                            color = Color(0xFFAF52DE),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    playbackSpeed = when (playbackSpeed) {
                                        "1x" -> "1.25x"
                                        "1.25x" -> "1.5x"
                                        "1.5x" -> "2x"
                                        else -> "1x"
                                    }
                                }
                                .padding(horizontal = 6.dp)
                        )

                        IconButton(onClick = { isPlaying = false }) {
                            Icon(Icons.Rounded.Pause, contentDescription = "Pause", tint = Color.White)
                        }
                    }
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
