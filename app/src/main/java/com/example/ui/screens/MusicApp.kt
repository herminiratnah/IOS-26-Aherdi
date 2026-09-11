package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SongItem
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

enum class MusicTab {
    LISTEN_NOW,
    BROWSE,
    LIBRARY,
    SEARCH
}

@Composable
fun MusicApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadLocalMusic(context)
    }

    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
    val cellularBars by viewModel.cellularSignalBars.collectAsState()
    val networkType by viewModel.networkType.collectAsState()

    val musicList by viewModel.musicList.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isMusicPlaying.collectAsState()
    val progress by viewModel.musicProgress.collectAsState()
    val currentTimeStr by viewModel.musicCurrentTimeStr.collectAsState()
    val durationStr by viewModel.musicDurationStr.collectAsState()

    var selectedTab by remember { mutableStateOf(MusicTab.LIBRARY) }
    var showFullPlayer by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status bar
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
                isDarkIcons = true,
                isMusicPlaying = isPlaying,
                musicProgress = progress,
                musicCurrentTimeStr = currentTimeStr,
                musicDurationStr = durationStr,
                onMusicPlayPause = { viewModel.toggleMusicPlay() },
                onMusicNext = { viewModel.skipNextSong(context) },
                onMusicPrev = { viewModel.skipPreviousSong(context) },
                onExpandMusic = { viewModel.showMusicInDynamicIsland() }
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (selectedTab) {
                        MusicTab.LISTEN_NOW -> "Listen Now"
                        MusicTab.BROWSE -> "Browse"
                        MusicTab.LIBRARY -> "Library"
                        MusicTab.SEARCH -> "Search"
                    },
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                IconButton(onClick = { viewModel.loadLocalMusic(context) }) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Scan Local Music",
                        tint = Color(0xFFFF2D55)
                    )
                }
            }

            // Body Content
            Box(modifier = Modifier.weight(1f)) {
                val filteredSongs = remember(musicList, searchQuery) {
                    if (searchQuery.isBlank()) musicList
                    else musicList.filter {
                        it.title.contains(searchQuery, ignoreCase = true) ||
                                it.artist.contains(searchQuery, ignoreCase = true)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedTab == MusicTab.SEARCH) {
                        item {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Artists, Songs, Lyrics, and More") },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Downloaded Music & Songs (${filteredSongs.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }

                    items(filteredSongs) { song ->
                        val isCurrent = currentSong?.id == song.id
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isCurrent) Color(0x15FF2D55) else Color.Transparent)
                                .clickable {
                                    viewModel.playSong(song, context)
                                }
                                .padding(horizontal = 8.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Cover art gradient box
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(song.coverGradientStart),
                                                Color(song.coverGradientEnd)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = song.title,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (isCurrent) Color(0xFFFF2D55) else Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = song.artist,
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            if (isCurrent && isPlaying) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = "Playing",
                                    tint = Color(0xFFFF2D55),
                                    modifier = Modifier.size(22.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play",
                                    tint = Color.LightGray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }

            // Mini Player Bar
            if (currentSong != null) {
                Surface(
                    tonalElevation = 6.dp,
                    shadowElevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
                    color = Color(0xFFF9F9FB),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFullPlayer = true }
                ) {
                    Column {
                        // Linear progress indicator
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp),
                            color = Color(0xFFFF2D55),
                            trackColor = Color(0xFFE5E5EA)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        Brush.linearGradient(
                                            listOf(
                                                Color(currentSong!!.coverGradientStart),
                                                Color(currentSong!!.coverGradientEnd)
                                            )
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = currentSong!!.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = currentSong!!.artist,
                                    fontSize = 12.sp,
                                    color = Color.Gray,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(onClick = { viewModel.toggleMusicPlay() }) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.Black,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(onClick = { viewModel.skipNextSong(context) }) {
                                Icon(
                                    imageVector = Icons.Rounded.FastForward,
                                    contentDescription = "Next",
                                    tint = Color.Black,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Navigation Tabs
            NavigationBar(
                containerColor = Color(0xFFF9F9FB),
                tonalElevation = 0.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == MusicTab.LISTEN_NOW,
                    onClick = { selectedTab = MusicTab.LISTEN_NOW },
                    icon = { Icon(Icons.Rounded.PlayCircle, contentDescription = "Listen Now") },
                    label = { Text("Listen Now", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF2D55), selectedTextColor = Color(0xFFFF2D55))
                )
                NavigationBarItem(
                    selected = selectedTab == MusicTab.BROWSE,
                    onClick = { selectedTab = MusicTab.BROWSE },
                    icon = { Icon(Icons.Rounded.GridView, contentDescription = "Browse") },
                    label = { Text("Browse", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF2D55), selectedTextColor = Color(0xFFFF2D55))
                )
                NavigationBarItem(
                    selected = selectedTab == MusicTab.LIBRARY,
                    onClick = { selectedTab = MusicTab.LIBRARY },
                    icon = { Icon(Icons.Rounded.LibraryMusic, contentDescription = "Library") },
                    label = { Text("Library", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF2D55), selectedTextColor = Color(0xFFFF2D55))
                )
                NavigationBarItem(
                    selected = selectedTab == MusicTab.SEARCH,
                    onClick = { selectedTab = MusicTab.SEARCH },
                    icon = { Icon(Icons.Rounded.Search, contentDescription = "Search") },
                    label = { Text("Search", fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(selectedIconColor = Color(0xFFFF2D55), selectedTextColor = Color(0xFFFF2D55))
                )
            }

            HomeBar(onGoHome = { viewModel.closeApp() }, isDark = true)
        }

        // Full Screen Now Playing Sheet Modal
        if (showFullPlayer && currentSong != null) {
            FullScreenMusicPlayer(
                song = currentSong!!,
                isPlaying = isPlaying,
                progress = progress,
                currentTimeStr = currentTimeStr,
                durationStr = durationStr,
                onTogglePlay = { viewModel.toggleMusicPlay() },
                onNext = { viewModel.skipNextSong(context) },
                onPrev = { viewModel.skipPreviousSong(context) },
                onSeek = { viewModel.seekMusicTo(it) },
                onDismiss = { showFullPlayer = false }
            )
        }
    }
}

@Composable
fun FullScreenMusicPlayer(
    song: SongItem,
    isPlaying: Boolean,
    progress: Float,
    currentTimeStr: String,
    durationStr: String,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onSeek: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(song.coverGradientStart).copy(alpha = 0.95f),
                        Color(song.coverGradientEnd).copy(alpha = 0.98f),
                        Color(0xFF1C1C1E)
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Drag handle pill & close
            Box(
                modifier = Modifier
                    .width(42.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Color.White.copy(alpha = 0.4f))
                    .clickable { onDismiss() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Large Animated Album Artwork
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(song.coverGradientStart),
                                Color(song.coverGradientEnd)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(110.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Song Info & Star/Like
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = song.artist,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = {}) {
                    Icon(
                        imageVector = Icons.Rounded.StarBorder,
                        contentDescription = "Favorite",
                        tint = Color.White
                    )
                }
            }

            // Scrubber Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Slider(
                    value = progress,
                    onValueChange = onSeek,
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = currentTimeStr, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                    Text(text = durationStr, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp)
                }
            }

            // Transport Controls: Prev, Play/Pause, Next
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPrev, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .clickable { onTogglePlay() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = Color.Black,
                        modifier = Modifier.size(42.dp)
                    )
                }

                IconButton(onClick = onNext, modifier = Modifier.size(48.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Bottom volume & AirPlay icons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.VolumeDown, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                Slider(
                    value = 0.7f,
                    onValueChange = {},
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = Color.White,
                        inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                    )
                )
                Icon(Icons.Rounded.VolumeUp, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
