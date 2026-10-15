package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeDown
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.math.abs
import com.example.viewmodel.DynamicIslandData
import com.example.viewmodel.DynamicIslandMode

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DynamicIsland(
    data: DynamicIslandData,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    isMusicPlaying: Boolean = false,
    musicProgress: Float = 0f,
    musicCurrentTimeStr: String = "0:00",
    musicDurationStr: String = "3:45",
    onMusicPlayPause: () -> Unit = {},
    onMusicNext: () -> Unit = {},
    onMusicPrev: () -> Unit = {},
    onExpandMusic: () -> Unit = {},
    onOpenAction: (() -> Unit)? = null,
    onAnswerCall: (() -> Unit)? = null,
    onDeclineCall: (() -> Unit)? = null,
    onToggleMuteCall: (() -> Unit)? = null,
    onToggleSpeakerCall: (() -> Unit)? = null,
    isCallMuted: Boolean = false,
    isCallSpeaker: Boolean = false,
    onAirPlaySelect: ((String) -> Unit)? = null,
    onSeekMusic: ((Float) -> Unit)? = null,
    onTimerPauseResume: (() -> Unit)? = null,
    onTimerCancel: (() -> Unit)? = null,
    onVoiceMemoStop: (() -> Unit)? = null,
    secondaryData: DynamicIslandData? = null,
    onSecondaryClick: (() -> Unit)? = null
) {
    val isExpanded = data.mode != DynamicIslandMode.COMPACT
    val view = LocalView.current

    // Authentic Apple Dynamic Island spring sizing (accurate physical scale & aspect ratios)
    val targetWidth = when (data.mode) {
        DynamicIslandMode.COMPACT -> {
            when {
                isMusicPlaying -> 168.dp
                data.mode == DynamicIslandMode.CALL || data.isIncomingCall -> 168.dp
                data.mode == DynamicIslandMode.TIMER || secondaryData?.mode == DynamicIslandMode.TIMER -> 158.dp
                data.mode == DynamicIslandMode.NAVIGATION -> 158.dp
                data.mode == DynamicIslandMode.VOICE_MEMO -> 154.dp
                data.mode == DynamicIslandMode.BATTERY -> 146.dp
                data.mode == DynamicIslandMode.SPORTS -> 156.dp
                data.mode == DynamicIslandMode.DELIVERY -> 160.dp
                data.mode == DynamicIslandMode.FLIGHT -> 158.dp
                data.mode == DynamicIslandMode.FOCUS -> 146.dp
                data.isCameraActive || data.isMicActive -> 132.dp
                else -> 116.dp
            }
        }
        DynamicIslandMode.UNLOCK -> 72.dp
        DynamicIslandMode.SILENT_MODE -> 218.dp
        DynamicIslandMode.FACE_ID -> 178.dp
        DynamicIslandMode.NOTIFICATION -> 324.dp
        DynamicIslandMode.CALL -> 328.dp
        DynamicIslandMode.MUSIC -> 332.dp
        DynamicIslandMode.AIRPLAY -> 324.dp
        DynamicIslandMode.TIMER -> 324.dp
        DynamicIslandMode.BATTERY -> 316.dp
        DynamicIslandMode.VOLUME -> 312.dp
        DynamicIslandMode.ACCESSORY -> 324.dp
        DynamicIslandMode.PRIVACY -> 316.dp
        DynamicIslandMode.AIRDROP -> 324.dp
        DynamicIslandMode.HOTSPOT -> 308.dp
        DynamicIslandMode.NAVIGATION -> 326.dp
        DynamicIslandMode.VOICE_MEMO -> 322.dp
        DynamicIslandMode.FOCUS -> 278.dp
        DynamicIslandMode.FIND_MY -> 324.dp
        DynamicIslandMode.SPORTS -> 326.dp
        DynamicIslandMode.DELIVERY -> 326.dp
        DynamicIslandMode.FLIGHT -> 326.dp
        DynamicIslandMode.NETWORK -> 304.dp
        else -> 250.dp
    }

    val targetHeight = when (data.mode) {
        DynamicIslandMode.COMPACT -> 28.dp
        DynamicIslandMode.UNLOCK -> 30.dp
        DynamicIslandMode.SILENT_MODE -> 44.dp
        DynamicIslandMode.FACE_ID -> 46.dp
        DynamicIslandMode.NOTIFICATION -> 68.dp
        DynamicIslandMode.CALL -> if (data.isIncomingCall) 78.dp else 72.dp
        DynamicIslandMode.MUSIC -> 170.dp
        DynamicIslandMode.AIRPLAY -> 164.dp
        DynamicIslandMode.TIMER -> 66.dp
        DynamicIslandMode.BATTERY -> 60.dp
        DynamicIslandMode.VOLUME -> 52.dp
        DynamicIslandMode.ACCESSORY -> 70.dp
        DynamicIslandMode.PRIVACY -> 54.dp
        DynamicIslandMode.AIRDROP -> 64.dp
        DynamicIslandMode.HOTSPOT -> 52.dp
        DynamicIslandMode.NAVIGATION -> 68.dp
        DynamicIslandMode.VOICE_MEMO -> 64.dp
        DynamicIslandMode.FOCUS -> 50.dp
        DynamicIslandMode.FIND_MY -> 62.dp
        DynamicIslandMode.SPORTS -> 62.dp
        DynamicIslandMode.DELIVERY -> 64.dp
        DynamicIslandMode.FLIGHT -> 62.dp
        DynamicIslandMode.NETWORK -> 52.dp
        else -> 46.dp
    }

    val targetCorner = when (data.mode) {
        DynamicIslandMode.COMPACT -> 14.dp
        DynamicIslandMode.UNLOCK -> 15.dp
        DynamicIslandMode.SILENT_MODE -> 22.dp
        DynamicIslandMode.FACE_ID -> 23.dp
        DynamicIslandMode.MUSIC -> 34.dp
        DynamicIslandMode.AIRPLAY -> 34.dp
        else -> 30.dp
    }

    // Spring animation spec - authentic iOS low-bouncy fluid spring physics
    val springSpecDp = spring<androidx.compose.ui.unit.Dp>(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    val islandWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = springSpecDp,
        label = "islandWidth"
    )

    val islandHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = springSpecDp,
        label = "islandHeight"
    )

    val islandCorner by animateDpAsState(
        targetValue = targetCorner,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "islandCorner"
    )

    var showAirPlayMenu by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .width(islandWidth)
                .height(islandHeight)
                .shadow(
                    elevation = if (isExpanded) 14.dp else 2.dp,
                    shape = RoundedCornerShape(islandCorner),
                    ambientColor = Color.Black.copy(alpha = 0.6f),
                    spotColor = Color.Black.copy(alpha = 0.8f)
                )
                .clip(RoundedCornerShape(islandCorner))
                .background(Color.Black)
                .border(
                    width = 0.6.dp,
                    color = Color(0x28FFFFFF),
                    shape = RoundedCornerShape(islandCorner)
                )
                .pointerInput(isExpanded) {
                    if (isExpanded) {
                        detectDragGestures { change, dragAmount ->
                            // Swipe up (collapse) or swipe sideways (dismiss)
                            if (dragAmount.y < -10f || abs(dragAmount.x) > 20f) {
                                change.consume()
                                onDismiss()
                            }
                        }
                    }
                }
                .combinedClickable(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        when {
                            data.mode == DynamicIslandMode.NOTIFICATION -> {
                                if (onOpenAction != null) {
                                    onOpenAction()
                                } else if (data.contentIntent != null) {
                                    try {
                                        data.contentIntent.send()
                                    } catch (_: Exception) {}
                                }
                                onDismiss()
                            }
                            data.mode == DynamicIslandMode.COMPACT -> {
                                if (isMusicPlaying) {
                                    onOpenAction?.invoke() ?: onExpandMusic()
                                } else if (data.isIncomingCall || data.mode == DynamicIslandMode.CALL) {
                                    onOpenAction?.invoke()
                                } else {
                                    onOpenAction?.invoke()
                                }
                            }
                            isExpanded && (data.mode == DynamicIslandMode.VOLUME || data.mode == DynamicIslandMode.BATTERY || data.mode == DynamicIslandMode.SILENT_MODE) -> {
                                onDismiss()
                            }
                            isExpanded -> {
                                onOpenAction?.invoke()
                            }
                        }
                    },
                    onLongClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        if (!isExpanded) {
                            if (isMusicPlaying) {
                                onExpandMusic()
                            } else {
                                onOpenAction?.invoke()
                            }
                        } else {
                            // Long-pressing on expanded island gently dismisses it
                            onDismiss()
                        }
                    }
                )
                .padding(
                    horizontal = if (isExpanded) 12.dp else 8.dp,
                    vertical = if (isExpanded) 6.dp else 2.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            // Subtle top grabber bar on expanded cards
            if (isExpanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp)
                        .width(32.dp)
                        .height(3.5.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x35FFFFFF))
                )
            }

            AnimatedContent(
                targetState = data,
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(160, delayMillis = 30)) + scaleIn(
                        initialScale = 0.92f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)
                    )).togetherWith(
                        fadeOut(animationSpec = tween(100)) + scaleOut(
                            targetScale = 0.92f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                        )
                    )
                },
                label = "DynamicIslandContent"
            ) { currentData ->
                when (currentData.mode) {
                    DynamicIslandMode.COMPACT -> {
                        CompactIslandContent(
                            isMusicPlaying = isMusicPlaying,
                            data = currentData,
                            secondaryData = secondaryData,
                            onExpandMusic = onExpandMusic
                        )
                    }
                    DynamicIslandMode.SILENT_MODE -> {
                        SilentModeContent(isSilent = currentData.isSilent)
                    }
                    DynamicIslandMode.FACE_ID -> {
                        FaceIdContent(
                            context = currentData.faceIdContext,
                            isSuccess = currentData.isFaceIdSuccess
                        )
                    }
                    DynamicIslandMode.VOLUME -> {
                        VolumeContent(data = currentData)
                    }
                    DynamicIslandMode.BATTERY -> {
                        BatteryContent(data = currentData, onDismiss = onDismiss)
                    }
                    DynamicIslandMode.ACCESSORY -> {
                        AccessoryContent(data = currentData)
                    }
                    DynamicIslandMode.PRIVACY -> {
                        PrivacyContent(data = currentData)
                    }
                    DynamicIslandMode.CALL -> {
                        CallContent(
                            data = currentData,
                            onAnswer = onAnswerCall ?: onDismiss,
                            onDecline = onDeclineCall ?: onDismiss,
                            onToggleMute = onToggleMuteCall ?: {},
                            onToggleSpeaker = onToggleSpeakerCall ?: {},
                            isMuted = isCallMuted,
                            isSpeaker = isCallSpeaker
                        )
                    }
                    DynamicIslandMode.AIRDROP -> {
                        AirDropContent(data = currentData)
                    }
                    DynamicIslandMode.HOTSPOT -> {
                        HotspotContent(data = currentData)
                    }
                    DynamicIslandMode.MUSIC -> {
                        MusicContent(
                            data = currentData,
                            isMusicPlaying = isMusicPlaying,
                            musicProgress = musicProgress,
                            musicCurrentTimeStr = musicCurrentTimeStr,
                            musicDurationStr = musicDurationStr,
                            onPlayPause = onMusicPlayPause,
                            onNext = onMusicNext,
                            onPrev = onMusicPrev,
                            onAirPlay = { showAirPlayMenu = !showAirPlayMenu },
                            onSeek = onSeekMusic,
                            onDismiss = onDismiss
                        )
                    }
                    DynamicIslandMode.AIRPLAY -> {
                        AirPlayMenuContent(
                            currentDevice = currentData.activeAirPlayDevice,
                            onSelectDevice = { dev ->
                                onAirPlaySelect?.invoke(dev)
                                onDismiss()
                            }
                        )
                    }
                    DynamicIslandMode.NAVIGATION -> {
                        NavigationContent(data = currentData)
                    }
                    DynamicIslandMode.TIMER -> {
                        TimerContent(
                            data = currentData,
                            onDismiss = onDismiss,
                            onPauseResume = onTimerPauseResume ?: {},
                            onCancel = onTimerCancel ?: onDismiss
                        )
                    }
                    DynamicIslandMode.VOICE_MEMO -> {
                        VoiceMemoContent(
                            data = currentData,
                            onDismiss = onDismiss,
                            onStop = onVoiceMemoStop ?: onDismiss
                        )
                    }
                    DynamicIslandMode.FOCUS -> {
                        FocusContent(data = currentData)
                    }
                    DynamicIslandMode.FIND_MY -> {
                        FindMyContent(data = currentData)
                    }
                    DynamicIslandMode.SPORTS -> {
                        SportsContent(data = currentData)
                    }
                    DynamicIslandMode.DELIVERY -> {
                        DeliveryContent(data = currentData)
                    }
                    DynamicIslandMode.FLIGHT -> {
                        FlightContent(data = currentData)
                    }
                    DynamicIslandMode.NETWORK -> {
                        NetworkContent(data = currentData)
                    }
                    DynamicIslandMode.NOTIFICATION -> {
                        NotificationContent(data = currentData)
                    }
                    DynamicIslandMode.UNLOCK -> {
                        UnlockContent()
                    }
                    else -> {
                        Text(
                            text = currentData.title,
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        // Secondary Island Bubble (Apple-style detached pill for dual activities e.g. Timer + Music)
        if (secondaryData != null && !isExpanded) {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .widthIn(min = 28.dp, max = 58.dp)
                    .shadow(elevation = 2.dp, shape = RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.Black)
                    .border(0.6.dp, Color(0x28FFFFFF), RoundedCornerShape(14.dp))
                    .clickable { onSecondaryClick?.invoke() }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                if (secondaryData.mode == DynamicIslandMode.TIMER) {
                    Text(
                        text = secondaryData.subtitle.ifEmpty { "04:59" },
                        color = Color(0xFFFF9500),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                } else if (secondaryData.mode == DynamicIslandMode.SPORTS) {
                    Text(
                        text = "${secondaryData.sportsHomeScore}-${secondaryData.sportsAwayScore}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = "Secondary Activity",
                        tint = Color(secondaryData.accentColor),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPACT ISLAND CONTENT (Live Activities, Punch-hole Camera, Sensor Optics)
// -------------------------------------------------------------------------
@Composable
private fun CompactIslandContent(
    isMusicPlaying: Boolean,
    data: DynamicIslandData,
    secondaryData: DynamicIslandData? = null,
    onExpandMusic: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        when {
            // Live Music playback in compact mode
            isMusicPlaying -> {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(Color(0xFFFF2D55)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }

                // Privacy dot if camera/mic active
                if (data.isCameraActive) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF34C759)))
                } else if (data.isMicActive) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFFFF9500)))
                }

                DancingEqualizer(color = Color(0xFFFF2D55), isPlaying = true)
            }

            // Ongoing or incoming phone call in compact mode
            data.isIncomingCall || data.mode == DynamicIslandMode.CALL -> {
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF34C759)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                }

                Text(
                    text = data.callDuration.ifBlank { "00:00" },
                    color = Color(0xFF34C759),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Timer running in compact mode
            data.mode == DynamicIslandMode.TIMER -> {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = null,
                    tint = Color(0xFFFF9500),
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = data.subtitle.ifBlank { "04:59" },
                    color = Color(0xFFFF9500),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Turn-by-Turn navigation in compact mode
            data.mode == DynamicIslandMode.NAVIGATION -> {
                Icon(
                    imageVector = Icons.Rounded.TurnRight,
                    contentDescription = null,
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(18.dp)
                )

                Text(
                    text = data.navDistance.ifBlank { "200 m" },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Voice memo recording in compact mode
            data.mode == DynamicIslandMode.VOICE_MEMO -> {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                )

                DancingEqualizer(color = Color(0xFFFF9500), isPlaying = true)
            }

            // Battery charging in compact mode
            data.mode == DynamicIslandMode.BATTERY && data.icon == "charging" -> {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = null,
                    tint = Color(0xFF34C759),
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = data.subtitle.ifBlank { "100%" },
                    color = Color(0xFF34C759),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Live Sports Activity in compact mode
            data.mode == DynamicIslandMode.SPORTS -> {
                Icon(
                    imageVector = Icons.Rounded.SportsSoccer,
                    contentDescription = null,
                    tint = Color(0xFFFF9500),
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = "${data.sportsHomeScore} - ${data.sportsAwayScore}",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Food / Delivery / Ride Hailing in compact mode
            data.mode == DynamicIslandMode.DELIVERY -> {
                Icon(
                    imageVector = if (data.deliveryService.contains("Grab", true)) Icons.Rounded.Fastfood else Icons.Rounded.TwoWheeler,
                    contentDescription = null,
                    tint = Color(0xFF00E676),
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = data.deliveryEta.ifBlank { "5 mnt" },
                    color = Color(0xFF00E676),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Flight Live Activity in compact mode
            data.mode == DynamicIslandMode.FLIGHT -> {
                Icon(
                    imageVector = Icons.Rounded.FlightTakeoff,
                    contentDescription = null,
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(16.dp)
                )

                Text(
                    text = data.flightCode.ifBlank { "GA 412" },
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Focus mode in compact mode
            data.mode == DynamicIslandMode.FOCUS -> {
                Icon(
                    imageVector = Icons.Rounded.Nightlight,
                    contentDescription = null,
                    tint = Color(0xFF5856D6),
                    modifier = Modifier.size(15.dp)
                )

                Text(
                    text = data.focusModeName.ifBlank { "Fokus" },
                    color = Color(0xFF5E5CE6),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Default Idle mode: Realistic Apple front camera optical lens & FaceID sensor dot
            else -> {
                // Front camera optical lens with circular glass sheen
                Box(
                    modifier = Modifier
                        .size(10.5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141416))
                        .border(0.5.dp, Color(0xFF24242A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(4.5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0C0C0E))
                    )
                }

                // Privacy indicator dots (green for camera, orange for microphone)
                if (data.isCameraActive) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF34C759))
                    )
                } else if (data.isMicActive) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFF9500))
                    )
                }

                // Simulated FaceID / ambient light sensor optical dot
                Box(
                    modifier = Modifier
                        .size(7.5.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0A0A0C))
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// 1. HARDWARE & SYSTEM ALERTS
// -------------------------------------------------------------------------

@Composable
private fun SilentModeContent(isSilent: Boolean) {
    val bellShake by rememberInfiniteTransition(label = "bellShake").animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(120, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isSilent) Color(0xFFFF3B30) else Color(0xFF34C759)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isSilent) Icons.Rounded.NotificationsOff else Icons.Rounded.NotificationsActive,
                    contentDescription = "Silent Mode",
                    tint = Color.White,
                    modifier = Modifier
                        .size(16.dp)
                        .graphicsLayer { rotationZ = if (isSilent) bellShake else 0f }
                )
            }
            Text(
                text = if (isSilent) "Mode Hening" else "Dering",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSilent) Color(0xFFFF3B30).copy(alpha = 0.25f) else Color(0xFF34C759).copy(alpha = 0.25f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (isSilent) "Hening" else "Aktif",
                color = if (isSilent) Color(0xFFFF453A) else Color(0xFF30D158),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FaceIdContent(context: String, isSuccess: Boolean) {
    val rotation by rememberInfiniteTransition(label = "faceid").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "faceRot"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (isSuccess) Color(0xFF34C759) else Color(0xFF007AFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSuccess) Icons.Rounded.Check else Icons.Rounded.Lock,
                contentDescription = "Face ID",
                tint = Color.White,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer { if (!isSuccess) rotationZ = rotation }
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Face ID",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (isSuccess) "Terverifikasi" else context,
                color = if (isSuccess) Color(0xFF34C759) else Color(0xFF8E8E93),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun VolumeContent(data: DynamicIslandData) {
    val volPct = data.subtitle.filter { it.isDigit() }.toIntOrNull() ?: 60

    Row(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (volPct == 0) Icons.Rounded.VolumeMute else Icons.AutoMirrored.Rounded.VolumeUp,
                    contentDescription = "Volume",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = "Volume Nada Dering",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(74.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0x33FFFFFF))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((volPct / 100f).coerceIn(0f, 1f))
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White)
                )
            }
            Text(
                text = "$volPct%",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun BatteryContent(data: DynamicIslandData, onDismiss: () -> Unit) {
    val isCharging = data.icon == "charging"
    val pct = data.subtitle.filter { it.isDigit() }.toIntOrNull() ?: 80
    val isLowBattery = !isCharging && pct <= 20

    Row(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (isLowBattery) Color(0xFFFF3B30) else Color(0xFF34C759)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isCharging) Icons.Rounded.Bolt else Icons.Rounded.BatteryStd,
                    contentDescription = "Battery",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = when {
                        isCharging -> "Pengisian Daya"
                        isLowBattery -> "Peringatan Baterai Lemah"
                        else -> "Baterai"
                    },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (isLowBattery) "Hubungkan ke pengisi daya" else "$pct% Terisi",
                    color = if (isLowBattery) Color(0xFFFF453A) else Color(0xFF30D158),
                    fontSize = 11.sp
                )
            }
        }

        // Authentic iOS Battery percentage bar
        Box(
            modifier = Modifier
                .width(46.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.5.dp, if (isLowBattery) Color(0xFFFF3B30) else Color(0xFF34C759), RoundedCornerShape(6.dp))
                .padding(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth((pct / 100f).coerceIn(0.05f, 1f))
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (isLowBattery) Color(0xFFFF3B30) else Color(0xFF34C759))
            )
        }
    }
}

