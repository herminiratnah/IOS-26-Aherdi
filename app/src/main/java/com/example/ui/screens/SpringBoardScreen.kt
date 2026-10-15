package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.*
import com.example.util.AppIconCache
import com.example.util.IOSFeedbackHelper
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.toBitmapSafe
import com.example.viewmodel.IOSViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SpringBoardScreen(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val currentWallpaperRes by viewModel.homeScreenWallpaperRes.collectAsState()
    val currentWallpaperUri by viewModel.homeScreenWallpaperUri.collectAsState()
    val currentDayOfWeek by viewModel.currentDayOfWeek.collectAsState()
    val currentDayOfMonth by viewModel.currentDayOfMonth.collectAsState()
    val currentHourFloat by viewModel.currentHourFloat.collectAsState()
    val currentMinuteFloat by viewModel.currentMinuteFloat.collectAsState()
    val currentSecondFloat by viewModel.currentSecondFloat.collectAsState()
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
    val weatherData by viewModel.weatherData.collectAsState()
    val activePhotoWidgetIndex by viewModel.activePhotoWidgetIndex.collectAsState()
    val selectedWidgetPhotoIds by viewModel.selectedWidgetPhotoIds.collectAsState()
    val leftWidgetType by viewModel.leftWidgetType.collectAsState()
    val rightWidgetType by viewModel.rightWidgetType.collectAsState()
    val widgetRowPosition by viewModel.widgetRowPosition.collectAsState()
    val widget1Type by viewModel.widget1Type.collectAsState()
    val widget1Size by viewModel.widget1Size.collectAsState()
    val widget1Row by viewModel.widget1Row.collectAsState()
    val widget1Align by viewModel.widget1Align.collectAsState()
    val widget1Enabled by viewModel.widget1Enabled.collectAsState()

    val widget2Type by viewModel.widget2Type.collectAsState()
    val widget2Size by viewModel.widget2Size.collectAsState()
    val widget2Row by viewModel.widget2Row.collectAsState()
    val widget2Align by viewModel.widget2Align.collectAsState()
    val widget2Enabled by viewModel.widget2Enabled.collectAsState()

    val widget3Type by viewModel.widget3Type.collectAsState()
    val widget3Size by viewModel.widget3Size.collectAsState()
    val widget3Row by viewModel.widget3Row.collectAsState()
    val widget3Align by viewModel.widget3Align.collectAsState()
    val widget3Enabled by viewModel.widget3Enabled.collectAsState()

    val widget4Type by viewModel.widget4Type.collectAsState()
    val widget4Size by viewModel.widget4Size.collectAsState()
    val widget4Row by viewModel.widget4Row.collectAsState()
    val widget4Align by viewModel.widget4Align.collectAsState()
    val widget4Enabled by viewModel.widget4Enabled.collectAsState()

    val widget5Type by viewModel.widget5Type.collectAsState()
    val widget5Size by viewModel.widget5Size.collectAsState()
    val widget5Row by viewModel.widget5Row.collectAsState()
    val widget5Align by viewModel.widget5Align.collectAsState()
    val widget5Enabled by viewModel.widget5Enabled.collectAsState()

    val widget6Type by viewModel.widget6Type.collectAsState()
    val widget6Size by viewModel.widget6Size.collectAsState()
    val widget6Row by viewModel.widget6Row.collectAsState()
    val widget6Align by viewModel.widget6Align.collectAsState()
    val widget6Enabled by viewModel.widget6Enabled.collectAsState()

    var showAddWidgetDialog by remember { mutableStateOf(false) }

    val isMusicPlaying by viewModel.isMusicPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    var activeWidgetDialogIndex by remember { mutableStateOf<Int?>(null) }
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val activeNotifications by viewModel.activeNotifications.collectAsState()
    var isPhotoWidgetPickerOpen by remember { mutableStateOf(false) }

    var draggedAppIndex by remember { mutableStateOf<Int?>(null) }
    var dragAppOffset by remember { mutableStateOf(Offset.Zero) }
    val slotBounds = remember { mutableStateMapOf<Int, Rect>() }
    var widgetDragOffset by remember { mutableStateOf(0f) }

    val customAppIcons by viewModel.customAppIcons.collectAsState()
    var targetAppForCustomIcon by remember { mutableStateOf<String?>(null) }
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && targetAppForCustomIcon != null) {
            viewModel.setCustomAppIcon(targetAppForCustomIcon!!, uri.toString(), context)
            targetAppForCustomIcon = null
        }
    }

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

    // 4-Page iOS Launcher (0 = Today View / Widgets, 1 = Home Page 1, 2 = Home Page 2, 3 = App Library)
    val pagerState = rememberPagerState(initialPage = 1, pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    // 3D Touch (Haptic Touch) & Jiggle Mode States
    var selected3DApp by remember { mutableStateOf<AppItem?>(null) }
    var isJiggleMode by remember { mutableStateOf(false) }
    var appToDelete by remember { mutableStateOf<AppItem?>(null) }
    var showAddAppSheet by remember { mutableStateOf(false) }

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

    val homeApps by viewModel.homeScreenApps.collectAsState()
    val rawDockApps by viewModel.dockApps.collectAsState()
    val installedAppsList by viewModel.installedApps.collectAsState()

    var showDockAppPicker by remember { mutableStateOf(false) }
    var dockAppToReplace by remember { mutableStateOf<AppItem?>(null) }

    // Dynamic Grid Apps matching homeScreenApps state
    val gridApps = remember(homeApps, unreadMessagesCount, missedCallsCount) {
        if (homeApps.isEmpty()) {
            listOf(
                // Row 1 (adjacent to 2x2 Calendar Widget): 4 apps
                AppItem(AppId.NOTES, "Notes", Color.White, null),
                AppItem(AppId.REMINDERS, "Reminder", Color.White, null),
                AppItem(AppId.CAMERA, "Camera", Color(0xFF636366), Icons.Rounded.CameraAlt),
                AppItem(AppId.MAIL, "Mail", Color(0xFF007AFF), Icons.Rounded.Email, badgeCount = 2),

                // Row 2: 4 apps
                AppItem(AppId.TV, "TV", Color.Black, Icons.Rounded.Tv),
                AppItem(AppId.PODCASTS, "Podcast", Color(0xFF8E44AD), Icons.Rounded.Podcasts),
                AppItem(AppId.APP_STORE, "App Store", Color(0xFF1D6EEB), Icons.Rounded.ShoppingBag),
                AppItem(AppId.MAPS, "Maps", Color(0xFF2ECC71), Icons.Rounded.Map),

                // Row 3: 4 apps
                AppItem(AppId.SETTINGS, "Settings", Color(0xFF8E8E93), Icons.Rounded.Settings, badgeCount = 1),
                AppItem(AppId.CUSTOM_APP, "Youtube", Color(0xFFFF0000), Icons.Rounded.PlayArrow, packageName = "com.google.android.youtube", isInstalledApp = true),
                AppItem(AppId.CUSTOM_APP, "Whatsapp", Color(0xFF25D366), Icons.Rounded.ChatBubble, packageName = "com.whatsapp", isInstalledApp = true),
                AppItem(AppId.DEPTH_STUDIO, "Depth Studio", Color(0xFF5856D6), Icons.Rounded.Layers),

                // Page 2 apps
                AppItem(AppId.FACETIME, "FaceTime", IOSGreen, Icons.Rounded.Videocam),
                AppItem(AppId.PHOTOS, "Photos", Color.White, null),
                AppItem(AppId.HEALTH, "Health", Color.White, Icons.Rounded.Favorite, badgeCount = 0),
                AppItem(AppId.WALLET, "Wallet", Color.Black, Icons.Rounded.AccountBalanceWallet),
                AppItem(AppId.CLOCK, "Clock", Color.Black, null),
                AppItem(AppId.ROOT_TOOLS, "iRoot SE", Color(0xFF5856D6), Icons.Rounded.Security)
            )
        } else {
            homeApps.map { item ->
                when (item.id) {
                    AppId.MAIL -> item.copy(badgeCount = 2)
                    AppId.SETTINGS -> item.copy(badgeCount = 1)
                    else -> item
                }
            }
        }
    }

    // Dock Apps from ViewModel (Customizable & Persistent)
    val dockApps = remember(rawDockApps, unreadMessagesCount, missedCallsCount) {
        rawDockApps.map { item ->
            when (item.id) {
                AppId.PHONE -> item.copy(badgeCount = missedCallsCount, isDock = true)
                AppId.MESSAGES -> item.copy(badgeCount = unreadMessagesCount, isDock = true)
                else -> item.copy(isDock = true)
            }
        }
    }

    val canSpringBoardHandleBack = isJiggleMode || selected3DApp != null ||
            showAddAppSheet || showDockAppPicker || isPhotoWidgetPickerOpen ||
            activeWidgetDialogIndex != null || pagerState.currentPage != 1

    androidx.activity.compose.BackHandler(enabled = canSpringBoardHandleBack) {
        when {
            activeWidgetDialogIndex != null -> activeWidgetDialogIndex = null
            selected3DApp != null -> selected3DApp = null
            showDockAppPicker -> {
                showDockAppPicker = false
                dockAppToReplace = null
            }
            isPhotoWidgetPickerOpen -> isPhotoWidgetPickerOpen = false
            showAddAppSheet -> showAddAppSheet = false
            isJiggleMode -> isJiggleMode = false
            pagerState.currentPage != 1 -> coroutineScope.launch { pagerState.animateScrollToPage(1) }
        }
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
                painter = com.example.ui.components.safePainterResource(id = if (currentWallpaperRes > 0) currentWallpaperRes else R.drawable.wallpaper_alpine, fallbackId = R.drawable.wallpaper_alpine),
                contentDescription = "iOS Wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 4.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Spacer for Single Global Status Bar
            Spacer(modifier = Modifier.height(44.dp))

            // Top Bar with "+" button and "Done" button during Jiggle Mode
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isJiggleMode) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                            .clickable {
                                IOSFeedbackHelper.vibrateLight(context)
                                showAddWidgetDialog = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add App",
                            tint = Color.Black,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.size(32.dp))
                }

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

            // Liquid Glass Dynamic Draggable Widgets & Apps Setup
            val allActiveWidgets = remember(
                widget1Enabled, widget1Row, widget1Type, widget1Size, widget1Align,
                widget2Enabled, widget2Row, widget2Type, widget2Size, widget2Align,
                widget3Enabled, widget3Row, widget3Type, widget3Size, widget3Align,
                widget4Enabled, widget4Row, widget4Type, widget4Size, widget4Align,
                widget5Enabled, widget5Row, widget5Type, widget5Size, widget5Align,
                widget6Enabled, widget6Row, widget6Type, widget6Size, widget6Align
            ) {
                val list = mutableListOf<WidgetSlotData>()
                if (widget1Enabled) list.add(WidgetSlotData(1, widget1Type, widget1Size, widget1Row, widget1Align, true))
                if (widget2Enabled) list.add(WidgetSlotData(2, widget2Type, widget2Size, widget2Row, widget2Align, true))
                if (widget3Enabled) list.add(WidgetSlotData(3, widget3Type, widget3Size, widget3Row, widget3Align, true))
                if (widget4Enabled) list.add(WidgetSlotData(4, widget4Type, widget4Size, widget4Row, widget4Align, true))
                if (widget5Enabled) list.add(WidgetSlotData(5, widget5Type, widget5Size, widget5Row, widget5Align, true))
                if (widget6Enabled) list.add(WidgetSlotData(6, widget6Type, widget6Size, widget6Row, widget6Align, true))
                list
            }

            val homeRowLayouts = remember(allActiveWidgets, gridApps) {
                val result = mutableListOf<HomeRowLayout>()
                var appOffset = 0
                val maxTargetRow = maxOf(5, (allActiveWidgets.maxOfOrNull { it.row } ?: 0))

                for (r in 0..maxTargetRow) {
                    val rWidgets = allActiveWidgets.filter { it.row == r }
                    val needed = when {
                        rWidgets.isEmpty() -> 4
                        rWidgets.size >= 2 -> 0
                        rWidgets[0].size == "SMALL" -> 4 // 4 apps sit in the 2x2 grid next to the 2x2 widget!
                        rWidgets[0].size == "COMPACT" -> 2 // 2 apps sit next to the 2x1 widget!
                        else -> 0 // MEDIUM (4x2) or LARGE (4x4) fills full row
                    }
                    val start = appOffset
                    val end = minOf(start + needed, gridApps.size)
                    val appsForThisRow = if (start < gridApps.size) gridApps.subList(start, end) else emptyList()
                    result.add(HomeRowLayout(r, rWidgets, appsForThisRow, start))
                    appOffset = end
                }

                // Any remaining apps beyond maxTargetRow go into additional rows
                while (appOffset < gridApps.size) {
                    val nextRow = result.size
                    val start = appOffset
                    val end = minOf(start + 4, gridApps.size)
                    val appsForThisRow = gridApps.subList(start, end)
                    result.add(HomeRowLayout(nextRow, emptyList(), appsForThisRow, start))
                    appOffset = end
                }
                result
            }

                        // Helper lambda for rendering each widget slot
                        @Composable
                        fun RenderIndividualWidget(
                            widgetIndex: Int,
                            widgetType: String,
                            widgetSize: String,
                            currentRow: Int,
                            modifier: Modifier = Modifier
                        ) {
                            val rotation = if (isJiggleMode) {
                                when (widgetIndex) {
                                    1 -> jiggleRotation * 0.4f
                                    2 -> -jiggleRotation * 0.4f
                                    else -> jiggleRotation * 0.35f
                                }
                            } else 0f

                            var singleDragOffsetY by remember { mutableFloatStateOf(0f) }
                            val heightDp = when (widgetSize) {
                                "COMPACT" -> 80.dp
                                "LARGE" -> 300.dp
                                else -> 148.dp
                            }

                            val widgetTitle = when (widgetType) {
                                "WHATSAPP" -> "WhatsApp"
                                "WEATHER" -> "Cuaca"
                                "PHOTOS" -> "Foto"
                                "CALENDAR" -> "Kalender"
                                "BATTERY" -> "Baterai"
                                "CLOCK" -> "Jam Dunia"
                                "MUSIC" -> "Musik"
                                "FITNESS" -> "Aktivitas"
                                "NOTES" -> "Catatan"
                                else -> widgetType
                            }

                            val isSmall = (widgetSize == "SMALL")
                            val effectiveModifier = if (isSmall && (modifier == Modifier || modifier == Modifier.fillMaxWidth())) {
                                Modifier.width(160.dp)
                            } else {
                                modifier
                            }

                            Column(
                                modifier = effectiveModifier
                                    .graphicsLayer { rotationZ = rotation }
                                    .pointerInput(widgetIndex, isJiggleMode) {
                                        detectDragGestures(
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                singleDragOffsetY += dragAmount.y
                                            },
                                            onDragEnd = {
                                                if (singleDragOffsetY > 45f) {
                                                    viewModel.moveSingleWidgetDown(widgetIndex, context)
                                                } else if (singleDragOffsetY < -45f) {
                                                    viewModel.moveSingleWidgetUp(widgetIndex, context)
                                                }
                                                singleDragOffsetY = 0f
                                            },
                                            onDragCancel = { singleDragOffsetY = 0f }
                                        )
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ResizableWidgetContainer(
                                    isJiggleMode = isJiggleMode,
                                    currentSize = widgetSize,
                                    widgetTitle = widgetTitle,
                                    onDelete = { viewModel.removeWidget(widgetIndex, context) },
                                    onCycleSize = { viewModel.toggleWidgetSize(widgetIndex, context) },
                                    onTapWidget = { activeWidgetDialogIndex = widgetIndex },
                                    onToggleAlign = { viewModel.toggleWidgetAlignment(widgetIndex, context) },
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(
                                        modifier = (if (isSmall) Modifier.size(160.dp, heightDp) else Modifier.fillMaxWidth().height(heightDp))
                                            .pointerInput(widgetIndex) {
                                                detectTapGestures(
                                                    onLongPress = {
                                                        IOSFeedbackHelper.vibrateMedium(context)
                                                        isJiggleMode = true
                                                    },
                                                    onTap = {
                                                        if (isJiggleMode) {
                                                            activeWidgetDialogIndex = widgetIndex
                                                        } else {
                                                            when (widgetType) {
                                                                "WHATSAPP" -> {
                                                                    val pm = context.packageManager
                                                                    val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
                                                                        ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")
                                                                    if (launchIntent != null) {
                                                                        context.startActivity(launchIntent)
                                                                    } else {
                                                                        try {
                                                                            context.startActivity(
                                                                                Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp"))
                                                                            )
                                                                        } catch (_: Exception) {}
                                                                    }
                                                                }
                                                                "WEATHER" -> viewModel.openApp(AppId.WEATHER)
                                                                "PHOTOS" -> viewModel.openApp(AppId.PHOTOS)
                                                                "CALENDAR" -> viewModel.openApp(AppId.CALENDAR)
                                                                "BATTERY" -> viewModel.openApp(AppId.SETTINGS)
                                                                "CLOCK" -> viewModel.openApp(AppId.CLOCK)
                                                                "MUSIC" -> viewModel.openApp(AppId.MUSIC)
                                                                "FITNESS" -> viewModel.openApp(AppId.HEALTH)
                                                                "NOTES" -> viewModel.openApp(AppId.NOTES)
                                                                else -> {}
                                                            }
                                                        }
                                                    }
                                                )
                                            }
                                    ) {
                                        when (widgetType) {
                                            "WHATSAPP" -> {
                                                LiquidGlassWhatsAppWidget(
                                                    isJiggleMode = isJiggleMode,
                                                    size = widgetSize,
                                                    notifications = activeNotifications,
                                                    unreadCount = activeNotifications.count { it.packageName.contains("whatsapp", ignoreCase = true) },
                                                    onClick = {
                                                        if (isJiggleMode) activeWidgetDialogIndex = widgetIndex
                                                        else {
                                                            val pm = context.packageManager
                                                            val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
                                                                ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")
                                                            if (launchIntent != null) {
                                                                context.startActivity(launchIntent)
                                                            } else {
                                                                try {
                                                                    context.startActivity(
                                                                        Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.whatsapp"))
                                                                    )
                                                                } catch (_: Exception) {}
                                                            }
                                                        }
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            "WEATHER" -> {
                                                LiquidGlassWeatherWidget(
                                                    weatherData = weatherData,
                                                    isJiggleMode = isJiggleMode,
                                                    onCycleWidget = { viewModel.cycleWidget(widgetIndex == 1, context) },
                                                    onClick = {
                                                        if (isJiggleMode) activeWidgetDialogIndex = widgetIndex
                                                        else viewModel.openApp(AppId.WEATHER)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            "PHOTOS" -> {
                                                LiquidGlassPhotosWidget(
                                                    currentPhoto = currentWidgetPhoto,
                                                    isJiggleMode = isJiggleMode,
                                                    onNextPhoto = { viewModel.nextWidgetPhoto() },
                                                    onPickPhotos = { isPhotoWidgetPickerOpen = true },
                                                    onCycleWidget = { viewModel.cycleWidget(widgetIndex == 1, context) },
                                                    onClick = {
                                                        if (isJiggleMode) activeWidgetDialogIndex = widgetIndex
                                                        else viewModel.openApp(AppId.PHOTOS)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            "CALENDAR" -> {
                                                LiquidGlassCalendarWidget(
                                                    dayOfWeek = currentDayOfWeek,
                                                    dayOfMonth = currentDayOfMonth,
                                                    isJiggleMode = isJiggleMode,
                                                    onCycleWidget = { viewModel.cycleWidget(widgetIndex == 1, context) },
                                                    onClick = {
                                                        if (isJiggleMode) activeWidgetDialogIndex = widgetIndex
                                                        else viewModel.openApp(AppId.CALENDAR)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            "BATTERY" -> {
                                                LiquidGlassBatteryWidget(
                                                    batteryLevel = batteryLevel,
                                                    isCharging = isBatteryCharging,
                                                    isJiggleMode = isJiggleMode,
                                                    onCycleWidget = { viewModel.cycleWidget(widgetIndex == 1, context) },
                                                    onClick = {
                                                        if (isJiggleMode) activeWidgetDialogIndex = widgetIndex
                                                        else viewModel.openApp(AppId.SETTINGS)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            "CLOCK" -> {
                                                LiquidGlassClockWidget(
                                                    hour = currentHourFloat,
                                                    minute = currentMinuteFloat,
                                                    second = currentSecondFloat,
                                                    isMedium = (widgetSize == "MEDIUM"),
                                                    onClick = {
                                                        if (isJiggleMode) activeWidgetDialogIndex = widgetIndex
                                                        else viewModel.openApp(AppId.CLOCK)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            "MUSIC" -> {
                                                LiquidGlassMusicWidget(
                                                    songTitle = currentSong?.title ?: "Apple Music",
                                                    artistName = currentSong?.artist ?: "Liquid Sound",
                                                    isPlaying = isMusicPlaying,
                                                    isMedium = (widgetSize == "MEDIUM"),
                                                    onTogglePlay = { viewModel.toggleMusicPlay() },
                                                    onClick = {
                                                        if (isJiggleMode) activeWidgetDialogIndex = widgetIndex
                                                        else viewModel.openApp(AppId.MUSIC)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            "FITNESS" -> {
                                                LiquidGlassFitnessWidget(
                                                    isMedium = (widgetSize == "MEDIUM"),
                                                    onClick = {
                                                        if (isJiggleMode) activeWidgetDialogIndex = widgetIndex
                                                        else viewModel.openApp(AppId.HEALTH)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            "NOTES" -> {
                                                LiquidGlassNotesWidget(
                                                    isMedium = (widgetSize == "MEDIUM"),
                                                    onClick = {
                                                        if (isJiggleMode) activeWidgetDialogIndex = widgetIndex
                                                        else viewModel.openApp(AppId.NOTES)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            else -> {
                                                LiquidGlassWeatherWidget(
                                                    weatherData = weatherData,
                                                    isJiggleMode = isJiggleMode,
                                                    onCycleWidget = { viewModel.cycleWidget(widgetIndex == 1, context) },
                                                    onClick = {
                                                        if (isJiggleMode) activeWidgetDialogIndex = widgetIndex
                                                        else viewModel.openApp(AppId.WEATHER)
                                                    },
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        @Composable
                        fun RenderAppSlot(
                            flatIndex: Int,
                            app: AppItem,
                            modifier: Modifier = Modifier
                        ) {
                            val isBeingDragged = (draggedAppIndex == flatIndex)
                            val rotationAngle = if (isJiggleMode && !isBeingDragged) {
                                if (flatIndex % 2 == 0) jiggleRotation else -jiggleRotation
                            } else 0f

                            val appKey = if (app.isInstalledApp && !app.packageName.isNullOrBlank()) app.packageName else app.id.name

                            Box(
                                modifier = modifier
                                    .width(64.dp)
                                    .onGloballyPositioned { coords ->
                                        slotBounds[flatIndex] = coords.boundsInRoot()
                                    }
                                    .zIndex(if (isBeingDragged) 10f else 1f)
                                    .offset {
                                        if (isBeingDragged) {
                                            IntOffset(dragAppOffset.x.roundToInt(), dragAppOffset.y.roundToInt())
                                        } else {
                                            IntOffset.Zero
                                        }
                                    }
                                    .scale(if (isBeingDragged) 1.15f else 1f)
                                    .pointerInput(app.id, flatIndex) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                isJiggleMode = true
                                                draggedAppIndex = flatIndex
                                                dragAppOffset = Offset.Zero
                                                IOSFeedbackHelper.vibrateLight(context)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragAppOffset += dragAmount
                                            },
                                            onDragEnd = {
                                                val fromIdx = draggedAppIndex
                                                if (fromIdx != null) {
                                                    val currentCenter = (slotBounds[fromIdx]?.center ?: Offset.Zero) + dragAppOffset
                                                    val targetEntry = slotBounds.entries.minByOrNull { (_, rect) ->
                                                        (rect.center - currentCenter).getDistance()
                                                    }
                                                    if (targetEntry != null && targetEntry.key != fromIdx && targetEntry.key in gridApps.indices) {
                                                        viewModel.reorderHomeScreenApps(fromIdx, targetEntry.key, context)
                                                    }
                                                }
                                                draggedAppIndex = null
                                                dragAppOffset = Offset.Zero
                                            },
                                            onDragCancel = {
                                                draggedAppIndex = null
                                                dragAppOffset = Offset.Zero
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AppIconItem(
                                    app = app,
                                    customIconUri = customAppIcons[appKey],
                                    dayOfWeek = currentDayOfWeek,
                                    dayOfMonth = currentDayOfMonth,
                                    hourFloat = currentHourFloat,
                                    minuteFloat = currentMinuteFloat,
                                    secondFloat = currentSecondFloat,
                                    isJiggleMode = isJiggleMode,
                                    rotationAngle = rotationAngle,
                                    onMoveLeft = if (isJiggleMode && flatIndex > 0) {
                                        { viewModel.moveHomeScreenApp(app, -1, context) }
                                    } else null,
                                    onMoveRight = if (isJiggleMode && flatIndex < gridApps.size - 1) {
                                        { viewModel.moveHomeScreenApp(app, 1, context) }
                                    } else null,
                                    onClick = {
                                        if (isJiggleMode) {
                                            isJiggleMode = false
                                        } else {
                                            if (app.isInstalledApp && !app.packageName.isNullOrBlank()) {
                                                viewModel.launchInstalledApp(context, app.packageName)
                                            } else {
                                                viewModel.openApp(app.id)
                                            }
                                        }
                                    },
                                    onLongClick = {
                                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                        selected3DApp = app
                                    },
                                    onClickDelete = {
                                        appToDelete = app
                                    }
                                )
                            }
                        }

                        @Composable
                        fun RenderHomeRowLayout(layout: HomeRowLayout) {
                            when {
                                layout.widgets.isEmpty() -> {
                                    // Standard 4-app row (matching iOS proportions: 64dp slots with 16dp outer margin)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        for (i in 0 until 4) {
                                            if (i < layout.apps.size) {
                                                val app = layout.apps[i]
                                                val flatIndex = layout.appStartIndex + i
                                                RenderAppSlot(flatIndex, app)
                                            } else {
                                                Spacer(modifier = Modifier.width(64.dp))
                                            }
                                        }
                                    }
                                }
                                layout.widgets.size == 1 && layout.widgets[0].size == "SMALL" -> {
                                    // 2x2 Widget with space for 4 apps (2x2 grid) alongside it!
                                    val widgetData = layout.widgets[0]
                                    val widgetView = @Composable {
                                        RenderIndividualWidget(
                                            widgetIndex = widgetData.id,
                                            widgetType = widgetData.type,
                                            widgetSize = widgetData.size,
                                            currentRow = layout.rowIndex,
                                            modifier = Modifier.fillMaxWidth().height(156.dp)
                                        )
                                    }
                                    val appsView = @Composable {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(156.dp),
                                            verticalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            // Sub-row 0: first 2 apps
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceAround,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                for (i in 0 until 2) {
                                                    if (i < layout.apps.size) {
                                                        val app = layout.apps[i]
                                                        val flatIndex = layout.appStartIndex + i
                                                        RenderAppSlot(flatIndex, app)
                                                    } else {
                                                        Spacer(modifier = Modifier.width(64.dp))
                                                    }
                                                }
                                            }
                                            // Sub-row 1: next 2 apps
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceAround,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                for (i in 2 until 4) {
                                                    if (i < layout.apps.size) {
                                                        val app = layout.apps[i]
                                                        val flatIndex = layout.appStartIndex + i
                                                        RenderAppSlot(flatIndex, app)
                                                    } else {
                                                        Spacer(modifier = Modifier.width(64.dp))
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (widgetData.align == "RIGHT") {
                                            Box(modifier = Modifier.weight(1f)) { appsView() }
                                            Box(modifier = Modifier.weight(1f)) { widgetView() }
                                        } else {
                                            Box(modifier = Modifier.weight(1f)) { widgetView() }
                                            Box(modifier = Modifier.weight(1f)) { appsView() }
                                        }
                                    }
                                }
                                layout.widgets.size == 1 && layout.widgets[0].size == "COMPACT" -> {
                                    // 2x1 Widget with space for 2 apps alongside it!
                                    val widgetData = layout.widgets[0]
                                    val widgetView = @Composable {
                                        RenderIndividualWidget(
                                            widgetIndex = widgetData.id,
                                            widgetType = widgetData.type,
                                            widgetSize = widgetData.size,
                                            currentRow = layout.rowIndex,
                                            modifier = Modifier.fillMaxWidth().height(80.dp)
                                        )
                                    }
                                    val appsView = @Composable {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().height(80.dp),
                                            horizontalArrangement = Arrangement.SpaceAround,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            for (i in 0 until 2) {
                                                if (i < layout.apps.size) {
                                                    val app = layout.apps[i]
                                                    val flatIndex = layout.appStartIndex + i
                                                    RenderAppSlot(flatIndex, app)
                                                } else {
                                                    Spacer(modifier = Modifier.width(64.dp))
                                                }
                                            }
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (widgetData.align == "RIGHT") {
                                            Box(modifier = Modifier.weight(1f)) { appsView() }
                                            Box(modifier = Modifier.weight(1f)) { widgetView() }
                                        } else {
                                            Box(modifier = Modifier.weight(1f)) { widgetView() }
                                            Box(modifier = Modifier.weight(1f)) { appsView() }
                                        }
                                    }
                                }
                                layout.widgets.size >= 2 && layout.widgets[0].size == "SMALL" && layout.widgets[1].size == "SMALL" -> {
                                    // Two 2x2 Widgets side-by-side
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RenderIndividualWidget(
                                            widgetIndex = layout.widgets[0].id,
                                            widgetType = layout.widgets[0].type,
                                            widgetSize = layout.widgets[0].size,
                                            currentRow = layout.rowIndex,
                                            modifier = Modifier.weight(1f).height(156.dp)
                                        )
                                        RenderIndividualWidget(
                                            widgetIndex = layout.widgets[1].id,
                                            widgetType = layout.widgets[1].type,
                                            widgetSize = layout.widgets[1].size,
                                            currentRow = layout.rowIndex,
                                            modifier = Modifier.weight(1f).height(156.dp)
                                        )
                                    }
                                }
                                else -> {
                                    // Full row width widgets (MEDIUM 4x2 or LARGE 4x4)
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        layout.widgets.forEach { w ->
                                            val h = if (w.size == "LARGE") 320.dp else 156.dp
                                            RenderIndividualWidget(
                                                widgetIndex = w.id,
                                                widgetType = w.type,
                                                widgetSize = w.size,
                                                currentRow = layout.rowIndex,
                                                modifier = Modifier.fillMaxWidth().height(h)
                                            )
                                        }
                                    }
                                }
                            }
                        }

            // 4-Page iOS Launcher (0 = Today View / Widgets, 1 = Home Page 1, 2 = Home Page 2, 3 = App Library)
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
                        // Page 1: Main Home Screen (Liquid Glass Dynamic Draggable Widgets & Apps)
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val page1Layouts = homeRowLayouts.filter { it.rowIndex in 0..2 }
                            page1Layouts.forEach { rowLayout ->
                                RenderHomeRowLayout(rowLayout)
                            }
                        }
                    }
                    2 -> {
                        // Page 2: Home Screen Page 2 (Additional Widgets & Apps)
                        Column(
                            modifier = Modifier
                                .fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val page2Layouts = homeRowLayouts.filter { it.rowIndex >= 3 }
                            page2Layouts.forEach { rowLayout ->
                                RenderHomeRowLayout(rowLayout)
                            }
                        }
                    }
                    3 -> {
                        // Page 3: App Library
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
                // Page 0 dot (Today View)
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

                // Page 3 dot (App Library)
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == 3) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = if (pagerState.currentPage == 3) 1f else 0.45f))
                        .clickable {
                            coroutineScope.launch { pagerState.animateScrollToPage(3) }
                        }
                )
            }

            // Persistent Bottom Translucent Dock (Visible on Page 0, 1, 2)
            if (pagerState.currentPage != 3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0.18f))
                            )
                        )
                        .border(
                            androidx.compose.foundation.BorderStroke(1.2.dp, LiquidGlassBorderBrush),
                            shape = RoundedCornerShape(32.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 8.dp)
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

                            val appKey = if (app.isInstalledApp && !app.packageName.isNullOrBlank()) app.packageName else app.id.name
                            AppIconItem(
                                app = app,
                                customIconUri = customAppIcons[appKey],
                                showLabel = false,
                                isJiggleMode = isJiggleMode,
                                rotationAngle = rotationAngle,
                                onClick = {
                                    if (isJiggleMode) {
                                        dockAppToReplace = app
                                        showDockAppPicker = true
                                    } else {
                                        if (app.isInstalledApp && !app.packageName.isNullOrBlank()) {
                                            viewModel.launchInstalledApp(context, app.packageName)
                                        } else {
                                            viewModel.openApp(app.id)
                                        }
                                    }
                                },
                                onLongClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    selected3DApp = app
                                },
                                onClickDelete = if (isJiggleMode) {
                                    {
                                        viewModel.removeDockApp(app, context)
                                    }
                                } else null
                            )
                        }

                        // If dock has fewer than 4 apps, show "+" button to add app
                        if (dockApps.size < 4) {
                            Box(
                                modifier = Modifier
                                    .size(58.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Color(0x30FFFFFF))
                                    .clickable {
                                        dockAppToReplace = null
                                        showDockAppPicker = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Tambah App ke Dock",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(26.dp)
                                )
                            }
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
                onOpenAppSwitcher = {
                    viewModel.openAppSwitcher()
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
                    val appKey = if (app.isInstalledApp && !app.packageName.isNullOrBlank()) app.packageName else app.id.name
                    IOS3DTouchContextMenu(
                        app = app,
                        customIconUri = customAppIcons[appKey],
                        onDismiss = { selected3DApp = null },
                        onAction = { action ->
                            selected3DApp = null
                            if (action == "Edit Home Screen") {
                                isJiggleMode = true
                            } else if (action == "Move Left") {
                                viewModel.moveHomeScreenApp(app, -1, context)
                            } else if (action == "Move Right") {
                                viewModel.moveHomeScreenApp(app, 1, context)
                            } else if (action == "Move Up") {
                                viewModel.moveHomeScreenApp(app, -4, context)
                            } else if (action == "Move Down") {
                                viewModel.moveHomeScreenApp(app, 4, context)
                            } else if (action == "Share App") {
                                viewModel.openShareSheet("Aplikasi: ${app.name}")
                            } else if (action == "Remove App") {
                                appToDelete = app
                            } else if (action == "Replace Dock App") {
                                dockAppToReplace = app
                                showDockAppPicker = true
                            } else if (action == "Remove From Dock") {
                                viewModel.removeDockApp(app, context)
                            } else if (action == "Add To Dock") {
                                viewModel.addDockApp(app, context)
                            } else {
                                if (app.isInstalledApp && !app.packageName.isNullOrBlank()) {
                                    viewModel.launchInstalledApp(context, app.packageName)
                                } else {
                                    viewModel.openApp(app.id)
                                }
                            }
                        },
                        onPickCustomIcon = {
                            targetAppForCustomIcon = appKey
                            selected3DApp = null
                            iconPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        onResetCustomIcon = if (customAppIcons.containsKey(appKey)) {
                            {
                                viewModel.resetCustomAppIcon(appKey, context)
                                selected3DApp = null
                            }
                        } else null
                    )
                }
            }
        }

        // Authentic iOS Remove App Alert Dialog
        appToDelete?.let { app ->
            AlertDialog(
                onDismissRequest = { appToDelete = null },
                containerColor = Color(0xFA2C2C2E),
                shape = RoundedCornerShape(18.dp),
                title = {
                    Text(
                        text = "Hapus \"${app.name}\"?",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = "Menghapus dari Layar Utama akan tetap menyimpan app di Perpustakaan App Anda.",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (app.isInstalledApp && !app.packageName.isNullOrBlank()) {
                            HorizontalDivider(color = Color(0x33FFFFFF), thickness = 0.5.dp)
                            TextButton(
                                onClick = {
                                    val target = app
                                    appToDelete = null
                                    viewModel.removeAppFromHomeScreen(target, context)
                                    try {
                                        val intent = Intent(Intent.ACTION_DELETE).apply {
                                            data = Uri.parse("package:${target.packageName}")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (ignored: Exception) {}
                                    IOSFeedbackHelper.vibrateLight(context)
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "Hapus App (Copot Pemasangan)",
                                    color = Color(0xFFFF453A),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        HorizontalDivider(color = Color(0x33FFFFFF), thickness = 0.5.dp)
                        TextButton(
                            onClick = {
                                val target = app
                                appToDelete = null
                                viewModel.removeAppFromHomeScreen(target, context)
                                IOSFeedbackHelper.vibrateLight(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Hapus dari Layar Utama",
                                color = Color(0xFFFF9500),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        HorizontalDivider(color = Color(0x33FFFFFF), thickness = 0.5.dp)
                        TextButton(
                            onClick = { appToDelete = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Batal",
                                color = IOSBlue,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            )
        }

        // Add Apps to Home Screen Bottom Dialog
        if (showAddAppSheet) {
            var appSearch by remember { mutableStateOf("") }
            val filteredInstalled = installedAppsList.filter {
                it.appName.contains(appSearch, ignoreCase = true)
            }

            AlertDialog(
                onDismissRequest = { showAddAppSheet = false },
                containerColor = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(22.dp),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tambah ke Layar Utama",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = { showAddAppSheet = false },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                        // Search Field
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2C2C2E))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                BasicTextField(
                                    value = appSearch,
                                    onValueChange = { appSearch = it },
                                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                    cursorBrush = SolidColor(IOSBlue),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { inner ->
                                        if (appSearch.isEmpty()) {
                                            Text("Cari aplikasi...", color = Color.Gray, fontSize = 14.sp)
                                        }
                                        inner()
                                    }
                                )
                            }
                        }
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.horizontalGradient(listOf(Color(0xFF25D366).copy(alpha = 0.25f), Color(0xFF007AFF).copy(alpha = 0.25f))))
                                    .border(BorderStroke(1.dp, Color(0xFF25D366).copy(alpha = 0.5f)), RoundedCornerShape(14.dp))
                                    .clickable {
                                        showAddAppSheet = false
                                        showAddWidgetDialog = true
                                    }
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF25D366)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Widgets, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Tambah Widget Baru", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        Text("WhatsApp, Cuaca, Jam, Foto, Baterai, Musik", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                                    }
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
                                }
                            }
                        }

                        if (filteredInstalled.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Tidak ada aplikasi ditemukan", color = Color.Gray, fontSize = 14.sp)
                                }
                            }
                        } else {
                            items(filteredInstalled) { installed ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFF2C2C2E))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color(installed.iconColor)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (installed.iconBitmap != null) {
                                                Image(
                                                    bitmap = installed.iconBitmap.asImageBitmap(),
                                                    contentDescription = installed.appName,
                                                    modifier = Modifier.fillMaxSize(),
                                                    contentScale = ContentScale.Crop
                                                )
                                            } else {
                                                Icon(
                                                    Icons.Rounded.Apps,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                        Column {
                                            Text(
                                                text = installed.appName,
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = installed.category,
                                                color = Color.Gray,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            IOSFeedbackHelper.vibrateLight(context)
                                            viewModel.addInstalledAppToHomeScreen(installed, context)
                                            showAddAppSheet = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Tambah", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddAppSheet = false }) {
                        Text("Selesai", color = IOSBlue, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            )
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
                                    } else if (photo.drawableRes > 0) {
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

        // Liquid Glass Widget Gallery & Size Dialog
        if (activeWidgetDialogIndex != null) {
            val wIdx = activeWidgetDialogIndex!!
            LiquidGlassWidgetGalleryDialog(
                widgetIndex = wIdx,
                currentType = when (wIdx) {
                    1 -> widget1Type
                    2 -> widget2Type
                    3 -> widget3Type
                    4 -> widget4Type
                    5 -> widget5Type
                    else -> widget6Type
                },
                currentSize = when (wIdx) {
                    1 -> widget1Size
                    2 -> widget2Size
                    3 -> widget3Size
                    4 -> widget4Size
                    5 -> widget5Size
                    else -> widget6Size
                },
                onSelectType = { newType ->
                    viewModel.setWidgetType(wIdx, newType, context)
                },
                onSelectSize = { newSize ->
                    viewModel.setWidgetSize(wIdx, newSize, context)
                },
                onDismiss = { activeWidgetDialogIndex = null }
            )
        }

        // Add Widget Modal Dialog (Opened when tapping '+' in Jiggle Mode)
        if (showAddWidgetDialog) {
            LiquidGlassAddWidgetDialog(
                onAddWidget = { newType, newSize ->
                    val preferredRow = if (pagerState.currentPage == 2) 3 else 0
                    viewModel.addWidget(newType, newSize, preferredRow, context)
                },
                onDismiss = { showAddWidgetDialog = false }
            )
        }

        // Dock App Picker Modal Dialog
        if (showDockAppPicker) {
            var searchDockApp by remember { mutableStateOf("") }
            val availableApps = remember(installedAppsList, searchDockApp) {
                if (searchDockApp.isBlank()) installedAppsList
                else installedAppsList.filter { it.appName.contains(searchDockApp, ignoreCase = true) }
            }

            AlertDialog(
                onDismissRequest = {
                    showDockAppPicker = false
                    dockAppToReplace = null
                },
                containerColor = Color(0xFF1C1C1E),
                shape = RoundedCornerShape(22.dp),
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (dockAppToReplace != null) "Ganti Aplikasi Dock" else "Pilih Aplikasi Dock",
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(
                                onClick = {
                                    showDockAppPicker = false
                                    dockAppToReplace = null
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(18.dp))
                            }
                        }
                        // Search Field
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF2C2C2E))
                                .padding(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                                BasicTextField(
                                    value = searchDockApp,
                                    onValueChange = { searchDockApp = it },
                                    textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                                    cursorBrush = SolidColor(IOSBlue),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { inner ->
                                        if (searchDockApp.isEmpty()) {
                                            Text("Cari aplikasi...", color = Color.Gray, fontSize = 14.sp)
                                        }
                                        inner()
                                    }
                                )
                            }
                        }
                    }
                },
                text = {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 350.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(availableApps) { app ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2C2C2E))
                                    .clickable {
                                        val newAppItem = if (app.isBuiltIn && app.builtInId != null) {
                                            val defaultGrid = listOf(
                                                AppItem(AppId.PHONE, "Phone", IOSGreen, Icons.Rounded.Call, isDock = true),
                                                AppItem(AppId.SAFARI, "Safari", Color.White, Icons.Rounded.Explore, isDock = true),
                                                AppItem(AppId.MESSAGES, "Messages", IOSGreen, Icons.Rounded.ChatBubble, isDock = true),
                                                AppItem(AppId.MUSIC, "Music", Color(0xFFFF2D55), Icons.Rounded.MusicNote, isDock = true),
                                                AppItem(AppId.FACETIME, "FaceTime", IOSGreen, Icons.Rounded.Videocam, isDock = true),
                                                AppItem(AppId.CAMERA, "Camera", Color(0xFF636366), Icons.Rounded.CameraAlt, isDock = true),
                                                AppItem(AppId.MAIL, "Mail", Color(0xFF007AFF), Icons.Rounded.Email, isDock = true),
                                                AppItem(AppId.SETTINGS, "Settings", Color(0xFF8E8E93), Icons.Rounded.Settings, isDock = true)
                                            )
                                            defaultGrid.firstOrNull { it.id == app.builtInId }
                                                ?: AppItem(id = app.builtInId, name = app.appName, iconBgColor = Color(app.iconColor), isDock = true)
                                        } else {
                                            val bmp = app.iconBitmap ?: AppIconCache.getIcon(context, app.packageName)
                                            AppItem(
                                                id = AppId.CUSTOM_APP,
                                                name = app.appName,
                                                iconBgColor = Color.Transparent,
                                                packageName = app.packageName,
                                                isInstalledApp = true,
                                                customBitmap = bmp,
                                                isDock = true
                                            )
                                        }

                                        if (dockAppToReplace != null) {
                                            viewModel.replaceDockApp(dockAppToReplace!!, newAppItem, context)
                                        } else {
                                            viewModel.addDockApp(newAppItem, context)
                                        }
                                        showDockAppPicker = false
                                        dockAppToReplace = null
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(app.iconColor)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (app.iconBitmap != null) {
                                            Image(
                                                bitmap = app.iconBitmap.asImageBitmap(),
                                                contentDescription = app.appName,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.Apps,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = app.appName,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(Icons.Rounded.CheckCircleOutline, contentDescription = null, tint = IOSBlue, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDockAppPicker = false
                        dockAppToReplace = null
                    }) {
                        Text("Selesai", color = IOSBlue, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconItem(
    app: AppItem,
    customIconUri: String? = null,
    showLabel: Boolean = true,
    dayOfWeek: String = "MON",
    dayOfMonth: String = "1",
    hourFloat: Float = 9.7f,
    minuteFloat: Float = 41f,
    secondFloat: Float = 0f,
    isJiggleMode: Boolean = false,
    rotationAngle: Float = 0f,
    onMoveLeft: (() -> Unit)? = null,
    onMoveRight: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onClickDelete: (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(if (showLabel) 64.dp else 60.dp)
            .graphicsLayer {
                rotationZ = rotationAngle
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Enclosing Box sized 64.dp so the badge placed at TopEnd with offset is NEVER clipped!
        Box(
            modifier = Modifier.size(64.dp),
            contentAlignment = Alignment.Center
        ) {
            val effectiveBgColor = if (app.iconBgColor != Color.Transparent) app.iconBgColor else Color(0xFF243B55)
            // Liquid Glass Translucent Icon Box (Default size 60.dp matching App Library and iOS reference)
            LiquidGlassIconBox(
                modifier = Modifier.size(60.dp),
                backgroundColor = effectiveBgColor,
                shape = RoundedCornerShape(13.5.dp)
            ) {
                if (customIconUri != null) {
                    AsyncImage(
                        model = customIconUri,
                        contentDescription = app.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (app.customBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(0.85f)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        Color.White.copy(alpha = 0.22f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                )
                            )
                            .border(
                                BorderStroke(0.8.dp, Color.White.copy(alpha = 0.35f)),
                                RoundedCornerShape(11.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = app.customBitmap.asImageBitmap(),
                            contentDescription = app.name,
                            modifier = Modifier
                                .fillMaxSize(0.88f)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else if (app.isInstalledApp && !app.packageName.isNullOrBlank()) {
                    val ctx = androidx.compose.ui.platform.LocalContext.current
                    val installedBmp = remember(app.packageName) {
                        AppIconCache.getIcon(ctx, app.packageName)
                    }
                    if (installedBmp != null) {
                        Image(
                            bitmap = installedBmp.asImageBitmap(),
                            contentDescription = app.name,
                            modifier = Modifier
                                .fillMaxSize(0.86f)
                                .clip(RoundedCornerShape(10.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        // High quality authentic fallback for well-known apps
                        when {
                            app.name.contains("youtube", ignoreCase = true) -> {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color(0xFFFF0000)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Youtube",
                                        tint = Color.White,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                            app.name.contains("whatsapp", ignoreCase = true) -> {
                                Box(
                                    modifier = Modifier.fillMaxSize().background(Color(0xFF25D366)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ChatBubble,
                                        contentDescription = "Whatsapp",
                                        tint = Color.White,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }
                            else -> {
                                Icon(
                                    imageVector = app.iconVector ?: Icons.Rounded.Apps,
                                    contentDescription = app.name,
                                    tint = Color.White,
                                    modifier = Modifier.size(30.dp)
                                )
                            }
                        }
                    }
                } else {
                    IOS26BuiltInAppIcon(
                        appId = app.id,
                        appName = app.name,
                        iconVector = app.iconVector,
                        dayOfWeek = dayOfWeek,
                        dayOfMonth = dayOfMonth,
                        hourFloat = hourFloat,
                        minuteFloat = minuteFloat,
                        secondFloat = secondFloat,
                        size = 60.dp
                    )
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
                        .size(24.dp)
                        .shadow(elevation = 3.dp, shape = CircleShape)
                        .clip(CircleShape)
                        .background(Color(0xFF8E8E93))
                        .clickable(enabled = onClickDelete != null) {
                            onClickDelete?.invoke()
                        },
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

            // Jiggle Mode Reorder Arrows
            if (isJiggleMode && (onMoveLeft != null || onMoveRight != null)) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .padding(horizontal = 2.dp, vertical = 1.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (onMoveLeft != null) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .clickable { onMoveLeft() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronLeft,
                                contentDescription = "Pindah Kiri",
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                    if (onMoveRight != null) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .clickable { onMoveRight() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ChevronRight,
                                contentDescription = "Pindah Kanan",
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
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
    customIconUri: String? = null,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
    onPickCustomIcon: () -> Unit,
    onResetCustomIcon: (() -> Unit)? = null
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
            if (customIconUri != null) {
                AsyncImage(
                    model = customIconUri,
                    contentDescription = app.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else if (app.iconVector != null) {
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

                if (!app.isDock) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction("Move Left") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pindah ke Kiri",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronLeft,
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

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction("Move Right") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pindah ke Kanan",
                            color = Color.Black,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Rounded.ChevronRight,
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
                }

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

                HorizontalDivider(
                    color = Color(0xFFE5E5EA),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                HorizontalDivider(
                    color = Color(0xFFE5E5EA),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                // Remove App from Home Screen
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction("Remove App") }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hapus App",
                        color = Color(0xFFFF3B30),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (app.isDock) {
                    HorizontalDivider(
                        color = Color(0xFFE5E5EA),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction("Replace Dock App") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ganti Aplikasi Dock",
                            color = Color(0xFF007AFF),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Rounded.SwapHoriz,
                            contentDescription = null,
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    HorizontalDivider(
                        color = Color(0xFFE5E5EA),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction("Remove From Dock") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Hapus dari Dock",
                            color = Color(0xFFFF3B30),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = null,
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else {
                    HorizontalDivider(
                        color = Color(0xFFE5E5EA),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction("Add To Dock") }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Pasang di Dock",
                            color = Color(0xFF34C759),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Rounded.VerticalAlignBottom,
                            contentDescription = null,
                            tint = Color(0xFF34C759),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Change Icon action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPickCustomIcon() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ganti Ikon (Custom Icon)",
                        color = Color(0xFF007AFF),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        imageVector = Icons.Rounded.Image,
                        contentDescription = null,
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(18.dp)
                    )
                }

                if (onResetCustomIcon != null) {
                    HorizontalDivider(
                        color = Color(0xFFE5E5EA),
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onResetCustomIcon() }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Kembalikan Ikon Bawaan",
                            color = Color(0xFFFF3B30),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Icon(
                            imageVector = Icons.Rounded.Restore,
                            contentDescription = null,
                            tint = Color(0xFFFF3B30),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
