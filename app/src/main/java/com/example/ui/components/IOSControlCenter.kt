package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppId
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

@Composable
fun IOSControlCenter(
    viewModel: IOSViewModel,
    isOpen: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val airplaneMode by viewModel.airplaneMode.collectAsState()
    val wifiEnabled by viewModel.wifiEnabled.collectAsState()
    val bluetoothEnabled by viewModel.bluetoothEnabled.collectAsState()
    val cellularEnabled by viewModel.cellularEnabled.collectAsState()
    val flashlightEnabled by viewModel.flashlightEnabled.collectAsState()
    val brightness by viewModel.brightness.collectAsState()
    val volume by viewModel.volume.collectAsState()
    val isMusicPlaying by viewModel.isMusicPlaying.collectAsState()
    val currentSong by viewModel.currentSong.collectAsState()
    val musicProgress by viewModel.musicProgress.collectAsState()
    val musicCurrentTimeStr by viewModel.musicCurrentTimeStr.collectAsState()
    val musicDurationStr by viewModel.musicDurationStr.collectAsState()

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xD9000000))
                .clickable { onDismiss() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .clickable(enabled = false) {}, // Prevent dismiss when tapping inside
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Top Dismiss Indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0x66FFFFFF))
                    )
                }

                // Row 1: Connectivity (2x2) & Music Player
                Row(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Connectivity 4-in-1 Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0x40FFFFFF))
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Airplane Mode
                                ControlCircleIcon(
                                    icon = Icons.Rounded.Flight,
                                    isActive = airplaneMode,
                                    activeColor = IOSOrange,
                                    onClick = { viewModel.toggleAirplaneMode() }
                                )
                                // Cellular Data
                                ControlCircleIcon(
                                    icon = Icons.Rounded.SignalCellularAlt,
                                    isActive = cellularEnabled,
                                    activeColor = IOSGreen,
                                    onClick = { viewModel.toggleCellular() }
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Wi-Fi
                                ControlCircleIcon(
                                    icon = Icons.Rounded.Wifi,
                                    isActive = wifiEnabled,
                                    activeColor = IOSBlue,
                                    onClick = { viewModel.toggleWifi() }
                                )
                                // Bluetooth
                                ControlCircleIcon(
                                    icon = Icons.Rounded.Bluetooth,
                                    isActive = bluetoothEnabled,
                                    activeColor = IOSBlue,
                                    onClick = { viewModel.toggleBluetooth() }
                                )
                            }
                        }
                    }

                    // Music Player Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0x40FFFFFF))
                            .padding(14.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Tap top to open Music App
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.openApp(AppId.MUSIC)
                                        onDismiss()
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (currentSong != null) {
                                                Brush.linearGradient(
                                                    listOf(
                                                        Color(currentSong!!.coverGradientStart),
                                                        Color(currentSong!!.coverGradientEnd)
                                                    )
                                                )
                                            } else {
                                                Brush.linearGradient(listOf(IOSPink, Color(0xFFFF5252)))
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = currentSong?.title ?: "Apple Music",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = currentSong?.artist ?: "Tap to play",
                                        color = Color.LightGray,
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            // Music Scrubber bar
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(3.dp)
                                        .clip(RoundedCornerShape(1.5.dp))
                                        .background(Color(0x33FFFFFF))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(musicProgress.coerceIn(0f, 1f))
                                            .fillMaxHeight()
                                            .clip(RoundedCornerShape(1.5.dp))
                                            .background(Color.White)
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = musicCurrentTimeStr,
                                        color = Color(0xAAFFFFFF),
                                        fontSize = 9.sp
                                    )
                                    Text(
                                        text = musicDurationStr,
                                        color = Color(0xAAFFFFFF),
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            // Controls: Prev, Play/Pause, Next
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipPrevious,
                                    contentDescription = "Prev",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { viewModel.skipPreviousSong(context) }
                                )
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x55FFFFFF))
                                        .clickable { viewModel.toggleMusicPlay() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isMusicPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = "Play",
                                        tint = Color.White,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = "Next",
                                    tint = Color.White,
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clickable { viewModel.skipNextSong(context) }
                                )
                            }
                        }
                    }
                }

                // Row 2: Sliders & Small Cards
                Row(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Two small cards: Rotation Lock & Screen Mirror
                    Column(
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Rotation lock
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x40FFFFFF))
                                .clickable { },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ScreenRotation,
                                contentDescription = "Rotation Lock",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        // Screen Mirror
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Color(0x40FFFFFF))
                                .clickable { },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Cast,
                                contentDescription = "AirPlay",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Brightness Vertical Slider
                    VerticalIOSSlider(
                        value = brightness,
                        onValueChange = { viewModel.setBrightness(it) },
                        icon = Icons.Rounded.WbSunny,
                        modifier = Modifier.weight(0.5f).fillMaxHeight()
                    )

                    // Volume Vertical Slider
                    VerticalIOSSlider(
                        value = volume,
                        onValueChange = { viewModel.setVolume(it) },
                        icon = Icons.Rounded.VolumeUp,
                        modifier = Modifier.weight(0.5f).fillMaxHeight()
                    )
                }

                // Row 3: Quick Action Tiles (Flashlight, Calculator, Camera, Root Tweaks)
                Row(
                    modifier = Modifier.fillMaxWidth().height(72.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Flashlight
                    ControlSquareButton(
                        icon = Icons.Rounded.FlashlightOn,
                        isActive = flashlightEnabled,
                        activeColor = IOSYellow,
                        label = "Flashlight",
                        onClick = { viewModel.toggleFlashlight(context) },
                        modifier = Modifier.weight(1f)
                    )
                    // Calculator
                    ControlSquareButton(
                        icon = Icons.Rounded.Calculate,
                        isActive = false,
                        activeColor = IOSOrange,
                        label = "Calculator",
                        onClick = {
                            viewModel.openApp(AppId.CALCULATOR)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    // Camera
                    ControlSquareButton(
                        icon = Icons.Rounded.CameraAlt,
                        isActive = false,
                        activeColor = IOSSystemGray,
                        label = "Camera",
                        onClick = {
                            viewModel.openApp(AppId.CAMERA)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                    // Root / Superuser Tweak shortcut
                    ControlSquareButton(
                        icon = Icons.Rounded.Security,
                        isActive = true,
                        activeColor = IOSPurple,
                        label = "iRoot SE",
                        onClick = {
                            viewModel.openApp(AppId.ROOT_TOOLS)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Root Quick Action Bar (Ultra Immersive & RAM Cleaner for J2 Prime)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0x50330066))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = IOSMint,
                            modifier = Modifier.size(20.dp)
                        )
                        Column {
                            Text(
                                text = "J2 Prime Root Optimizer",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Drop RAM Cache & Boost CPU",
                                color = Color(0xFFC7C7CC),
                                fontSize = 10.sp
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.cleanJ2PrimeRamRoot()
                            viewModel.boostCpuRoot()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IOSMint),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Text("Boost Now", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ControlCircleIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(54.dp)
            .clip(CircleShape)
            .background(if (isActive) activeColor else Color(0x33FFFFFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
fun ControlSquareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    activeColor: Color,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(20.dp))
            .background(if (isActive) activeColor else Color(0x40FFFFFF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isActive && activeColor == IOSYellow) Color.Black else Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
fun VerticalIOSSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    var dragProgress by remember(value) { mutableStateOf(value) }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0x40FFFFFF))
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    // Drag up increases, drag down decreases
                    val change = -delta / 250f
                    val newProgress = (dragProgress + change).coerceIn(0f, 1f)
                    dragProgress = newProgress
                    onValueChange(newProgress)
                }
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Filled portion
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(dragProgress)
                .background(Color.White)
        )

        // Centered / Bottom icon
        Box(
            modifier = Modifier
                .padding(bottom = 16.dp)
                .size(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (dragProgress > 0.2f) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