@Composable
private fun AccessoryContent(data: DynamicIslandData) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2C2C2E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Headphones,
                    contentDescription = "Accessory",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = data.accessoryName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Aksesori Terhubung",
                    color = Color(0xFF34C759),
                    fontSize = 11.sp
                )
            }
        }

        // L, R, Case battery pills
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BatteryPill(label = "L", pct = data.batteryLeft)
            BatteryPill(label = "R", pct = data.batteryRight)
            BatteryPill(label = "Case", pct = data.batteryCase)
        }
    }
}

@Composable
private fun BatteryPill(label: String, pct: Int) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF1C1C1E))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = "$label $pct%",
            color = Color(0xFF34C759),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun PrivacyContent(data: DynamicIslandData) {
    val isCam = data.isCameraActive
    val dotColor = if (isCam) Color(0xFF34C759) else Color(0xFFFF9500)

    Row(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(dotColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(dotColor)
                )
            }
            Column {
                Text(
                    text = if (isCam) "Kamera Sedang Aktif" else "Mikrofon Sedang Aktif",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Akses oleh ${data.privacyAppName}",
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp
                )
            }
        }
        Icon(
            imageVector = Icons.Rounded.Security,
            contentDescription = "Privacy Shield",
            tint = dotColor,
            modifier = Modifier.size(18.dp)
        )
    }
}

