package com.example.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppId
import com.example.viewmodel.IOSViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SiriOverlay(
    viewModel: IOSViewModel,
    isOpen: Boolean,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isFlashlightOn by viewModel.flashlightEnabled.collectAsState()
    val isMusicPlaying by viewModel.isMusicPlaying.collectAsState()

    var userQuery by remember { mutableStateOf("") }
    var siriResponse by remember { mutableStateOf("Ready. What can I do for you?") }
    var isThinking by remember { mutableStateOf(false) }

    // Apple Intelligence iOS 18 Edge Glow Pulse
    val infiniteTransition = rememberInfiniteTransition(label = "siriGlow")
    val glowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowPhase"
    )

    val orbPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    )

    // Execute Siri Intent
    fun handleCommand(input: String) {
        val lower = input.lowercase().trim()
        isThinking = true
        coroutineScope.launch {
            delay(400)
            isThinking = false
            when {
                lower.contains("senter") || lower.contains("flashlight") || lower.contains("lampu") -> {
                    viewModel.toggleFlashlight(context)
                    siriResponse = if (!isFlashlightOn) "Turned flashlight on." else "Turned flashlight off."
                }
                lower.contains("musik") || lower.contains("music") || lower.contains("lagu") || lower.contains("play") -> {
                    viewModel.toggleMusicPlay()
                    siriResponse = if (!isMusicPlaying) "Playing your music." else "Paused music playback."
                }
                lower.contains("baterai") || lower.contains("battery") -> {
                    siriResponse = "Your battery is at $batteryLevel%."
                }
                lower.contains("kamera") || lower.contains("camera") || lower.contains("foto") -> {
                    siriResponse = "Opening Camera..."
                    delay(500)
                    viewModel.openApp(AppId.CAMERA)
                    onDismiss()
                }
                lower.contains("telepon") || lower.contains("call") || lower.contains("phone") -> {
                    siriResponse = "Opening Phone..."
                    delay(500)
                    viewModel.openApp(AppId.PHONE)
                    onDismiss()
                }
                lower.contains("cuaca") || lower.contains("weather") -> {
                    siriResponse = "In Jakarta it's 29°C and mostly sunny."
                }
                lower.contains("halo") || lower.contains("hai") || lower.contains("hello") || lower.contains("hi") -> {
                    siriResponse = "Hello! How can I help you today?"
                }
                else -> {
                    siriResponse = "Found info for \"$input\". Siri is powered by Apple Intelligence."
                }
            }
        }
    }

    val quickPrompts = remember {
        listOf(
            "🔦 Flashlight",
            "🎵 Play Music",
            "🔋 Battery Status",
            "📷 Open Camera",
            "☀️ Weather",
            "📞 Call"
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
    ) {
        // Glowing Rainbow Apple Intelligence Screen Border (iOS 18 signature)
        val edgeGlowBrush = Brush.sweepGradient(
            colors = listOf(
                Color(0xFFFF2D55),
                Color(0xFFAF52DE),
                Color(0xFF5856D6),
                Color(0xFF007AFF),
                Color(0xFF34C759),
                Color(0xFFFF9500),
                Color(0xFFFF2D55)
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    width = 4.dp,
                    brush = edgeGlowBrush,
                    shape = RoundedCornerShape(38.dp)
                )
        )

        // Main Siri Card at the Bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 28.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* keep clicks from dismissing */ }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Siri Response Speech Bubble
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color.White.copy(alpha = 0.14f))
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = "Apple Intelligence",
                        tint = Color(0xFFAF52DE),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = if (isThinking) "Thinking..." else siriResponse,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Quick Prompt Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(quickPrompts) { prompt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .clickable {
                                userQuery = prompt
                                handleCommand(prompt)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = prompt,
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Interactive Text Query Input
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = userQuery,
                    onValueChange = { userQuery = it },
                    placeholder = {
                        Text(
                            text = "Ask Siri or Apple Intelligence...",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = {
                        if (userQuery.isNotBlank()) {
                            keyboardController?.hide()
                            handleCommand(userQuery)
                            userQuery = ""
                        }
                    })
                )

                IconButton(
                    onClick = {
                        if (userQuery.isNotBlank()) {
                            keyboardController?.hide()
                            handleCommand(userQuery)
                            userQuery = ""
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowUpward,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF007AFF))
                            .padding(4.dp)
                    )
                }
            }

            // Pulsing Apple Siri Orb
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .scale(orbPulse)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFF2D55),
                                Color(0xFFAF52DE),
                                Color(0xFF007AFF),
                                Color.Transparent
                            )
                        )
                    )
                    .clickable {
                        siriResponse = "Listening... tap a prompt or type above."
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.85f))
                )
            }
        }
    }
}
