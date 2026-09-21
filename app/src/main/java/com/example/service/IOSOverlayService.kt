package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.media.AudioManager
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
import com.example.ui.components.IOSStatusBar
import com.example.viewmodel.DynamicIslandData
import com.example.viewmodel.DynamicIslandMode
import com.example.util.RootExecutor
import com.example.util.IOSTintedBackgroundManager
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
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        RootExecutor.execute("appops set ${context.packageName} SYSTEM_ALERT_WINDOW allow")
                        RootExecutor.execute("pm grant ${context.packageName} android.permission.SYSTEM_ALERT_WINDOW")
                        delay(400)
                        if (Settings.canDrawOverlays(context)) {
                            withContext(Dispatchers.Main) {
                                start(context)
                            }
                        }
                    } catch (ignored: Exception) {}
                }
                return
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
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startForeground(2001, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(2001, notification)
                }
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

        var flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            @Suppress("DEPRECATION")
            flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_OVERSCAN
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            flags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 0
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
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
                var isCharging by remember { mutableStateOf(false) }
                var isExpanded by remember { mutableStateOf(false) }
                var activeNotification by remember { mutableStateOf<AppNotificationData?>(null) }
                var temporaryIslandData by remember { mutableStateOf<DynamicIslandData?>(null) }
                var currentForegroundPkg by remember { mutableStateOf("") }

                // Auto-collapse temporary smart HUDs after 2.5 seconds
                LaunchedEffect(temporaryIslandData) {
                    if (temporaryIslandData != null) {
                        delay(2500)
                        temporaryIslandData = null
                    }
                }

                var lastRingerMode by remember {
                    val audio = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                    mutableIntStateOf(audio?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL)
                }

                // Register intelligent hardware & system broadcast listeners (Charging, Volume, Ringer, Clipboard)
                DisposableEffect(Unit) {
                    val systemReceiver = object : BroadcastReceiver() {
                        override fun onReceive(context: Context?, intent: Intent?) {
                            when (intent?.action) {
                                Intent.ACTION_POWER_CONNECTED -> {
                                    val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                                    val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: batteryPct
                                    isCharging = true
                                    temporaryIslandData = DynamicIslandData(
                                        mode = DynamicIslandMode.BATTERY,
                                        title = "Mengisi Daya",
                                        subtitle = "$pct%",
                                        icon = "charging",
                                        accentColor = 0xFF34C759
                                    )
                                }
                                Intent.ACTION_POWER_DISCONNECTED -> {
                                    val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                                    val pct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: batteryPct
                                    isCharging = false
                                    temporaryIslandData = DynamicIslandData(
                                        mode = DynamicIslandMode.BATTERY,
                                        title = "Dilepas",
                                        subtitle = "$pct%",
                                        icon = "battery",
                                        accentColor = 0xFF8E8E93
                                    )
                                }
                                "android.media.VOLUME_CHANGED_ACTION" -> {
                                    val audio = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
                                    val cur = audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                                    val max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    if (max > 0) {
                                        val pct = ((cur.toFloat() / max.toFloat()) * 100).toInt()
                                        temporaryIslandData = DynamicIslandData(
                                            mode = DynamicIslandMode.VOLUME,
                                            title = "Volume",
                                            subtitle = "$pct%",
                                            icon = if (cur == 0) "volume_mute" else "volume",
                                            accentColor = 0xFFFFFFFF
                                        )
                                    }
                                }
                                AudioManager.RINGER_MODE_CHANGED_ACTION -> {
                                    val audio = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
                                    val newRingerMode = audio.ringerMode
                                    if (newRingerMode != lastRingerMode) {
                                        lastRingerMode = newRingerMode
                                        when (newRingerMode) {
                                            AudioManager.RINGER_MODE_SILENT -> {
                                                temporaryIslandData = DynamicIslandData(
                                                    mode = DynamicIslandMode.NOTIFICATION,
                                                    title = "Hening",
                                                    subtitle = "Mode Senyap Aktif",
                                                    icon = "volume_mute",
                                                    accentColor = 0xFFFF3B30
                                                )
                                            }
                                            AudioManager.RINGER_MODE_VIBRATE -> {
                                                temporaryIslandData = DynamicIslandData(
                                                    mode = DynamicIslandMode.NOTIFICATION,
                                                    title = "Getar",
                                                    subtitle = "Mode Getar Aktif",
                                                    icon = "volume",
                                                    accentColor = 0xFFFF9500
                                                )
                                            }
                                            AudioManager.RINGER_MODE_NORMAL -> {
                                                temporaryIslandData = DynamicIslandData(
                                                    mode = DynamicIslandMode.NOTIFICATION,
                                                    title = "Dering",
                                                    subtitle = "Mode Suara Aktif",
                                                    icon = "volume",
                                                    accentColor = 0xFF34C759
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    val filter = IntentFilter().apply {
                        addAction(Intent.ACTION_POWER_CONNECTED)
                        addAction(Intent.ACTION_POWER_DISCONNECTED)
                        addAction("android.media.VOLUME_CHANGED_ACTION")
                        addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
                    }
                    registerReceiver(systemReceiver, filter)

                    // Smart clipboard detection
                    val clipManager = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clipListener = ClipboardManager.OnPrimaryClipChangedListener {
                        try {
                            val clip = clipManager?.primaryClip
                            if (clip != null && clip.itemCount > 0) {
                                val text = clip.getItemAt(0)?.coerceToText(this@IOSOverlayService)?.toString()?.trim() ?: ""
                                if (text.isNotBlank() && text.length <= 35) {
                                    temporaryIslandData = DynamicIslandData(
                                        mode = DynamicIslandMode.NOTIFICATION,
                                        title = "Papan Klip",
                                        subtitle = "Disalin: \"$text\"",
                                        icon = "terminal",
                                        accentColor = 0xFF007AFF
                                    )
                                } else if (text.isNotBlank()) {
                                    temporaryIslandData = DynamicIslandData(
                                        mode = DynamicIslandMode.NOTIFICATION,
                                        title = "Papan Klip",
                                        subtitle = "Teks disalin ke papan klip",
                                        icon = "terminal",
                                        accentColor = 0xFF007AFF
                                    )
                                }
                            }
                        } catch (_: Exception) {}
                    }
                    clipManager?.addPrimaryClipChangedListener(clipListener)

                    onDispose {
                        try {
                            unregisterReceiver(systemReceiver)
                        } catch (_: Exception) {}
                        try {
                            clipManager?.removePrimaryClipChangedListener(clipListener)
                        } catch (_: Exception) {}
                    }
                }

                // Synchronized Dynamic Island state with Launcher ViewModel
                val bridgeIslandData by OverlayBridge.dynamicIslandData.collectAsState()

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
                val bridgeFocusMode by OverlayBridge.currentFocusMode.collectAsState()

                // Ensure display overscan is always 0,0,0,0 so Status Bar & Dynamic Island
                // never follow downward and remain firmly pinned to the physical top (Y=0)
                LaunchedEffect(Unit) {
                    RootExecutor.resetTopDisplayInset()
                }

                // Detect Foreground Package to smartly adapt Status Bar color to WhatsApp & all other apps
                LaunchedEffect(Unit) {
                    while (isActive) {
                        try {
                            val pkg = IOSTintedBackgroundManager.detectForegroundPackage(this@IOSOverlayService, packageName)
                            if (pkg.isNotBlank() && pkg != currentForegroundPkg) {
                                currentForegroundPkg = pkg
                                OverlayBridge.currentForegroundPackage.value = pkg
                            }
                        } catch (_: Exception) {}
                        delay(600)
                    }
                }

                // Live Clock and Battery
                LaunchedEffect(Unit) {
                    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
                    val bm = getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
                    while (isActive) {
                        currentTime = sdf.format(Date())
                        batteryPct = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100
                        val status = registerReceiver(null, android.content.IntentFilter(Intent.ACTION_BATTERY_CHANGED))
                            ?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                        isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
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

                // Unified Dynamic Island: Prioritize temporary smart HUDs, notifications, calls, music
                val currentIslandData = remember(temporaryIslandData, bridgeIslandData, activeNotification, isInCall, isExpanded, isMusicPlaying, songTitle, songArtist, callName, callDuration) {
                    when {
                        temporaryIslandData != null -> temporaryIslandData!!
                        activeNotification != null -> {
                            DynamicIslandData(
                                mode = if (isExpanded) DynamicIslandMode.NOTIFICATION else DynamicIslandMode.COMPACT,
                                title = activeNotification!!.appName,
                                subtitle = "${activeNotification!!.title}: ${activeNotification!!.message}",
                                icon = activeNotification!!.iconKey,
                                accentColor = activeNotification!!.accentColor,
                                packageName = activeNotification!!.packageName,
                                contentIntent = activeNotification!!.contentIntent
                            )
                        }
                        isInCall -> {
                            DynamicIslandData(
                                mode = DynamicIslandMode.CALL,
                                title = callName.ifBlank { "Panggilan Masuk" },
                                subtitle = callDuration.ifBlank { "00:00" },
                                icon = "phone",
                                accentColor = 0xFF34C759
                            )
                        }
                        bridgeIslandData.mode != DynamicIslandMode.COMPACT -> {
                            bridgeIslandData
                        }
                        isMusicPlaying && isExpanded -> {
                            DynamicIslandData(
                                mode = DynamicIslandMode.MUSIC,
                                title = songTitle.ifBlank { "Music" },
                                subtitle = songArtist.ifBlank { "Apple Music" },
                                icon = "music",
                                accentColor = 0xFFFF2D55
                            )
                        }
                        else -> {
                            bridgeIslandData
                        }
                    }
                }

                // Smart status bar background color & icon contrast adaptation
                val isNightMode = (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES

                val (targetBgColor, isDarkIcons) = remember(currentForegroundPkg, isNightMode) {
                    IOSTintedBackgroundManager.getThemeForPackage(currentForegroundPkg, isNightMode)
                }

                val animatedBgColor by androidx.compose.animation.animateColorAsState(
                    targetValue = targetBgColor,
                    animationSpec = tween(280),
                    label = "StatusBarBgColor"
                )

                // Standalone iOS Status Bar container: owns dedicated top space at Y=0
                // and perfectly blends with the active foreground app via dynamic tinted background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(animatedBgColor)
                ) {
                    IOSStatusBar(
                        dynamicIslandData = currentIslandData,
                        onDismissDynamicIsland = {
                            isExpanded = false
                            OverlayBridge.onDismissDynamicIsland?.invoke()
                        },
                        onOpenControlCenter = {
                            expandSettingsOverApp()
                        },
                        onOpenNotificationCenter = {
                            expandNotificationsOverApp()
                        },
                        currentTime = currentTime,
                        batteryLevel = batteryPct,
                        isBatteryCharging = isCharging,
                        isWifiConnected = true,
                        wifiSignalLevel = 4,
                        cellularBars = 4,
                        networkType = "5G",
                        isAirplaneMode = false,
                        isDarkIcons = isDarkIcons,
                        focusMode = bridgeFocusMode,
                        isMusicPlaying = isMusicPlaying,
                        musicProgress = musicProgress,
                        musicCurrentTimeStr = musicCurrentTimeStr,
                        musicDurationStr = musicDurationStr,
                        onMusicPlayPause = { OverlayBridge.onTogglePlayPause?.invoke() },
                        onMusicNext = { OverlayBridge.onNextSong?.invoke() },
                        onMusicPrev = { OverlayBridge.onPrevSong?.invoke() },
                        onExpandMusic = { isExpanded = !isExpanded },
                        onOpenIslandAction = {
                            val intent = currentIslandData.contentIntent
                            if (intent != null) {
                                try {
                                    intent.send()
                                } catch (_: Exception) {}
                            } else {
                                val targetPkg = currentIslandData.packageName
                                if (!targetPkg.isNullOrBlank()) {
                                    try {
                                        val launchIntent = packageManager.getLaunchIntentForPackage(targetPkg)
                                        if (launchIntent != null) {
                                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            startActivity(launchIntent)
                                        }
                                    } catch (_: Exception) {}
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
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

    private fun expandSettingsOverApp() {
        try {
            @Suppress("WrongConstant")
            val sbm = getSystemService("statusbar")
            val method = sbm?.javaClass?.getMethod("expandSettingsPanel")
            method?.invoke(sbm)
        } catch (e: Exception) {
            serviceScope.launch {
                com.example.util.RootExecutor.executeCommand("cmd statusbar expand-settings || service call statusbar 2")
            }
        }
    }

    private fun expandNotificationsOverApp() {
        try {
            @Suppress("WrongConstant")
            val sbm = getSystemService("statusbar")
            val method = sbm?.javaClass?.getMethod("expandNotificationsPanel")
            method?.invoke(sbm)
        } catch (e: Exception) {
            serviceScope.launch {
                com.example.util.RootExecutor.executeCommand("cmd statusbar expand-notifications || service call statusbar 1")
            }
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
