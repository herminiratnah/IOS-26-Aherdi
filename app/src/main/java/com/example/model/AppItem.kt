package com.example.model

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
    ROOT_TOOLS
}

data class AppItem(
    val id: AppId,
    val name: String,
    val iconBgColor: Color,
    val iconVector: ImageVector? = null,
    val badgeCount: Int = 0,
    val isDock: Boolean = false
)
