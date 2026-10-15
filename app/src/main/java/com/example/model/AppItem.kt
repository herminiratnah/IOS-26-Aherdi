package com.example.model

import android.graphics.Bitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppId {
    FACETIME,
    CALENDAR,
    PHOTOS,
    CAMERA,
    MAIL,
    NOTES,
    REMINDERS,
    CLOCK,
    TV,
    PODCASTS,
    APP_STORE,
    MAPS,
    HEALTH,
    WALLET,
    SETTINGS,
    CALCULATOR,
    PHONE,
    SAFARI,
    MESSAGES,
    MUSIC,
    WEATHER,
    STOCKS,
    BOOKS,
    ROOT_TOOLS,
    DYNAMIC_ISLAND,
    DEPTH_STUDIO,
    CUSTOM_APP
}

data class AppItem(
    val id: AppId = AppId.CUSTOM_APP,
    val name: String,
    val iconBgColor: Color = Color(0xFF007AFF),
    val iconVector: ImageVector? = null,
    val badgeCount: Int = 0,
    val isDock: Boolean = false,
    val packageName: String? = null,
    val isInstalledApp: Boolean = false,
    val customBitmap: Bitmap? = null
)
