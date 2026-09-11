package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.util.RootExecutor
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class IOSOverlayService : Service() {

    private var windowManager: WindowManager? = null
    private var overlayView: ComposeView? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())

    private val overlayLifecycleOwner = object : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)
        private val store = ViewModelStore()

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry
        override val viewModelStore: ViewModelStore get() = store

        fun create() {
            savedStateRegistryController.performRestore(null)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        }

        fun destroy() {
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            store.clear()
        }
    }

    companion object {
        var isRunning: Boolean = false

        fun start(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                try {
                    RootExecutor.execute("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
                    RootExecutor.execute("pm grant ${context.packageName} android.permission.SYSTEM_ALERT_WINDOW")
                } catch (ignored: Exception) {}
                if (!Settings.canDrawOverlays(context)) {
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:${context.packageName}")
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try { context.startActivity(intent) } catch (ignored: Exception) {}
                    return
                }
            }
            val intent = Intent(context, IOSOverlayService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                try {
                    context.startService(intent)
                } catch (ignored: Exception) {}
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, IOSOverlayService::class.java)
            context.stopService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val channelId = "ios_overlay_channel"
                val channel = NotificationChannel(
                    channelId,
                    "iOS Dynamic Island & Status Bar Overlay",
                    NotificationManager.IMPORTANCE_LOW
                )
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                manager?.createNotificationChannel(channel)
                val notification = Notification.Builder(this, channelId)
                    .setContentTitle("iOS Dynamic Island Active")
                    .setContentText("Dynamic Island & Status Bar running over all apps")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .build()
                startForeground(2001, notification)
            } catch (e: Exception) {}
        }
        overlayLifecycleOwner.create()
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            y = 0
        }

        overlayView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(overlayLifecycleOwner)
            setViewTreeSavedStateRegistryOwner(overlayLifecycleOwner)
            setViewTreeViewModelStoreOwner(overlayLifecycleOwner)
            setContent {
                val isLauncherForeground by OverlayBridge.isLauncherForeground.collectAsState()
                val isOverlayEnabled by OverlayBridge.isOverlayEnabled.collectAsState()

                // HIDE OVERLAY COMPLETELY WHEN LAUNCHER (MainActivity) IS IN FOREGROUND!
                // This guarantees only 1 Status Bar and 1 Dynamic Island exists at any time.
                // When user switches to WhatsApp, Instagram, etc., isLauncherForeground becomes false
                // and the iOS Dynamic Island & Status Bar smoothly appears over those third-party apps!
                LaunchedEffect(isLauncherForeground, isOverlayEnabled) {
                    overlayView?.visibility = if (!isLauncherForeground && isOverlayEnabled) View.VISIBLE else View.GONE
                }

                if (isLauncherForeground || !isOverlayEnabled) {
                    return@setContent
                }

                var currentTime by remember { mutableStateOf("09:41") }
                var batteryPct by remember { mutableIntStateOf(100) }
                var isExpanded by remember { mutableStateOf(false) }
                var activeNotification by remember { mutableStateOf<AppNotificationData?>(null) }

                // Collect Cross-App States from OverlayBridge
                val isMusicPlaying by OverlayBridge.isMusicPlaying.collectAsState()
                val songTitle by OverlayBridge.songTitle.collectAsState()
                val songArtist by OverlayBridge.songArtist.collectAsState()
                val musicProgress by OverlayBridge.musicProgress.collectAsState()
                val musicCurrentTimeStr by OverlayBridge.musicCurrentTimeStr.collectAsState()
                val musicDurationStr by OverlayBridge.musicDurationStr.collectAsState()

                val isInCall by OverlayBridge.isInCall.collectAsState()
                val callName by OverlayBridge.callName.collectAsState()
                val callDuration by OverlayBridge.callDuration.collectAsState()

                // Live Clock and Battery (only updates when active in other apps)
                LaunchedEffect(Unit) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                    while (isActive) {
                        currentTime = sdf.format(Date())
                        batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
                        delay(2000)
                    }
                }

                // Collect incoming notifications (WhatsApp, Instagram, Telegram, SMS, etc.)
                LaunchedEffect(Unit) {
                    IOSNotificationListenerService.notificationFlow.collect { notif ->
                        activeNotification = notif
                        isExpanded = true
                        delay(5000)
                        activeNotification = null
                        isExpanded = false
                    }
                }

                // Equalizer Bar Animation - only active when music is actually playing to save battery/CPU!
                val infiniteTransition = rememberInfiniteTransition(label = "music_eq")
                val bar1 by if (isMusicPlaying) {
                    infiniteTransition.animateFloat(
                        initialValue = 0.3f, targetValue = 1.0f,
                        animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
                        label = "bar1"
                    )
                } else {
                    remember { mutableFloatStateOf(0.4f) }
                }
                val bar2 by if (isMusicPlaying) {
                    infiniteTransition.animateFloat(
                        initialValue = 0.8f, targetValue = 0.2f,
                        animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
                        label = "bar2"
                    )
                } else {
                    remember { mutableFloatStateOf(0.6f) }
                }
                val bar3 by if (isMusicPlaying) {
                    infiniteTransition.animateFloat(
                        initialValue = 0.2f, targetValue = 0.9f,
                        animationSpec = infiniteRepeatable(tween(300, easing = LinearEasing), RepeatMode.Reverse),
                        label = "bar3"
                    )
                } else {
                    remember { mutableFloatStateOf(0.3f) }
                }
                val bar4 by if (isMusicPlaying) {
                    infiniteTransition.animateFloat(
                        initialValue = 0.6f, targetValue = 0.3f,
                        animationSpec = infiniteRepeatable(tween(480, easing = LinearEasing), RepeatMode.Reverse),
                        label = "bar4"
                    )
                } else {
                    remember { mutableFloatStateOf(0.5f) }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Status Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Clock (tappable to open launcher)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    val launchIntent = Intent(this@IOSOverlayService, MainActivity::class.java).apply {
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                                    }
                                    startActivity(launchIntent)
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = currentTime,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Center: Dynamic Island Pill
                        Box(
                            modifier = Modifier
                                .animateContentSize(animationSpec = spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium))
                                .clip(RoundedCornerShape(26.dp))
                                .background(Color.Black)
                                .clickable {
                                    isExpanded = !isExpanded
                                }
                                .padding(
                                    horizontal = if (isExpanded) 14.dp else 10.dp,
                                    vertical = if (isExpanded) 10.dp else 6.dp
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // PRIORITY 1: INCOMING NOTIFICATION (WhatsApp, Instagram, etc.)
                            if (activeNotification != null) {
                                val notif = activeNotification!!
                                if (isExpanded) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.widthIn(max = 290.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(notif.accentColor)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Notifications,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "${notif.appName}: ${notif.title}",
                                                color = Color.White,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = notif.message,
                                                color = Color(0xFFD1D1D6),
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(notif.accentColor))
                                        )
                                        Text(
                                            text = "${notif.appName}: ${notif.title}",
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 140.dp)
                                        )
                                    }
                                }
                            }
                            // PRIORITY 2: ACTIVE CALL (In WhatsApp, Instagram, or Phone Call)
                            else if (isInCall) {
                                if (isExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .widthIn(max = 280.dp)
                                            .padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF34C759)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Call,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = callName,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1
                                                )
                                                Text(
                                                    text = "In Call • $callDuration",
                                                    color = Color(0xFF34C759),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                            // End Call Button
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFFF3B30))
                                                    .clickable {
                                                        OverlayBridge.onEndCall?.invoke()
                                                        isExpanded = false
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CallEnd,
                                                    contentDescription = "End Call",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Compact Call Dynamic Island
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF34C759))
                                        )
                                        Text(
                                            text = callName,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 90.dp)
                                        )
                                        Text(
                                            text = callDuration,
                                            color = Color(0xFF34C759),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                            // PRIORITY 3: LIVE MUSIC PLAYING
                            else if (isMusicPlaying) {
                                if (isExpanded) {
                                    // Full Expanded iOS Music Dynamic Island Card
                                    Column(
                                        modifier = Modifier
                                            .widthIn(max = 290.dp)
                                            .padding(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            // Album Art
                                            Box(
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        Brush.linearGradient(
                                                            listOf(Color(0xFFFF2D55), Color(0xFF5856D6))
                                                        )
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.MusicNote,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = songTitle,
                                                    color = Color.White,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = songArtist,
                                                    color = Color(0xFF8E8E93),
                                                    fontSize = 11.sp,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }

                                            // Animated Live Equalizer Wave
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                verticalAlignment = Alignment.Bottom,
                                                modifier = Modifier.height(20.dp)
                                            ) {
                                                Box(modifier = Modifier.width(3.dp).height((18 * bar1).dp.coerceAtLeast(4.dp)).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF2D55)))
                                                Box(modifier = Modifier.width(3.dp).height((18 * bar2).dp.coerceAtLeast(4.dp)).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF2D55)))
                                                Box(modifier = Modifier.width(3.dp).height((18 * bar3).dp.coerceAtLeast(4.dp)).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF2D55)))
                                                Box(modifier = Modifier.width(3.dp).height((18 * bar4).dp.coerceAtLeast(4.dp)).clip(RoundedCornerShape(2.dp)).background(Color(0xFFFF2D55)))
                                            }
                                        }

                                        // Progress Bar
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(Color(0x33FFFFFF))
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth(musicProgress.coerceIn(0f, 1f))
                                                        .fillMaxHeight()
                                                        .background(Color.White)
                                                )
                                            }
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(text = musicCurrentTimeStr, color = Color(0xFF8E8E93), fontSize = 10.sp)
                                                Text(text = musicDurationStr, color = Color(0xFF8E8E93), fontSize = 10.sp)
                                            }
                                        }

                                        // Media Controls
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceEvenly,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .clickable { OverlayBridge.onPrevSong?.invoke() },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.SkipPrevious,
                                                    contentDescription = "Previous",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                                    .clickable { OverlayBridge.onTogglePlayPause?.invoke() },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Pause,
                                                    contentDescription = "Pause",
                                                    tint = Color.Black,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .clickable { OverlayBridge.onNextSong?.invoke() },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.SkipNext,
                                                    contentDescription = "Next",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    // Compact Music Pill with Live Animated Wave
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Mini Album Art
                                        Box(
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    Brush.linearGradient(
                                                        listOf(Color(0xFFFF2D55), Color(0xFF5856D6))
                                                    )
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.MusicNote,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(10.dp)
                                            )
                                        }

                                        Text(
                                            text = songTitle,
                                            color = Color.White,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.widthIn(max = 110.dp)
                                        )

                                        // Live Equalizer Sound Wave Animation
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                            verticalAlignment = Alignment.Bottom,
                                            modifier = Modifier.height(13.dp)
                                        ) {
                                            Box(modifier = Modifier.width(2.5.dp).height((12 * bar1).dp.coerceAtLeast(3.dp)).clip(RoundedCornerShape(1.dp)).background(Color(0xFFFF2D55)))
                                            Box(modifier = Modifier.width(2.5.dp).height((12 * bar2).dp.coerceAtLeast(3.dp)).clip(RoundedCornerShape(1.dp)).background(Color(0xFFFF2D55)))
                                            Box(modifier = Modifier.width(2.5.dp).height((12 * bar3).dp.coerceAtLeast(3.dp)).clip(RoundedCornerShape(1.dp)).background(Color(0xFFFF2D55)))
                                            Box(modifier = Modifier.width(2.5.dp).height((12 * bar4).dp.coerceAtLeast(3.dp)).clip(RoundedCornerShape(1.dp)).background(Color(0xFFFF2D55)))
                                        }
                                    }
                                }
                            }
                            // PRIORITY 4: STANDARD COMPACT DYNAMIC ISLAND PILL
                            else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Camera lens circle
                                    Box(
                                        modifier = Modifier
                                            .size(9.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF1C1C1E))
                                    )
                                    // iOS indicator dot
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF34C759))
                                    )
                                }
                            }
                        }

                        // Right: Signal, Wi-Fi, Battery
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SignalCellularAlt,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Icon(
                                imageVector = Icons.Rounded.Wifi,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = "$batteryPct%",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Icon(
                                    imageVector = Icons.Rounded.BatteryFull,
                                    contentDescription = null,
                                    tint = if (batteryPct > 20) Color(0xFF34C759) else Color(0xFFFF3B30),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        try {
            overlayView?.visibility = if (!OverlayBridge.isLauncherForeground.value && OverlayBridge.isOverlayEnabled.value) View.VISIBLE else View.GONE
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        overlayLifecycleOwner.destroy()
        super.onDestroy()
        isRunning = false
        serviceScope.cancel()
        if (overlayView != null) {
            try {
                windowManager?.removeView(overlayView)
            } catch (ignored: Exception) {}
            overlayView = null
        }
    }
}
