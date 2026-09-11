package com.example.service

import android.app.Notification
import android.content.Context
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

data class AppNotificationData(
    val packageName: String,
    val appName: String,
    val title: String,
    val message: String,
    val iconKey: String,
    val accentColor: Long
)

class IOSNotificationListenerService : NotificationListenerService() {

    companion object {
        private val _notificationFlow = MutableSharedFlow<AppNotificationData>(extraBufferCapacity = 20)
        val notificationFlow: SharedFlow<AppNotificationData> = _notificationFlow.asSharedFlow()
        var isConnected: Boolean = false

        fun isPermissionGranted(context: Context): Boolean {
            return NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        }
    }

    private val serviceScope = CoroutineScope(Dispatchers.Default)

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        val pkg = sbn.packageName ?: return

        // Skip our own app notifications
        if (pkg == packageName) return

        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        // Extract title and text
        val title = extras.getString(Notification.EXTRA_TITLE)
            ?: extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
            ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
            ?: extras.getString(Notification.EXTRA_BIG_TEXT)
            ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
            ?: ""

        // Skip empty or purely ongoing background service notifications
        if (title.isBlank() && text.isBlank()) return
        if ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0) return

        val pm = packageManager
        val appName = try {
            val appInfo = pm.getApplicationInfo(pkg, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            pkg
        }

        // Map apps to custom iOS icon and accent colors
        val (iconKey, accentColor) = when {
            pkg.contains("whatsapp", ignoreCase = true) -> Pair("whatsapp", 0xFF25D366)
            pkg.contains("instagram", ignoreCase = true) -> Pair("instagram", 0xFFE1306C)
            pkg.contains("telegram", ignoreCase = true) -> Pair("telegram", 0xFF2AABEE)
            pkg.contains("twitter", ignoreCase = true) || pkg.contains(".x.", ignoreCase = true) -> Pair("message", 0xFF1DA1F2)
            pkg.contains("tiktok", ignoreCase = true) -> Pair("music", 0xFFEE1D52)
            pkg.contains("youtube", ignoreCase = true) -> Pair("music", 0xFFFF0000)
            pkg.contains("spotify", ignoreCase = true) -> Pair("music", 0xFF1ED760)
            pkg.contains("mail", ignoreCase = true) || pkg.contains("gmail", ignoreCase = true) -> Pair("mail", 0xFFEA4335)
            pkg.contains("dialer", ignoreCase = true) || pkg.contains("phone", ignoreCase = true) -> Pair("phone", 0xFF34C759)
            pkg.contains("mms", ignoreCase = true) || pkg.contains("messaging", ignoreCase = true) -> Pair("message", 0xFF34C759)
            pkg.contains("bank", ignoreCase = true) || pkg.contains("bca", ignoreCase = true) || pkg.contains("bri", ignoreCase = true) || pkg.contains("dana", ignoreCase = true) || pkg.contains("ovo", ignoreCase = true) || pkg.contains("gopay", ignoreCase = true) -> Pair("shield", 0xFF007AFF)
            else -> Pair("bell", 0xFF007AFF)
        }

        serviceScope.launch {
            _notificationFlow.emit(
                AppNotificationData(
                    packageName = pkg,
                    appName = appName,
                    title = if (title.isNotBlank()) "$appName: $title" else appName,
                    message = text.ifBlank { "Pemberitahuan baru" },
                    iconKey = iconKey,
                    accentColor = accentColor
                )
            )
        }
    }
}