// -------------------------------------------------------------------------
// 2. CALLS & COMMUNICATIONS (FaceTime, WhatsApp, Telegram, AirDrop, Hotspot)
// -------------------------------------------------------------------------

@Composable
private fun CallContent(
    data: DynamicIslandData,
    onAnswer: () -> Unit,
    onDecline: () -> Unit,
    onToggleMute: () -> Unit = {},
    onToggleSpeaker: () -> Unit = {},
    isMuted: Boolean = false,
    isSpeaker: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(data.accentColor)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = "Call",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = data.title.ifEmpty { "Panggilan Masuk" },
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (data.isIncomingCall) data.subtitle.ifEmpty { "${data.voipAppName} Audio..." } else data.callDuration.ifBlank { "00:00" },
                    color = Color(0xFF34C759),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (data.isIncomingCall) {
            // Interactive Accept & Decline Buttons
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                        .clickable { onDecline() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CallEnd,
                        contentDescription = "Decline Call",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF34C759))
                        .clickable { onAnswer() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Answer Call",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            // Ongoing active call controls: Mute, Speaker, End Call, and dancing audio waves
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DancingEqualizer(color = Color(0xFF34C759))

                // Mute Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) Color(0xFFFF3B30) else Color(0x33FFFFFF))
                        .clickable { onToggleMute() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Rounded.MicOff else Icons.Rounded.Mic,
                        contentDescription = "Mute",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // Speaker Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isSpeaker) Color(0xFF007AFF) else Color(0x33FFFFFF))
                        .clickable { onToggleSpeaker() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Speaker",
                        tint = Color.White,
                        modifier = Modifier.size(15.dp)
                    )
                }

                // End Call Button
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                        .clickable { onDecline() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CallEnd,
                        contentDescription = "End Call",
                        tint = Color.White,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AirDropContent(data: DynamicIslandData) {
    val rot by rememberInfiniteTransition(label = "airdrop_rot").animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.NearMe,
                    contentDescription = "AirDrop",
                    tint = Color.White,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { if (!data.airdropCompleted) rotationZ = rot }
                )
            }
            Column {
                Text(
                    text = "AirDrop · ${data.airdropSender}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (data.airdropCompleted) "Transfer Selesai" else data.airdropFileName,
                    color = if (data.airdropCompleted) Color(0xFF34C759) else Color(0xFF8E8E93),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(if (data.airdropCompleted) Color(0xFF34C759).copy(alpha = 0.25f) else Color(0xFF007AFF).copy(alpha = 0.25f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (data.airdropCompleted) "Selesai" else "${(data.airdropProgress * 100).toInt()}%",
                color = if (data.airdropCompleted) Color(0xFF30D158) else Color(0xFF0A84FF),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun HotspotContent(data: DynamicIslandData) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Link,
                    contentDescription = "Personal Hotspot",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "Hotspot Pribadi",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${data.hotspotDeviceCount} Terhubung (${data.hotspotDeviceName})",
                    color = Color(0xFF0A84FF),
                    fontSize = 11.sp
                )
            }
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(0xFF34C759))
        )
    }
}

