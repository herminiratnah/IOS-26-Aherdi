package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppId
import com.example.model.AppItem
import com.example.ui.theme.*
import com.example.util.IOSFeedbackHelper
import com.example.viewmodel.IOSViewModel
import kotlin.math.roundToInt

@Composable
fun IOSAppSwitcher(
    viewModel: IOSViewModel,
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    val recentApps by viewModel.recentApps.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()

    AnimatedVisibility(
        visible = isOpen,
        enter = fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.92f, animationSpec = tween(280)),
        exit = fadeOut(animationSpec = tween(240)) + scaleOut(targetScale = 0.95f, animationSpec = tween(240))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.65f))
                .clickable { onDismiss() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Pengalih Aplikasi",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )

                    if (recentApps.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.White.copy(alpha = 0.2f))
                                .clickable {
                                    IOSFeedbackHelper.vibrateMedium(context)
                                    viewModel.clearAllRecentApps()
                                }
                                .padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Tutup Semua",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Card Carousel
                if (recentApps.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Rounded.LayersClear,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.4f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Tidak Ada Aplikasi Terbuka",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyRow(
                        state = listState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 40.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        items(recentApps, key = { it.name }) { appId ->
                            AppSwitcherCard(
                                appId = appId,
                                onOpen = {
                                    IOSFeedbackHelper.vibrateLight(context)
                                    viewModel.openApp(appId)
                                },
                                onDismissCard = {
                                    IOSFeedbackHelper.vibrateLight(context)
                                    viewModel.dismissRecentApp(appId)
                                }
                            )
                        }
                    }
                }

                // Home Indicator Bar at the bottom
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .padding(bottom = 12.dp)
                        .width(135.dp)
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.White.copy(alpha = 0.7f))
                        .clickable { onDismiss() }
                )
            }
        }
    }
}

@Composable
fun AppSwitcherCard(
    appId: AppId,
    onOpen: () -> Unit,
    onDismissCard: () -> Unit
) {
    var offsetY by remember { mutableFloatStateOf(0f) }
    val animatedAlpha by animateFloatAsState(
        targetValue = if (offsetY < -150f) 0f else 1f,
        label = "cardAlpha"
    )

    val appInfo = remember(appId) { getAppMeta(appId) }

    Box(
        modifier = Modifier
            .width(260.dp)
            .height(490.dp)
            .offset { IntOffset(0, offsetY.roundToInt()) }
            .graphicsLayer {
                alpha = animatedAlpha
                scaleX = if (offsetY < -100f) 0.85f else 1f
                scaleY = if (offsetY < -100f) 0.85f else 1f
            }
            .draggable(
                orientation = Orientation.Vertical,
                state = rememberDraggableState { delta ->
                    // Allow swiping up to dismiss
                    if (offsetY + delta <= 0f) {
                        offsetY += delta
                    }
                },
                onDragStopped = {
                    if (offsetY < -200f) {
                        onDismissCard()
                    } else {
                        offsetY = 0f
                    }
                }
            )
            .clickable { onOpen() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // App Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(appInfo.color),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = appInfo.icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = appInfo.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable { onDismissCard() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Snapshot Preview Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .shadow(16.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF1C1C1E))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            ) {
                AppSnapshotContent(appId = appId, appInfo = appInfo)
            }
        }
    }
}

