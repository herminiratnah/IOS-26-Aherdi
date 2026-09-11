package com.example.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object OverlayBridge {
    // Music State
    val isMusicPlaying = MutableStateFlow(false)
    val songTitle = MutableStateFlow("Blinding Lights")
    val songArtist = MutableStateFlow("The Weeknd")
    val musicProgress = MutableStateFlow(0.35f)
    val musicDurationStr = MutableStateFlow("03:20")
    val musicCurrentTimeStr = MutableStateFlow("01:10")

    // Phone Call State
    val isInCall = MutableStateFlow(false)
    val isRinging = MutableStateFlow(false)
    val callName = MutableStateFlow("Dimas")
    val callNumber = MutableStateFlow("0857-1122-3344")
    val callDuration = MutableStateFlow("01:15")

    // Launcher Foreground / Visibility State
    // When true, user is inside our launcher app -> Overlay hides so there is ONLY ONE status bar & Dynamic Island!
    // When false, user is in third-party apps (WhatsApp, IG, etc.) -> Overlay appears at top!
    val isLauncherForeground = MutableStateFlow(true)
    val isOverlayEnabled = MutableStateFlow(true)

    // Actions
    var onTogglePlayPause: (() -> Unit)? = null
    var onNextSong: (() -> Unit)? = null
    var onPrevSong: (() -> Unit)? = null
    var onEndCall: (() -> Unit)? = null
    var onToggleMute: (() -> Unit)? = null
    var onToggleSpeaker: (() -> Unit)? = null
}