// -------------------------------------------------------------------------
// 3. MEDIA PLAYER & AUDIO CONTROLS & AIRPLAY
// -------------------------------------------------------------------------

@Composable
private fun MusicContent(
    data: DynamicIslandData,
    isMusicPlaying: Boolean,
    musicProgress: Float,
    musicCurrentTimeStr: String,
    musicDurationStr: String,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onAirPlay: () -> Unit,
    onSeek: ((Float) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Top row: Artwork, Song Info & Dancing Equalizer
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFF2D55), Color(0xFFFF375F))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.title.ifEmpty { "Apple Music" },
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = data.subtitle.ifEmpty { "Now Playing" },
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            DancingEqualizer(color = Color(0xFFFF2D55), isPlaying = isMusicPlaying)
        }

        // Progress Scrubber Bar with interactive touch seek
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val frac = (offset.x / size.width).coerceIn(0f, 1f)
                            onSeek?.invoke(frac)
                        }
                    },
                contentAlignment = Alignment.CenterStart
            ) {
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
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White)
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = musicCurrentTimeStr, color = Color.LightGray, fontSize = 11.sp)
                Text(text = musicDurationStr, color = Color.LightGray, fontSize = 11.sp)
            }
        }

        // Playback Controls Row: Prev, Play/Pause, Next, AirPlay Switcher, Collapse
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrev, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous Song",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF2D55))
                    .clickable { onPlayPause() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isMusicPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            IconButton(onClick = onNext, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next Song",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            }

            // AirPlay output shortcut
            IconButton(onClick = onAirPlay, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Airplay,
                    contentDescription = "AirPlay Output",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(22.dp)
                )
            }

            IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Collapse Island",
                    tint = Color(0x99FFFFFF),
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun AirPlayMenuContent(
    currentDevice: String,
    onSelectDevice: (String) -> Unit
) {
    val devices = listOf("Speaker iPhone", "AirPods Pro", "Living Room TV (Apple TV)")

    Column(
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Output Audio (AirPlay)",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = Icons.Rounded.Airplay,
                contentDescription = null,
                tint = Color(0xFF007AFF),
                modifier = Modifier.size(16.dp)
            )
        }

        devices.forEach { dev ->
            val isSelected = currentDevice.contains(dev, ignoreCase = true) || (dev.contains("AirPods") && currentDevice.contains("AirPods"))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) Color(0xFF1C1C1E) else Color.Transparent)
                    .clickable { onSelectDevice(dev) }
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = when {
                            dev.contains("AirPods") -> Icons.Rounded.Headphones
                            dev.contains("TV") -> Icons.Rounded.Tv
                            else -> Icons.Rounded.PhoneIphone
                        },
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF007AFF) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = dev,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// 4. NAVIGATION & SYSTEM UTILITIES (Maps, Timer, Voice Memos, Focus, Find My)
// -------------------------------------------------------------------------