@Composable
fun AppSnapshotContent(appId: AppId, appInfo: AppMeta) {
    when (appId) {
        AppId.SAFARI -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF2F2F7))
                    .padding(12.dp)
            ) {
                // Address bar preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text("apple.com", fontSize = 12.sp, color = Color.Gray)
                }
                Spacer(modifier = Modifier.height(12.dp))
                // Web Page mockup
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(14.dp)
                ) {
                    Column {
                        Text("Apple Experience", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("iPhone 16 Pro • Titanium Desain Kuat & Ringan", fontSize = 12.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF007AFF), Color(0xFF5856D6))))
                        )
                    }
                }
            }
        }
        AppId.PHOTOS -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(8.dp)
            ) {
                Text("Semua Foto", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(4.dp))
                Spacer(modifier = Modifier.height(6.dp))
                // Photo grid preview
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF34C759)))
                    Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF9500)))
                    Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF007AFF)))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFAF52DE)))
                    Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFFFF2D55)))
                    Box(modifier = Modifier.weight(1f).height(100.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF5856D6)))
                }
            }
        }
        AppId.CALCULATOR -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .padding(14.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text("2,048", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Light, modifier = Modifier.align(Alignment.End))
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFA5A5A5)))
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFA5A5A5)))
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFFA5A5A5)))
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(IOSOrange))
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF333333)))
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF333333)))
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF333333)))
                    Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(IOSOrange))
                }
            }
        }
        AppId.MUSIC -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(Color(0xFFE040FB), Color(0xFF1E1035))))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(54.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Now Playing", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Apple Music Live", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }
        AppId.CAMERA -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .border(1.5.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.PhotoCamera, contentDescription = null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(44.dp))
                }
            }
        }
        AppId.SETTINGS -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF2F2F7))
                    .padding(14.dp)
            ) {
                Text("Pengaturan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(Color(0xFF007AFF)))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Apple Account", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text("iCloud+, Media & Pembelian", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                )
            }
        }
        else -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF1C1C1E)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(appInfo.icon, contentDescription = null, tint = appInfo.color, modifier = Modifier.size(50.dp))
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(appInfo.title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

data class AppMeta(val title: String, val color: Color, val icon: androidx.compose.ui.graphics.vector.ImageVector)

fun getAppMeta(appId: AppId): AppMeta {
    return when (appId) {
        AppId.SAFARI -> AppMeta("Safari", Color(0xFF007AFF), Icons.Rounded.Explore)
        AppId.PHOTOS -> AppMeta("Foto", Color(0xFFFF9500), Icons.Rounded.PhotoLibrary)
        AppId.CAMERA -> AppMeta("Kamera", Color(0xFF8E8E93), Icons.Rounded.PhotoCamera)
        AppId.CALCULATOR -> AppMeta("Kalkulator", IOSOrange, Icons.Rounded.Calculate)
        AppId.MUSIC -> AppMeta("Musik", Color(0xFFFA2D48), Icons.Rounded.MusicNote)
        AppId.SETTINGS -> AppMeta("Pengaturan", Color(0xFF8E8E93), Icons.Rounded.Settings)
        AppId.NOTES -> AppMeta("Catatan", IOSYellow, Icons.Rounded.EditNote)
        AppId.MESSAGES -> AppMeta("Pesan", Color(0xFF34C759), Icons.Rounded.Chat)
        AppId.PHONE -> AppMeta("Telepon", Color(0xFF34C759), Icons.Rounded.Phone)
        AppId.MAIL -> AppMeta("Mail", Color(0xFF007AFF), Icons.Rounded.Mail)
        AppId.CLOCK -> AppMeta("Jam", Color.Black, Icons.Rounded.AccessTime)
        AppId.WEATHER -> AppMeta("Cuaca", Color(0xFF007AFF), Icons.Rounded.WbSunny)
        AppId.MAPS -> AppMeta("Peta", Color(0xFF34C759), Icons.Rounded.Map)
        AppId.HEALTH -> AppMeta("Kesehatan", Color(0xFFFF2D55), Icons.Rounded.Favorite)
        AppId.WALLET -> AppMeta("Dompet", Color.Black, Icons.Rounded.AccountBalanceWallet)
        AppId.PODCASTS -> AppMeta("Podcast", Color(0xFF8944AB), Icons.Rounded.Podcasts)
        AppId.APP_STORE -> AppMeta("App Store", Color(0xFF007AFF), Icons.Rounded.ShoppingBag)
        AppId.REMINDERS -> AppMeta("Pengingat", Color(0xFFFF9500), Icons.Rounded.FormatListBulleted)
        AppId.CALENDAR -> AppMeta("Kalender", Color(0xFFFF3B30), Icons.Rounded.CalendarToday)
        AppId.ROOT_TOOLS -> AppMeta("Root Tools", Color(0xFFFF3B30), Icons.Rounded.Build)
        else -> AppMeta("Aplikasi", Color(0xFF8E8E93), Icons.Rounded.Apps)
    }
}
