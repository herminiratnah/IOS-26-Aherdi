package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.util.IOSFeedbackHelper
import com.example.viewmodel.IOSViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class AirDropTarget(
    val id: String,
    val name: String,
    val deviceType: String, // "iPhone", "Mac", "iPad"
    val icon: ImageVector,
    val avatarColor: Long
)

@Composable
fun IOSShareSheet(
    viewModel: IOSViewModel,
    isOpen: Boolean,
    title: String = "Item",
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sendingTargetId by remember { mutableStateOf<String?>(null) }
    var sentTargetId by remember { mutableStateOf<String?>(null) }

    val airdropTargets = remember {
        listOf(
            AirDropTarget("1", "iPhone Hermin", "iPhone", Icons.Rounded.PhoneIphone, 0xFF007AFF),
            AirDropTarget("2", "MacBook Pro", "Mac", Icons.Rounded.LaptopMac, 0xFF5856D6),
            AirDropTarget("3", "iPad Air", "iPad", Icons.Rounded.TabletMac, 0xFFFF9500),
            AirDropTarget("4", "Semua Orang", "AirDrop", Icons.Rounded.Airplay, 0xFF34C759)
        )
    }

    val quickApps = remember {
        listOf(
            Triple("WhatsApp", Icons.Rounded.Chat, Color(0xFF25D366)),
            Triple("Instagram", Icons.Rounded.CameraAlt, Color(0xFFE1306C)),
            Triple("Pesan", Icons.Rounded.Message, IOSGreen),
            Triple("Salin", Icons.Rounded.ContentCopy, Color(0xFF8E8E93)),
            Triple("Bluetooth", Icons.Rounded.Bluetooth, Color(0xFF007AFF))
        )
    }

    // Pulse animation for radar
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                ),
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFFF2F2F7))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {} // prevent dismiss
                    )
                    .padding(20.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Grabber handle
                    Box(
                        modifier = Modifier
                            .size(36.dp, 4.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC7C7CC))
                            .align(Alignment.CenterHorizontally)
                    )

                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("AirDrop & Bagikan", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                            Text(title, fontSize = 13.sp, color = Color.Gray)
                        }
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE5E5EA))
                        ) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }

                    // AirDrop Section
                    Text("AIRDROP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(airdropTargets) { target ->
                            val isSending = sendingTargetId == target.id
                            val isSent = sentTargetId == target.id

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(72.dp)
                                    .clickable {
                                        IOSFeedbackHelper.vibrateMedium(context)
                                        sendingTargetId = target.id
                                        coroutineScope.launch {
                                            delay(1500)
                                            sendingTargetId = null
                                            sentTargetId = target.id
                                            viewModel.showDynamicIslandNotification(
                                                "AirDrop Berhasil",
                                                "Terkirim ke ${target.name}",
                                                "airdrop",
                                                target.avatarColor
                                            )
                                            delay(1200)
                                            sentTargetId = null
                                            onDismiss()
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .scale(if (isSending) pulseScale else 1f)
                                        .clip(CircleShape)
                                        .background(
                                            if (isSent) IOSGreen else Color(target.avatarColor)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isSent) Icons.Rounded.Check else target.icon,
                                        contentDescription = target.name,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = when {
                                        isSent -> "Terkirim!"
                                        isSending -> "Mengirim..."
                                        else -> target.name
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = if (isSent || isSending) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSent) IOSGreen else Color.Black,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)

                    // Quick App Row
                    Text("APLIKASI", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(quickApps) { app ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(64.dp)
                                    .clickable {
                                        IOSFeedbackHelper.vibrateLight(context)
                                        if (app.first == "Salin") {
                                            viewModel.showDynamicIslandNotification("Disalin", "Tautan disalin ke papan klip", "copy")
                                            onDismiss()
                                        } else {
                                            // Fallback to standard share
                                            val sendIntent = Intent().apply {
                                                action = Intent.ACTION_SEND
                                                putExtra(Intent.EXTRA_TEXT, "Shared from iOS Launcher: $title")
                                                type = "text/plain"
                                            }
                                            context.startActivity(Intent.createChooser(sendIntent, "Bagikan via ${app.first}"))
                                            onDismiss()
                                        }
                                    }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(50.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(app.third),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(app.second, contentDescription = app.first, tint = Color.White, modifier = Modifier.size(26.dp))
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(app.first, fontSize = 11.sp, color = Color.Black, textAlign = TextAlign.Center)
                            }
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE5E5EA), thickness = 0.5.dp)

                    // System Share Fallback Row
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable {
                                val sendIntent = Intent().apply {
                                    action = Intent.ACTION_SEND
                                    putExtra(Intent.EXTRA_TEXT, "Shared from iOS Launcher: $title")
                                    type = "text/plain"
                                }
                                context.startActivity(Intent.createChooser(sendIntent, "Pilihan Lainnya"))
                                onDismiss()
                            }
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Pilihan Lainnya (Android System Share)", fontSize = 15.sp, color = IOSBlue, fontWeight = FontWeight.SemiBold)
                            Icon(Icons.Rounded.Share, contentDescription = null, tint = IOSBlue, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}
