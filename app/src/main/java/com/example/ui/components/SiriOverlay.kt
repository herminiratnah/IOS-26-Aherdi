package com.example.ui.components

import android.content.Context
import android.os.Build
import android.speech.tts.TextToSpeech
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.IOSPersistenceManager
import com.example.model.AppId
import com.example.util.SiriAiEngine
import com.example.viewmodel.IOSViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

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
    val focusManager = LocalFocusManager.current

    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isFlashlightOn by viewModel.flashlightEnabled.collectAsState()
    val isMusicPlaying by viewModel.isMusicPlaying.collectAsState()

    var userQuery by remember { mutableStateOf("") }
    var siriResponse by remember { mutableStateOf("Halo! Ada yang bisa Siri bantu hari ini?") }
    var isThinking by remember { mutableStateOf(false) }
    var currentPendingQuery by remember { mutableStateOf("") }
    var chatHistory by remember { mutableStateOf<List<SiriAiEngine.SiriMessage>>(emptyList()) }
    var showQuickApiKeyDialog by remember { mutableStateOf(false) }
    var quickApiKeyInput by remember { mutableStateOf("") }
    var isExpandedView by remember { mutableStateOf(false) }

    val isGeminiConfigured = remember(isOpen) {
        SiriAiEngine.isGeminiConfigured(context)
    }

    // Android Text-to-Speech Engine
    var textToSpeech by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsMuted by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        var ttsInstance: TextToSpeech? = null
        ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val localeId = Locale("id", "ID")
                val res = ttsInstance?.setLanguage(localeId)
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    ttsInstance?.language = Locale.ENGLISH
                }
            }
        }
        textToSpeech = ttsInstance
        onDispose {
            ttsInstance?.stop()
            ttsInstance?.shutdown()
        }
    }

    fun speakSiriResponse(text: String) {
        if (!isTtsMuted && textToSpeech != null) {
            val maxLen = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                TextToSpeech.getMaxSpeechInputLength().coerceAtMost(3500)
            } else {
                3500
            }
            if (text.length <= maxLen) {
                textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "siri_speech_0")
            } else {
                val chunks = text.chunked(maxLen)
                textToSpeech?.speak(chunks[0], TextToSpeech.QUEUE_FLUSH, null, "siri_speech_0")
                for (i in 1 until chunks.size) {
                    textToSpeech?.speak(chunks[i], TextToSpeech.QUEUE_ADD, null, "siri_speech_$i")
                }
            }
        }
    }

    // Apple Intelligence iOS 18 Edge Glow Pulse
    val infiniteTransition = rememberInfiniteTransition(label = "siriGlow")
    val glowPhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "glowPhase"
    )

    val orbPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    )

    // Execute Siri Intent & AI Engine
    fun handleCommand(input: String) {
        val lower = input.lowercase().trim()
        isThinking = true
        currentPendingQuery = input
        coroutineScope.launch {
            when {
                lower.contains("senter") || lower.contains("flashlight") || lower.contains("lampu") -> {
                    delay(250)
                    viewModel.toggleFlashlight(context)
                    isThinking = false
                    val reply = if (!isFlashlightOn) "Lampu senter dinyalakan." else "Lampu senter dimatikan."
                    siriResponse = reply
                    val updated = chatHistory.toMutableList().apply {
                        add(SiriAiEngine.SiriMessage(role = "user", content = input))
                        add(SiriAiEngine.SiriMessage(role = "model", content = reply))
                    }
                    chatHistory = updated.takeLast(10)
                    speakSiriResponse(reply)
                }
                lower.contains("musik") || lower.contains("music") || lower.contains("lagu") || lower.contains("play") -> {
                    delay(250)
                    viewModel.toggleMusicPlay()
                    isThinking = false
                    val reply = if (!isMusicPlaying) "Memutar musik kamu." else "Musik dijeda."
                    siriResponse = reply
                    val updated = chatHistory.toMutableList().apply {
                        add(SiriAiEngine.SiriMessage(role = "user", content = input))
                        add(SiriAiEngine.SiriMessage(role = "model", content = reply))
                    }
                    chatHistory = updated.takeLast(10)
                    speakSiriResponse(reply)
                }
                lower.contains("baterai") || lower.contains("battery") -> {
                    delay(200)
                    isThinking = false
                    val reply = "Baterai perangkatmu saat ini berada di level $batteryLevel%."
                    siriResponse = reply
                    val updated = chatHistory.toMutableList().apply {
                        add(SiriAiEngine.SiriMessage(role = "user", content = input))
                        add(SiriAiEngine.SiriMessage(role = "model", content = reply))
                    }
                    chatHistory = updated.takeLast(10)
                    speakSiriResponse(reply)
                }
                lower.contains("kamera") || lower.contains("camera") || lower.contains("foto") -> {
                    val reply = "Membuka Kamera..."
                    siriResponse = reply
                    speakSiriResponse(reply)
                    delay(400)
                    isThinking = false
                    viewModel.openApp(AppId.CAMERA)
                    onDismiss()
                }
                lower.contains("ram") || lower.contains("boost") || lower.contains("bersihkan") -> {
                    viewModel.cleanJ2PrimeRamRoot()
                    delay(300)
                    isThinking = false
                    val reply = "RAM berhasil dioptimalkan dan cache telah dibersihkan."
                    siriResponse = reply
                    val updated = chatHistory.toMutableList().apply {
                        add(SiriAiEngine.SiriMessage(role = "user", content = input))
                        add(SiriAiEngine.SiriMessage(role = "model", content = reply))
                    }
                    chatHistory = updated.takeLast(10)
                    speakSiriResponse(reply)
                }
                else -> {
                    // Call Gemini AI with real-time streaming (SSE) and full multi-turn conversation memory!
                    val currentHistory = chatHistory
                    val accumulatedText = StringBuilder()
                    var hasReceivedFirstChunk = false
                    val response = SiriAiEngine.queryAiStreaming(
                        prompt = input,
                        history = currentHistory,
                        context = context
                    ) { chunk ->
                        if (!hasReceivedFirstChunk) {
                            hasReceivedFirstChunk = true
                            isThinking = false
                            siriResponse = ""
                        }
                        accumulatedText.append(chunk)
                        siriResponse = accumulatedText.toString()
                    }
                    isThinking = false
                    siriResponse = response
                    val updated = currentHistory.toMutableList().apply {
                        add(SiriAiEngine.SiriMessage(role = "user", content = input))
                        add(SiriAiEngine.SiriMessage(role = "model", content = response))
                    }
                    chatHistory = updated.takeLast(10)
                    speakSiriResponse(response)
                }
            }
        }
    }

    val quickPrompts = remember {
        listOf(
            "☀️ Hari ini hari apa?",
            "🗓️ Tanggal berapa sekarang?",
            "💡 Beri kata motivasi",
            "✨ Siapa kamu?",
            "🔦 Nyalakan Senter",
            "🎵 Putar Musik",
            "🔋 Cek Baterai",
            "⚡ Bersihkan RAM",
            "📷 Buka Kamera"
        )
    }

    fun dismissGracefully() {
        focusManager.clearFocus()
        keyboardController?.hide()
        textToSpeech?.stop()
        onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { dismissGracefully() }
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
                    width = if (isThinking) 4.5.dp else 3.dp,
                    brush = edgeGlowBrush,
                    shape = RoundedCornerShape(38.dp)
                )
        )

        // Main Siri Card at the Bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp)
                .imePadding()
                .navigationBarsPadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Keep inside clicks from dismissing */ }
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Status Header Pill: Gemini AI Active or Setup API Key
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // AI Status Badge
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0x35FFFFFF))
                        .clickable {
                            quickApiKeyInput = IOSPersistenceManager.getGeminiApiKey(context)
                            showQuickApiKeyDialog = true
                        }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = if (isGeminiConfigured) Color(0xFF34C759) else Color(0xFFFF9500),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isGeminiConfigured) "Gemini AI Online (${IOSPersistenceManager.getGeminiModel(context).replace("gemini-", "").replace("-flash", " Flash")})" else "Atur Gemini API Key",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // TTS Mute / Unmute Button
                IconButton(
                    onClick = {
                        isTtsMuted = !isTtsMuted
                        if (isTtsMuted) textToSpeech?.stop()
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isTtsMuted) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                        contentDescription = "Suara Siri",
                        tint = if (isTtsMuted) Color.White.copy(alpha = 0.5f) else Color(0xFF007AFF),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Siri Response Speech Bubble (Flexible, scrollable multi-turn chat, with copy & expand buttons)
            val responseScrollState = rememberScrollState()

            LaunchedEffect(chatHistory.size, siriResponse, isThinking) {
                if (responseScrollState.maxValue > 0) {
                    responseScrollState.animateScrollTo(responseScrollState.maxValue)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x33FFFFFF))
                    .border(1.dp, Color.White.copy(alpha = 0.22f), RoundedCornerShape(22.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top control bar inside bubble: Siri icon, title, copy, expand, reset topic
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Apple Intelligence",
                            tint = Color(0xFFAF52DE),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (chatHistory.isNotEmpty()) "OBROLAN AKTIF (${chatHistory.size / 2})" else "APPLE INTELLIGENCE",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (chatHistory.isNotEmpty()) {
                            // Reset conversation button
                            IconButton(
                                onClick = {
                                    chatHistory = emptyList()
                                    siriResponse = "Topik baru dimulai. Ada yang ingin kamu tanyakan ke Siri?"
                                    viewModel.showDynamicIslandNotification("Siri", "Topik baru dimulai", "sparkles", 0xFF007AFF)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Mulai Topik Baru",
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        if (!isThinking && (siriResponse.isNotBlank() || chatHistory.isNotEmpty())) {
                            // Copy button
                            IconButton(
                                onClick = {
                                    val textToCopy = if (chatHistory.isNotEmpty()) {
                                        chatHistory.joinToString("\n\n") { msg ->
                                            "${if (msg.role == "user") "Anda" else "Siri"}: ${msg.content}"
                                        }
                                    } else {
                                        siriResponse
                                    }
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val clip = android.content.ClipData.newPlainText("Siri", textToCopy)
                                    clipboard.setPrimaryClip(clip)
                                    viewModel.showDynamicIslandNotification("Siri", "Teks disalin ke papan klip", "sparkles", 0xFF007AFF)
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Salin",
                                    tint = Color.White.copy(alpha = 0.8f),
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            // Full screen expand button for long answers
                            IconButton(
                                onClick = { isExpandedView = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.OpenInFull,
                                    contentDescription = "Layar Penuh",
                                    tint = Color(0xFF007AFF),
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 45.dp, max = 260.dp)
                        .verticalScroll(responseScrollState)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (chatHistory.isEmpty()) {
                            if (isThinking) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFFAF52DE)
                                    )
                                    Text(
                                        text = "Siri sedang berpikir...",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 14.sp
                                    )
                                }
                            } else {
                                Text(
                                    text = siriResponse,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    lineHeight = 22.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        } else {
                            // Render multi-turn conversation thread
                            for (msg in chatHistory) {
                                if (msg.role == "user") {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFF007AFF).copy(alpha = 0.5f))
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(
                                                text = msg.content,
                                                color = Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                } else {
                                    Text(
                                        text = msg.content,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        lineHeight = 21.sp,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }

                            // If currently waiting for follow-up response
                            if (isThinking) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color(0xFF007AFF).copy(alpha = 0.5f))
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = currentPendingQuery,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFFAF52DE)
                                    )
                                    Text(
                                        text = "Siri sedang berpikir...",
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }

                if (responseScrollState.maxValue > 0 && !isThinking) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "↓ Gulir ke bawah atau ketuk [⤢] untuk baca tuntas",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 10.sp
                        )
                    }
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
                            .background(Color.White.copy(alpha = 0.14f))
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
                    .background(Color.White.copy(alpha = 0.18f))
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = userQuery,
                    onValueChange = { userQuery = it },
                    placeholder = {
                        Text(
                            text = "Tanya Siri atau ajak ngobrol bebas...",
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
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            handleCommand(userQuery)
                            userQuery = ""
                        }
                    })
                )

                IconButton(
                    onClick = {
                        if (userQuery.isNotBlank()) {
                            focusManager.clearFocus()
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
                    .scale(if (isThinking) orbPulse * 1.08f else orbPulse)
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
                        siriResponse = "Silakan ketik pertanyaan atau ketuk salah satu saran cepat di atas."
                        speakSiriResponse(siriResponse)
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.88f))
                )
            }
        }
    }

    // Quick Dialog to Enter Gemini API Key directly inside Siri
    if (showQuickApiKeyDialog) {
        AlertDialog(
            onDismissRequest = { showQuickApiKeyDialog = false },
            containerColor = Color(0xFA1C1C1E),
            shape = RoundedCornerShape(22.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.VpnKey, contentDescription = null, tint = Color(0xFF007AFF))
                    Text("Google Gemini API Key", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Masukkan API Key gratis dari aistudio.google.com/apikey untuk mengaktifkan obrolan bebas tanpa batas.",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = quickApiKeyInput,
                        onValueChange = { quickApiKeyInput = it.trim() },
                        placeholder = { Text("Tempel API Key AI Studio di sini...", color = Color.Gray, fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF007AFF),
                            unfocusedBorderColor = Color(0xFF38383A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        IOSPersistenceManager.saveGeminiApiKey(context, quickApiKeyInput)
                        showQuickApiKeyDialog = false
                        viewModel.showDynamicIslandNotification("Siri", "API Key disimpan!", "sparkles", 0xFF34C759)
                    }
                ) {
                    Text("Simpan", color = Color(0xFF007AFF), fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickApiKeyDialog = false }) {
                    Text("Batal", color = Color.White.copy(alpha = 0.6f), fontSize = 15.sp)
                }
            }
        )
    }

    // Full Screen Expanded Reader for Long Responses
    if (isExpandedView) {
        Dialog(
            onDismissRequest = { isExpandedView = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.88f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1C1E))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color(0xFFAF52DE),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (chatHistory.isNotEmpty()) "Riwayat Obrolan Siri" else "Jawaban Lengkap Siri",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = { speakSiriResponse(siriResponse) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.VolumeUp,
                                        contentDescription = "Dengarkan",
                                        tint = Color(0xFF007AFF)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val textToCopy = if (chatHistory.isNotEmpty()) {
                                            chatHistory.joinToString("\n\n") { msg ->
                                                "${if (msg.role == "user") "Anda" else "Siri"}: ${msg.content}"
                                            }
                                        } else {
                                            siriResponse
                                        }
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("Siri", textToCopy)
                                        clipboard.setPrimaryClip(clip)
                                        viewModel.showDynamicIslandNotification("Siri", "Teks disalin ke papan klip", "sparkles", 0xFF007AFF)
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Salin",
                                        tint = Color.White.copy(alpha = 0.8f)
                                    )
                                }
                                IconButton(onClick = { isExpandedView = false }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Close,
                                        contentDescription = "Tutup",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 12.dp),
                            color = Color(0xFF2C2C2E)
                        )

                        val fullTextScrollState = rememberScrollState()
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .verticalScroll(fullTextScrollState)
                        ) {
                            val displayText = if (chatHistory.isNotEmpty()) {
                                buildString {
                                    chatHistory.forEach { msg ->
                                        if (msg.role == "user") {
                                            append("👤 Anda:\n${msg.content}\n\n")
                                        } else {
                                            append("✨ Siri:\n${msg.content}\n\n─────────────────────\n\n")
                                        }
                                    }
                                }.trim()
                            } else {
                                siriResponse
                            }
                            Text(
                                text = displayText,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 15.sp,
                                lineHeight = 23.sp,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}