@Composable
private fun NavigationContent(data: DynamicIslandData) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34C759)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (data.navTurnIcon) {
                        "turn_left" -> Icons.Rounded.TurnLeft
                        "roundabout" -> Icons.Rounded.Refresh
                        "straight" -> Icons.Rounded.Navigation
                        else -> Icons.Rounded.TurnRight
                    },
                    contentDescription = "Turn Direction",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = data.navInstruction,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Dalam ${data.navDistance} · ${data.navEta}",
                    color = Color(0xFF30D158),
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1C1C1E))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = data.navDistance,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TimerContent(
    data: DynamicIslandData,
    onDismiss: () -> Unit,
    onPauseResume: () -> Unit = {},
    onCancel: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF9500).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Timer,
                    contentDescription = "Timer",
                    tint = Color(0xFFFF9500),
                    modifier = Modifier.size(20.dp)
                )
            }
            Column {
                Text(
                    text = "Timer",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = data.subtitle.ifEmpty { "04:59" },
                    color = Color(0xFFFF9500),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pause / Resume Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FF9500))
                    .clickable { onPauseResume() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Pause,
                    contentDescription = "Pause Timer",
                    tint = Color(0xFFFF9500),
                    modifier = Modifier.size(18.dp)
                )
            }

            // Cancel / Stop Button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
                    .clickable {
                        onCancel()
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Cancel Timer",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun VoiceMemoContent(
    data: DynamicIslandData,
    onDismiss: () -> Unit,
    onStop: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30).copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFF3B30))
                )
            }
            Column {
                Text(
                    text = "Perekam Suara",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = data.voiceMemoDuration.ifEmpty { "01:24" },
                    color = Color(0xFFFF453A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            DancingEqualizer(color = Color(0xFFFF9500))

            // Stop button
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFF3B30))
                    .clickable {
                        onStop()
                        onDismiss()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Stop,
                    contentDescription = "Stop Recording",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun FocusContent(data: DynamicIslandData) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.90f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Nightlight,
                contentDescription = null,
                tint = Color(0xFF5856D6),
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = data.focusModeName,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF5856D6).copy(alpha = 0.25f))
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) {
            Text(
                text = "Aktif",
                color = Color(0xFF5E5CE6),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FindMyContent(data: DynamicIslandData) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF34C759)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Navigation,
                    contentDescription = "Find My Arrow",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = data.findMyDeviceName,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${data.findMyDistance} di depan Anda",
                    color = Color(0xFF30D158),
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF2C2C2E)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.VolumeUp,
                contentDescription = "Play Sound",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// -------------------------------------------------------------------------
