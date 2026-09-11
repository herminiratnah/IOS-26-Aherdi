package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.DynamicIslandData
import com.example.viewmodel.DynamicIslandMode

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
    onExpandMusic: () -> Unit = {}
) {
    val isExpanded = data.mode != DynamicIslandMode.COMPACT

    Box(
        modifier = modifier
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
            .clip(RoundedCornerShape(32.dp))
            .background(Color.Black)
            .clickable {
                if (isExpanded) {
                    onDismiss()
                } else if (isMusicPlaying) {
                    onExpandMusic()
                }
            }
            .padding(horizontal = if (isExpanded) 14.dp else 10.dp, vertical = if (isExpanded) 10.dp else 5.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(targetState = data, label = "DynamicIslandContent") { currentData ->
            when (currentData.mode) {
                DynamicIslandMode.COMPACT -> {
                    // Sleek authentic pill shape with camera dot and active music visualizer
                    Row(
                        modifier = Modifier
                            .width(if (isMusicPlaying) 125.dp else 115.dp)
                            .height(24.dp)
                            .clickable {
                                if (isMusicPlaying) onExpandMusic()
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Simulated front camera sensor punch-hole
                        Box(
                            modifier = Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF141416))
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (isMusicPlaying) {
                            // Active music wave visualizer in compact pill
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "compact_eq")
                                val h1 by infiniteTransition.animateFloat(
                                    initialValue = 4f, targetValue = 12f,
                                    animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing), RepeatMode.Reverse),
                                    label = "c1"
                                )
                                val h2 by infiniteTransition.animateFloat(
                                    initialValue = 12f, targetValue = 5f,
                                    animationSpec = infiniteRepeatable(tween(500, easing = LinearEasing), RepeatMode.Reverse),
                                    label = "c2"
                                )
                                val h3 by infiniteTransition.animateFloat(
                                    initialValue = 6f, targetValue = 14f,
                                    animationSpec = infiniteRepeatable(tween(350, easing = LinearEasing), RepeatMode.Reverse),
                                    label = "c3"
                                )
                                listOf(h1, h2, h3).forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(h.dp)
                                            .clip(RoundedCornerShape(1.dp))
                                            .background(Color(0xFFFF2D55))
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        // Simulated FaceID / ambient light sensor dot
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0A0A0C))
                        )
                    }
                }
                DynamicIslandMode.NOTIFICATION -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(currentData.accentColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (currentData.icon) {
                                    "message" -> Icons.Rounded.ChatBubble
                                    "whatsapp" -> Icons.Rounded.Chat
                                    "charging" -> Icons.Rounded.Bolt
                                    "battery" -> Icons.Rounded.BatteryStd
                                    "volume" -> Icons.Rounded.VolumeUp
                                    "volume_mute" -> Icons.Rounded.VolumeMute
                                    "mail" -> Icons.Rounded.Mail
                                    "phone" -> Icons.Rounded.Call
                                    "camera" -> Icons.Rounded.CameraAlt
                                    "wifi" -> Icons.Rounded.Wifi
                                    "bluetooth" -> Icons.Rounded.Bluetooth
                                    "airplane" -> Icons.Rounded.Flight
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
                                text = currentData.title,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentData.subtitle,
                                color = Color(0xFFC7C7CC),
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Small indicator
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(currentData.accentColor))
                        )
                    }
                }
                DynamicIslandMode.CALL -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
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
                                    .background(Color(0xFF34C759)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Call,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = currentData.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = currentData.subtitle,
                                    color = Color(0xFF34C759),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        // Green audio waves simulation
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            listOf(10.dp, 16.dp, 8.dp, 14.dp, 12.dp).forEach { h ->
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height(h)
                                        .clip(RoundedCornerShape(1.dp))
                                        .background(Color(0xFF34C759))
                                )
                            }
                        }
                    }
                }
                DynamicIslandMode.MUSIC -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth(0.96f)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Top row: Artwork, Song Info & Dancing Equalizer Bars
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
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFFF2D55)),
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
                                        text = currentData.title.ifEmpty { "Apple Music" },
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentData.subtitle.ifEmpty { "Now Playing" },
                                        color = Color.LightGray,
                                        fontSize = 12.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Dynamic animated dancing equalizer bars
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val infiniteTransition = rememberInfiniteTransition(label = "island_eq")
                                val h1 by infiniteTransition.animateFloat(
                                    initialValue = 6f, targetValue = 22f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(450, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "eq1"
                                )
                                val h2 by infiniteTransition.animateFloat(
                                    initialValue = 18f, targetValue = 8f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(550, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "eq2"
                                )
                                val h3 by infiniteTransition.animateFloat(
                                    initialValue = 10f, targetValue = 24f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(400, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "eq3"
                                )
                                val h4 by infiniteTransition.animateFloat(
                                    initialValue = 20f, targetValue = 10f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(600, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "eq4"
                                )
                                listOf(h1, h2, h3, h4).forEach { h ->
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(if (isMusicPlaying) h.dp else 8.dp)
                                            .clip(RoundedCornerShape(1.5.dp))
                                            .background(Color(0xFFFF2D55))
                                    )
                                }
                            }
                        }

                        // Progress Scrubber Bar
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
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
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = musicCurrentTimeStr,
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                                Text(
                                    text = musicDurationStr,
                                    color = Color.LightGray,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        // Playback Controls Row: Prev, Play/Pause, Next, Collapse
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onMusicPrev,
                                modifier = Modifier.size(36.dp)
                            ) {
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
                                    .clickable { onMusicPlayPause() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isMusicPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            IconButton(
                                onClick = onMusicNext,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Next Song",
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.size(36.dp)
                            ) {
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
                DynamicIslandMode.TIMER -> {
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
                                imageVector = Icons.Rounded.Timer,
                                contentDescription = null,
                                tint = Color(0xFFFF9500),
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Timer",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = currentData.subtitle,
                            color = Color(0xFFFF9500),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                DynamicIslandMode.BATTERY -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
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
                                    .background(Color(currentData.accentColor)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentData.icon == "charging") Icons.Rounded.Bolt else Icons.Rounded.BatteryStd,
                                    contentDescription = "Battery",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = currentData.title,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = currentData.subtitle,
                                    color = Color(currentData.accentColor),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        // Authentic iOS Battery percentage bar
                        val pct = currentData.subtitle.filter { it.isDigit() }.toFloatOrNull() ?: 80f
                        Box(
                            modifier = Modifier
                                .width(44.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .border(1.5.dp, Color(currentData.accentColor), RoundedCornerShape(6.dp))
                                .padding(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((pct / 100f).coerceIn(0.05f, 1f))
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(currentData.accentColor))
                            )
                        }
                    }
                }
                DynamicIslandMode.VOLUME -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
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
                                    imageVector = if (currentData.icon == "volume_mute") Icons.Rounded.VolumeMute else Icons.Rounded.VolumeUp,
                                    contentDescription = "Volume",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Volume",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        val volPct = currentData.subtitle.filter { it.isDigit() }.toIntOrNull() ?: 50
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(70.dp)
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
}
