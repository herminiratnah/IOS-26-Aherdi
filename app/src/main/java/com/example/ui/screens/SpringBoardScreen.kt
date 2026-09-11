package com.example.ui.screens

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppId
import com.example.model.AppItem
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpringBoardScreen(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val currentWallpaperRes by viewModel.currentWallpaperRes.collectAsState()
    val currentWallpaperUri by viewModel.currentWallpaperUri.collectAsState()
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val currentDayOfWeek by viewModel.currentDayOfWeek.collectAsState()
    val currentDayOfMonth by viewModel.currentDayOfMonth.collectAsState()
    val currentHourFloat by viewModel.currentHourFloat.collectAsState()
    val currentMinuteFloat by viewModel.currentMinuteFloat.collectAsState()
    val currentSecondFloat by viewModel.currentSecondFloat.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
    val cellularBars by viewModel.cellularSignalBars.collectAsState()
    val networkType by viewModel.networkType.collectAsState()
    val airplaneMode by viewModel.airplaneMode.collectAsState()
    val isMusicPlaying by viewModel.isMusicPlaying.collectAsState()
    val musicProgress by viewModel.musicProgress.collectAsState()
    val musicCurrentTimeStr by viewModel.musicCurrentTimeStr.collectAsState()
    val musicDurationStr by viewModel.musicDurationStr.collectAsState()
    val context = LocalContext.current

    // Real Notification counts from ViewModel
    val messagesThreads by viewModel.messagesThreads.collectAsState()
    val unreadMessagesCount = remember(messagesThreads) {
        messagesThreads.sumOf { it.unreadCount }
    }
    val callRecords by viewModel.callRecords.collectAsState()
    val missedCallsCount = remember(callRecords) {
        callRecords.count { it.isMissed }
    }

    val photoItems by viewModel.photoItems.collectAsState()
    val activePhotoWidgetIndex by viewModel.activePhotoWidgetIndex.collectAsState()
    val selectedWidgetPhotoIds by viewModel.selectedWidgetPhotoIds.collectAsState()
    var isPhotoWidgetPickerOpen by remember { mutableStateOf(false) }

    val currentWidgetPhoto = remember(photoItems, activePhotoWidgetIndex, selectedWidgetPhotoIds) {
        val filtered = if (selectedWidgetPhotoIds.isNotEmpty()) {
            photoItems.filter { it.id in selectedWidgetPhotoIds }
        } else {
            photoItems
        }
        if (filtered.isNotEmpty()) {
            filtered.getOrNull(activePhotoWidgetIndex % filtered.size) ?: filtered.first()
        } else {
            null
        }
    }

    // 3-Page iOS Launcher (0 = Today View / Widgets, 1 = Main Home Screen, 2 = App Library)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    // 3D Touch (Haptic Touch) & Jiggle Mode States
    var selected3DApp by remember { mutableStateOf<AppItem?>(null) }
    var isJiggleMode by remember { mutableStateOf(false) }

    // Jiggle Mode Oscillation Animation
    val infiniteTransition = rememberInfiniteTransition(label = "jiggleTransition")
    val jiggleRotation by infiniteTransition.animateFloat(
        initialValue = -2.2f,
        targetValue = 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "jiggleRotation"
    )

    // 4x4 Grid Apps matching authentic iOS 17/18 Home Screen
    val gridApps = remember(unreadMessagesCount, missedCallsCount) {
        listOf(
            AppItem(AppId.FACETIME, "FaceTime", IOSGreen, Icons.Rounded.Videocam),
            AppItem(AppId.CALENDAR, "Calendar", Color.White, null),
            AppItem(AppId.PHOTOS, "Photos", Color.White, null),
            AppItem(AppId.CAMERA, "Camera", Color(0xFF636366), Icons.Rounded.CameraAlt),

            AppItem(AppId.MAIL, "Mail", Color(0xFF007AFF), Icons.Rounded.Email, badgeCount = 2),
            AppItem(AppId.NOTES, "Notes", Color.White, null),
            AppItem(AppId.REMINDERS, "Reminders", Color.White, null),
            AppItem(AppId.CLOCK, "Clock", Color.Black, null),

            AppItem(AppId.TV, "TV", Color.Black, Icons.Rounded.Tv),
            AppItem(AppId.PODCASTS, "Podcasts", Color(0xFF8E44AD), Icons.Rounded.Podcasts),
            AppItem(AppId.APP_STORE, "App Store", Color(0xFF1D6EEB), Icons.Rounded.ShoppingBag),
            AppItem(AppId.MAPS, "Maps", Color(0xFF2ECC71), Icons.Rounded.Map),

            AppItem(AppId.HEALTH, "Health", Color.White, Icons.Rounded.Favorite, badgeCount = 0),
            AppItem(AppId.WALLET, "Wallet", Color.Black, Icons.Rounded.AccountBalanceWallet),
            AppItem(AppId.SETTINGS, "Settings", Color(0xFF8E8E93), Icons.Rounded.Settings, badgeCount = 1),
            AppItem(AppId.ROOT_TOOLS, "iRoot SE", Color(0xFF5856D6), Icons.Rounded.Security)
        )
    }

    // Dock Apps
    val dockApps = remember(unreadMessagesCount, missedCallsCount) {
        listOf(
            AppItem(AppId.PHONE, "Phone", IOSGreen, Icons.Rounded.Call, badgeCount = missedCallsCount, isDock = true),
            AppItem(AppId.SAFARI, "Safari", Color.White, Icons.Rounded.Explore, isDock = true),
            AppItem(AppId.MESSAGES, "Messages", IOSGreen, Icons.Rounded.ChatBubble, badgeCount = unreadMessagesCount, isDock = true),
            AppItem(AppId.MUSIC, "Music", Color(0xFFFF2D55), Icons.Rounded.MusicNote, isDock = true)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(enabled = isJiggleMode) { isJiggleMode = false }
    ) {
        // Dynamic Wallpaper background
        if (!currentWallpaperUri.isNullOrBlank()) {
            AsyncImage(
                model = currentWallpaperUri,
                contentDescription = "iOS Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = if (currentWallpaperRes != 0) currentWallpaperRes else R.drawable.ios26_wallpaper),
                contentDescription = "iOS Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Status Bar with Dynamic Island
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
                isDarkIcons = false,
                isMusicPlaying = isMusicPlaying,
                musicProgress = musicProgress,
                musicCurrentTimeStr = musicCurrentTimeStr,
                musicDurationStr = musicDurationStr,
                onMusicPlayPause = { viewModel.toggleMusicPlay() },
                onMusicNext = { viewModel.skipNextSong(context) },
                onMusicPrev = { viewModel.skipPreviousSong(context) },
                onExpandMusic = { viewModel.showMusicInDynamicIsland() }
            )

            // Top Bar with "Done" button during Jiggle Mode
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (isJiggleMode) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.85f))
                            .clickable { isJiggleMode = false }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Done",
                            color = IOSBlue,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 3-Page Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                when (page) {
                    0 -> {
                        // Page 0: Today View / Widgets
                        TodayViewScreen(viewModel = viewModel)
                    }
                    1 -> {
                        // Page 1: Main Home Screen (Weather + Photos + 4x4 Grid)
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Widgets Row: Weather (left) & Photos (right)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 22.dp),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Weather Widget
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(148.dp)
                                        .graphicsLayer {
                                            if (isJiggleMode) rotationZ = jiggleRotation * 0.4f
                                        }
                                        .clip(RoundedCornerShape(26.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                listOf(Color(0xFF2980B9), Color(0xFF6DD5FA))
                                            )
                                        )
                                        .clickable {
                                            if (!isJiggleMode) viewModel.openApp(AppId.WEATHER)
                                        }
                                        .padding(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column {
                                            Text(
                                                text = "Jakarta",
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = "28°",
                                                color = Color.White,
                                                fontSize = 36.sp,
                                                fontWeight = FontWeight.Light,
                                                letterSpacing = (-1).sp
                                            )
                                        }
                                        Column {
                                            Icon(
                                                imageVector = Icons.Rounded.WbCloudy,
                                                contentDescription = null,
                                                tint = Color.White.copy(alpha = 0.9f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "Mostly Cloudy",
                                                color = Color.White,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "H:32°  L:24°",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }

                                // Photos Slideshow Widget
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(148.dp)
                                        .graphicsLayer {
                                            if (isJiggleMode) rotationZ = -jiggleRotation * 0.4f
                                        }
                                        .clip(RoundedCornerShape(26.dp))
                                        .clickable {
                                            if (!isJiggleMode) viewModel.openApp(AppId.PHOTOS)
                                        }
                                ) {
                                    if (currentWidgetPhoto?.imageUri != null) {
                                        AsyncImage(
                                            model = currentWidgetPhoto.imageUri,
                                            contentDescription = "Photos Widget",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else if (currentWidgetPhoto?.drawableRes != null && currentWidgetPhoto.drawableRes != 0) {
                                        Image(
                                            painter = painterResource(id = currentWidgetPhoto.drawableRes),
                                            contentDescription = "Photos Widget",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Image(
                                            painter = painterResource(id = R.drawable.photo_widget_nature),
                                            contentDescription = "Photos Widget",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    // Top right control buttons (Next & Pick)
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x66000000))
                                                .clickable {
                                                    viewModel.nextWidgetPhoto()
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.SkipNext,
                                                contentDescription = "Next Photo",
                                                tint = Color.White,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Box(
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(Color(0x66000000))
                                                .clickable {
                                                    isPhotoWidgetPickerOpen = true
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Tune,
                                                contentDescription = "Select Photos",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    // Bottom gradient label
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .fillMaxWidth()
                                            .background(
                                                Brush.verticalGradient(
                                                    listOf(Color.Transparent, Color(0x99000000))
                                                )
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Column {
                                            Text(
                                                text = currentWidgetPhoto?.title ?: "Memories",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = currentWidgetPhoto?.date ?: "Photos",
                                                color = Color.White.copy(alpha = 0.8f),
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // 4x4 Grid Apps
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp)
                                    .weight(1f),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                for (row in 0 until 4) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        for (col in 0 until 4) {
                                            val index = row * 4 + col
                                            if (index < gridApps.size) {
                                                val app = gridApps[index]
                                                val rotationAngle = if (isJiggleMode) {
                                                    if ((row + col) % 2 == 0) jiggleRotation else -jiggleRotation
                                                } else 0f

                                                AppIconItem(
                                                    app = app,
                                                    dayOfWeek = currentDayOfWeek,
                                                    dayOfMonth = currentDayOfMonth,
                                                    hourFloat = currentHourFloat,
                                                    minuteFloat = currentMinuteFloat,
                                                    secondFloat = currentSecondFloat,
                                                    isJiggleMode = isJiggleMode,
                                                    rotationAngle = rotationAngle,
                                                    onClick = {
                                                        if (isJiggleMode) {
                                                            isJiggleMode = false
                                                        } else {
                                                            viewModel.openApp(app.id)
                                                        }
                                                    },
                                                    onLongClick = {
                                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                                        selected3DApp = app
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        // Page 2: App Library
                        AppLibraryScreen(viewModel = viewModel)
                    }
                }
            }

            // Page Dots & Search Pill
            Row(
                modifier = Modifier.padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Page 0 dot
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == 0) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (pagerState.currentPage == 0) 1f else 0.45f))
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(0) }
                        }
                )

                // Page 1 dot or Search pill
                if (pagerState.currentPage == 1) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x35000000))
                            .clickable {
                                viewModel.openSpotlight()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "Search",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(if (pagerState.currentPage == 1) 8.dp else 6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (pagerState.currentPage == 1) 1f else 0.45f))
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(1) }
                            }
                    )
                }

                // Page 2 dot
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == 2) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (pagerState.currentPage == 2) 1f else 0.45f))
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(2) }
                        }
                )
            }

            // Persistent Bottom Translucent Dock (Visible on Page 0 and Page 1)
            if (pagerState.currentPage != 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(34.dp))
                        .background(Color(0x40FFFFFF))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        dockApps.forEachIndexed { index, app ->
                            val rotationAngle = if (isJiggleMode) {
                                if (index % 2 == 0) jiggleRotation else -jiggleRotation
                            } else 0f

                            AppIconItem(
                                app = app,
                                showLabel = false,
                                isJiggleMode = isJiggleMode,
                                rotationAngle = rotationAngle,
                                onClick = {
                                    if (isJiggleMode) isJiggleMode = false else viewModel.openApp(app.id)
                                },
                                onLongClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    selected3DApp = app
                                }
                            )
                        }
                    }
                }
            }

            // Home Bar indicator
            HomeBar(
                onGoHome = {
                    if (isJiggleMode) {
                        isJiggleMode = false
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(1)
                        }
                    }
                },
                onHoldForSiri = {
                    viewModel.openSiri()
                },
                isDark = false
            )
        }

        // 3D Touch (Haptic Touch) Context Menu Modal Overlay
        AnimatedVisibility(
            visible = selected3DApp != null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            val app = selected3DApp
            if (app != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable { selected3DApp = null },
                    contentAlignment = Alignment.Center
                ) {
                    IOS3DTouchContextMenu(
                        app = app,
                        onDismiss = { selected3DApp = null },
                        onAction = { action ->
                            selected3DApp = null
                            if (action == "Edit Home Screen") {
                                isJiggleMode = true
                            } else {
                                viewModel.openApp(app.id)
                            }
                        }
                    )
                }
            }
        }

        // Photo Widget Slideshow Picker Modal
        if (isPhotoWidgetPickerOpen) {
            AlertDialog(
                onDismissRequest = { isPhotoWidgetPickerOpen = false },
                title = {
                    Text(
                        text = "Pilih Foto Slideshow Widget",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.Black
                    )
                },
                text = {
                    Column {
                        Text(
                            text = "Centang foto yang ingin ditampilkan di widget secara bergiliran:",
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        LazyColumn(modifier = Modifier.height(280.dp)) {
                            items(photoItems) { photo ->
                                val isSelected = photo.id in selectedWidgetPhotoIds
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.togglePhotoInWidgetSelection(photo.id)
                                        }
                                        .padding(vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (photo.imageUri != null) {
                                        AsyncImage(
                                            model = photo.imageUri,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    } else if (photo.drawableRes != 0) {
                                        Image(
                                            painter = painterResource(id = photo.drawableRes),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = photo.title,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp,
                                            color = Color.Black,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = photo.date,
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = {
                                            viewModel.togglePhotoInWidgetSelection(photo.id)
                                        }
                                    )
                                }
                                HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { isPhotoWidgetPickerOpen = false }) {
                        Text("Selesai", color = IOSBlue, fontWeight = FontWeight.Bold)
                    }
                },
                containerColor = Color.White,
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconItem(
    app: AppItem,
    showLabel: Boolean = true,
    dayOfWeek: String = "MON",
    dayOfMonth: String = "1",
    hourFloat: Float = 9.7f,
    minuteFloat: Float = 41f,
    secondFloat: Float = 0f,
    isJiggleMode: Boolean = false,
    rotationAngle: Float = 0f,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(68.dp)
            .graphicsLayer {
                rotationZ = rotationAngle
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Enclosing Box sized 62.dp so the badge placed at TopEnd with offset is NEVER clipped!
        Box(
            modifier = Modifier.size(62.dp),
            contentAlignment = Alignment.Center
        ) {
            // Clipped Icon Shape
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .shadow(elevation = 4.dp, shape = RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(app.iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                when (app.id) {
                    AppId.CALENDAR -> {
                        // Calendar with dynamic day of week and day of month
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(16.dp)
                                    .background(Color(0xFFFF3B30)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayOfWeek,
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = dayOfMonth,
                                    color = Color.Black,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Normal
                                )
                            }
                        }
                    }
                    AppId.PHOTOS -> {
                        // iOS colorful flower petals
                        Box(modifier = Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val colors = listOf(
                                    Color(0xFFFF3B30), Color(0xFFFF9500), Color(0xFFFFCC00),
                                    Color(0xFF34C759), Color(0xFF007AFF), Color(0xFFAF52DE)
                                )
                                colors.forEachIndexed { i, c ->
                                    drawCircle(
                                        color = c.copy(alpha = 0.85f),
                                        radius = size.minDimension / 4.2f,
                                        center = androidx.compose.ui.geometry.Offset(
                                            size.width / 2 + (size.width / 5) * kotlin.math.cos(i * kotlin.math.PI.toFloat() / 3f),
                                            size.height / 2 + (size.height / 5) * kotlin.math.sin(i * kotlin.math.PI.toFloat() / 3f)
                                        )
                                    )
                                }
                            }
                        }
                    }
                    AppId.NOTES -> {
                        Column(modifier = Modifier.fillMaxSize()) {
                            Box(modifier = Modifier.fillMaxWidth().height(14.dp).background(Color(0xFFFFCC00)))
                            Box(modifier = Modifier.fillMaxWidth().weight(1f).background(Color.White).padding(6.dp)) {
                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    repeat(3) {
                                        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(Color(0xFFE5E5EA)))
                                    }
                                }
                            }
                        }
                    }
                    AppId.REMINDERS -> {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IOSBlue))
                                Box(modifier = Modifier.width(22.dp).height(2.dp).background(Color.LightGray))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IOSOrange))
                                Box(modifier = Modifier.width(22.dp).height(2.dp).background(Color.LightGray))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(IOSGreen))
                                Box(modifier = Modifier.width(22.dp).height(2.dp).background(Color.LightGray))
                            }
                        }
                    }
                    AppId.CLOCK -> {
                        // Black clock face with real-time ticking hands
                        Box(modifier = Modifier.size(42.dp).clip(CircleShape).background(Color.Black), contentAlignment = Alignment.Center) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(color = Color.White, radius = 2.dp.toPx())

                                val hourAngle = (hourFloat % 12f) * 30f - 90f
                                val hourRad = Math.toRadians(hourAngle.toDouble())
                                val hourLen = 9.dp.toPx()
                                drawLine(
                                    color = Color.White,
                                    start = center,
                                    end = androidx.compose.ui.geometry.Offset(
                                        (center.x + hourLen * kotlin.math.cos(hourRad)).toFloat(),
                                        (center.y + hourLen * kotlin.math.sin(hourRad)).toFloat()
                                    ),
                                    strokeWidth = 2.5f
                                )

                                val minAngle = (minuteFloat % 60f) * 6f - 90f
                                val minRad = Math.toRadians(minAngle.toDouble())
                                val minLen = 13.dp.toPx()
                                drawLine(
                                    color = Color.White,
                                    start = center,
                                    end = androidx.compose.ui.geometry.Offset(
                                        (center.x + minLen * kotlin.math.cos(minRad)).toFloat(),
                                        (center.y + minLen * kotlin.math.sin(minRad)).toFloat()
                                    ),
                                    strokeWidth = 2f
                                )

                                val secAngle = (secondFloat % 60f) * 6f - 90f
                                val secRad = Math.toRadians(secAngle.toDouble())
                                val secLen = 15.dp.toPx()
                                drawLine(
                                    color = Color(0xFFFF9500),
                                    start = center,
                                    end = androidx.compose.ui.geometry.Offset(
                                        (center.x + secLen * kotlin.math.cos(secRad)).toFloat(),
                                        (center.y + secLen * kotlin.math.sin(secRad)).toFloat()
                                    ),
                                    strokeWidth = 1.2f
                                )
                            }
                        }
                    }
                    AppId.SAFARI -> {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF32ADE6), Color(0xFF007AFF))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Explore,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    else -> {
                        if (app.iconVector != null) {
                            val iconTint = if (app.iconBgColor == Color.White) IOSBlue else Color.White
                            Icon(
                                imageVector = app.iconVector,
                                contentDescription = app.name,
                                tint = iconTint,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }

            // UNCLIPPED Notification Badge: Placed as sibling to clipped box!
            if (app.badgeCount > 0 && !isJiggleMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 6.dp, y = (-5).dp)
                        .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
                        .shadow(elevation = 4.dp, shape = CircleShape)
                        .background(Color(0xFFFF3B30), shape = CircleShape)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (app.badgeCount > 99) "99+" else app.badgeCount.toString(),
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Jiggle Mode Delete Button (-)
            if (isJiggleMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(x = (-4).dp, y = (-4).dp)
                        .size(20.dp)
                        .shadow(elevation = 3.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF8E8E93)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(10.dp)
                            .height(2.5.dp)
                            .background(Color.White)
                    )
                }
            }
        }

        if (showLabel) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = app.name,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun IOS3DTouchContextMenu(
    app: AppItem,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    val quickActions = remember(app.id) {
        when (app.id) {
            AppId.MESSAGES -> listOf(
                "New Message" to Icons.Rounded.Create,
                "Chat with Raka" to Icons.Rounded.Person,
                "Chat with Nadia" to Icons.Rounded.Person
            )
            AppId.CAMERA -> listOf(
                "Take Selfie" to Icons.Rounded.CameraFront,
                "Record Video" to Icons.Rounded.Videocam,
                "Take Portrait" to Icons.Rounded.Portrait
            )
            AppId.PHONE -> listOf(
                "View Recents" to Icons.Rounded.History,
                "Create New Contact" to Icons.Rounded.PersonAdd,
                "Call Voicemail" to Icons.Rounded.Voicemail
            )
            AppId.SETTINGS -> listOf(
                "Wi-Fi" to Icons.Rounded.Wifi,
                "Bluetooth" to Icons.Rounded.Bluetooth,
                "Battery" to Icons.Rounded.BatteryFull,
                "Cellular" to Icons.Rounded.CellTower
            )
            AppId.SAFARI -> listOf(
                "New Tab" to Icons.Rounded.Add,
                "New Private Tab" to Icons.Rounded.VisibilityOff,
                "Show Bookmarks" to Icons.Rounded.Bookmark
            )
            AppId.PHOTOS -> listOf(
                "Most Recent" to Icons.Rounded.Schedule,
                "Favorites" to Icons.Rounded.Favorite,
                "Search" to Icons.Rounded.Search
            )
            AppId.NOTES -> listOf(
                "New Note" to Icons.Rounded.EditNote,
                "New Checklist" to Icons.Rounded.Checklist
            )
            AppId.CLOCK -> listOf(
                "Create Alarm" to Icons.Rounded.AlarmAdd,
                "Start Stopwatch" to Icons.Rounded.Timer,
                "Start Timer" to Icons.Rounded.HourglassTop
            )
            AppId.MAPS -> listOf(
                "Mark My Location" to Icons.Rounded.MyLocation,
                "Search Nearby" to Icons.Rounded.NearMe
            )
            AppId.CALENDAR -> listOf(
                "Add Event" to Icons.Rounded.Event,
                "View Today" to Icons.Rounded.Today
            )
            else -> listOf(
                "Open ${app.name}" to Icons.Rounded.OpenInNew
            )
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .padding(24.dp)
            .clickable(interactionSource = null, indication = null) {} // Prevent dismiss when tapping dialog itself
    ) {
        // Highlighted App Icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .shadow(elevation = 12.dp, shape = RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(app.iconBgColor),
            contentAlignment = Alignment.Center
        ) {
            if (app.iconVector != null) {
                Icon(
                    imageVector = app.iconVector,
                    contentDescription = app.name,
                    tint = if (app.iconBgColor == Color.White) IOSBlue else Color.White,
                    modifier = Modifier.size(38.dp)
                )
            } else {
                Text(
                    text = app.name.take(2).uppercase(),
                    color = Color.Black,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Actions Card
        Box(
            modifier = Modifier
                .width(260.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(Color.White.copy(alpha = 0.94f))
                .padding(vertical = 6.dp)
        ) {
            Column {
                quickActions.forEachIndexed { index, pair ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction(pair.first) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = pair.first,
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = pair.second,
                            contentDescription = null,
                            tint = Color.Black.copy(alpha = 0.8f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (index < quickActions.size - 1) {
                        HorizontalDivider(
                            color = Color(0xFFE5E5EA),
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = Color(0xFFD1D1D6),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                // Edit Home Screen action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction("Edit Home Screen") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Home Screen",
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Rounded.DashboardCustomize,
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                HorizontalDivider(
                    color = Color(0xFFE5E5EA),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Share App action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction("Share App") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Share App",
                        color = Color.Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = null,
                        tint = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