// 5. LIVE ACTIVITIES (Sports, Delivery/Ride Hailing, Flight)
// -------------------------------------------------------------------------

@Composable
private fun SportsContent(data: DynamicIslandData) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.SportsSoccer,
                contentDescription = null,
                tint = Color(0xFFFF9500),
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "${data.sportsHomeTeam} vs ${data.sportsAwayTeam}",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF1C1C1E))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "${data.sportsHomeScore} - ${data.sportsAwayScore}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = data.sportsMatchTime,
                color = Color(0xFF34C759),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun DeliveryContent(data: DynamicIslandData) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00AA13)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.TwoWheeler,
                    contentDescription = "Driver",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "${data.deliveryService} · ${data.deliveryDriverName}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${data.deliveryVehiclePlate} · ${data.deliveryStatus}",
                    color = Color(0xFF00C853),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF00AA13).copy(alpha = 0.25f))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = data.deliveryEta,
                color = Color(0xFF00E676),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FlightContent(data: DynamicIslandData) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.96f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF007AFF)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.FlightTakeoff,
                    contentDescription = "Flight",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column {
                Text(
                    text = "${data.flightCode} · ${data.flightRoute}",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${data.flightGate} · ${data.flightEta}",
                    color = Color(0xFF8E8E93),
                    fontSize = 11.sp
                )
            }
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF34C759).copy(alpha = 0.25f))
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = data.flightStatus,
                color = Color(0xFF30D158),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun NetworkContent(data: DynamicIslandData) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF007AFF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (data.icon) {
                    "bluetooth" -> Icons.Rounded.Bluetooth
                    "airplane" -> Icons.Rounded.Flight
                    else -> Icons.Rounded.Wifi
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Column {
            Text(
                text = data.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = data.subtitle,
                color = Color(0xFF8E8E93),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun NotificationContent(data: DynamicIslandData) {
    Row(
        modifier = Modifier
            .fillMaxWidth(0.94f)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(data.accentColor)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = when (data.icon) {
                    "message" -> Icons.Rounded.ChatBubble
                    "whatsapp" -> Icons.Rounded.Chat
                    "mail" -> Icons.Rounded.Mail
                    "phone" -> Icons.Rounded.Call
                    "camera" -> Icons.Rounded.CameraAlt
                    "shield" -> Icons.Rounded.Security
                    "speed" -> Icons.Rounded.Speed
                    "memory" -> Icons.Rounded.Memory
                    "terminal" -> Icons.Rounded.Terminal
                    else -> Icons.Rounded.Notifications
                },
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = data.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = data.subtitle,
                color = Color(0xFFC7C7CC),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(Color(data.accentColor))
        )
    }
}

@Composable
private fun UnlockContent() {
    var isUnlockedAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(60)
        isUnlockedAnim = true
    }
    val lockScale by animateFloatAsState(
        targetValue = if (isUnlockedAnim) 1.15f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "lockScale"
    )
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isUnlockedAnim) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
            contentDescription = "Unlocked",
            tint = Color.White,
            modifier = Modifier
                .size(17.dp)
                .graphicsLayer {
                    scaleX = lockScale
                    scaleY = lockScale
                }
        )
    }
}

// -------------------------------------------------------------------------
// REUSABLE EQUALIZER ANIMATION
// -------------------------------------------------------------------------

@Composable
private fun DancingEqualizer(
    color: Color,
    isPlaying: Boolean = true
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isPlaying) {
            val infiniteTransition = rememberInfiniteTransition(label = "eq_bars")
            val h1 by infiniteTransition.animateFloat(
                initialValue = 4f, targetValue = 18f,
                animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
                label = "h1"
            )
            val h2 by infiniteTransition.animateFloat(
                initialValue = 16f, targetValue = 6f,
                animationSpec = infiniteRepeatable(tween(420, easing = LinearEasing), RepeatMode.Reverse),
                label = "h2"
            )
            val h3 by infiniteTransition.animateFloat(
                initialValue = 8f, targetValue = 20f,
                animationSpec = infiniteRepeatable(tween(290, easing = LinearEasing), RepeatMode.Reverse),
                label = "h3"
            )
            val h4 by infiniteTransition.animateFloat(
                initialValue = 14f, targetValue = 5f,
                animationSpec = infiniteRepeatable(tween(510, easing = LinearEasing), RepeatMode.Reverse),
                label = "h4"
            )
            listOf(h1, h2, h3, h4).forEach { h ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(h.dp)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(color)
                )
            }
        } else {
            // When music is paused or stopped, equalizer is static and paused!
            listOf(4.dp, 6.dp, 4.dp, 5.dp).forEach { h ->
                Box(
                    modifier = Modifier
                        .width(3.dp)
                        .height(h)
                        .clip(RoundedCornerShape(1.5.dp))
                        .background(color.copy(alpha = 0.5f))
                )
            }
        }
    }
}
