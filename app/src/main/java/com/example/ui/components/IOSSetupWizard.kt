package com.example.ui.components

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import com.example.util.IOSFeedbackHelper
import com.example.viewmodel.IOSViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IOSSetupWizard(
    viewModel: IOSViewModel,
    isOpen: Boolean,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isOpen) return

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 10 Steps matching iOS 26 Setup Flow:
    // 0: Hello, 1: Country/Region, 2: Appearance, 3: Quick Start, 4: Wi-Fi,
    // 5: Data & Privacy, 6: Face ID, 7: Passcode, 8: Transfer Data, 9: Welcome to iPhone
    var currentStep by remember { mutableIntStateOf(0) }

    // Multi-language "Hello" ticker
    val greetings = remember {
        listOf("Hello", "Halo", "Hola", "Bonjour", "Ciao", "Olá", "Guten Tag", "こんにちは", "你好")
    }
    var greetingIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(currentStep) {
        if (currentStep == 0) {
            while (true) {
                delay(2200)
                greetingIndex = (greetingIndex + 1) % greetings.size
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                if (targetState > initialState) {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn())
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                } else {
                    (slideInHorizontally(initialOffsetX = { -it }) + fadeIn())
                        .togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                }
            },
            label = "SetupStep"
        ) { step ->
            when (step) {
                // ==========================================
                // STEP 0: HELLO SCREEN (Authentic iOS 26)
                // ==========================================
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Animated Gradient Greeting Text
                            AnimatedContent(
                                targetState = greetings[greetingIndex],
                                transitionSpec = {
                                    (fadeIn(tween(450)) + slideInVertically(initialOffsetY = { 24 }))
                                        .togetherWith(fadeOut(tween(350)) + slideOutVertically(targetOffsetY = { -24 }))
                                },
                                label = "GreetingAnimation"
                            ) { text ->
                                Text(
                                    text = text,
                                    color = Color.White,
                                    fontSize = 54.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-1.5).sp,
                                    textAlign = TextAlign.Center
                                )
                            }

                            Text(
                                text = "Welcome to iPhone\niOS 26 Experience",
                                color = Color(0xFF8E8E93),
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 24.sp
                            )
                        }

                        // Bottom swipe up / button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 20.dp)
                                .pointerInput(Unit) {
                                    detectVerticalDragGestures { _, dragAmount ->
                                        if (dragAmount < -25f) {
                                            IOSFeedbackHelper.vibrateLight(context)
                                            currentStep = 1
                                        }
                                    }
                                }
                        ) {
                            Button(
                                onClick = {
                                    IOSFeedbackHelper.vibrateLight(context)
                                    currentStep = 1
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "Continue",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Swipe up to configure",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // ==========================================
                // STEP 1: SELECT COUNTRY OR REGION
                // ==========================================
                1 -> {
                    val userCountry by viewModel.userCountry.collectAsState()
                    var searchQuery by remember { mutableStateOf("") }
                    val countries = remember {
                        listOf(
                            "Indonesia" to "🇮🇩",
                            "United States" to "🇺🇸",
                            "United Kingdom" to "🇬🇧",
                            "Japan" to "🇯🇵",
                            "Singapore" to "🇸🇬",
                            "Germany" to "🇩🇪",
                            "Australia" to "🇦🇺",
                            "Malaysia" to "🇲🇾",
                            "France" to "🇫🇷",
                            "South Korea" to "🇰🇷",
                            "Canada" to "🇨🇦",
                            "Netherlands" to "🇳🇱",
                            "Brazil" to "🇧🇷",
                            "Italy" to "🇮🇹",
                            "Saudi Arabia" to "🇸🇦"
                        )
                    }
                    val filtered = countries.filter {
                        it.first.contains(searchQuery, ignoreCase = true)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        SetupTopBar(onBack = { currentStep = 0 })

                        Text(
                            text = "Select Country or Region",
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 10.dp, bottom = 12.dp)
                        )

                        // Search Bar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp))
                                BasicTextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
                                    cursorBrush = SolidColor(IOSBlue),
                                    modifier = Modifier.weight(1f),
                                    decorationBox = { innerTextField ->
                                        if (searchQuery.isEmpty()) {
                                            Text("Search", color = Color.Gray, fontSize = 15.sp)
                                        }
                                        innerTextField()
                                    }
                                )
                                if (searchQuery.isNotEmpty()) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = null,
                                        tint = Color.Gray,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { searchQuery = "" }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Country List
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFF1C1C1E)),
                            verticalArrangement = Arrangement.spacedBy(1.dp)
                        ) {
                            items(filtered) { (name, flag) ->
                                val isSelected = (name == userCountry)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(if (isSelected) Color(0xFF2C2C2E) else Color(0xFF1C1C1E))
                                        .clickable {
                                            IOSFeedbackHelper.vibrateLight(context)
                                            viewModel.setUserCountry(name)
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text(text = flag, fontSize = 22.sp)
                                        Text(text = name, color = Color.White, fontSize = 16.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = IOSBlue, modifier = Modifier.size(20.dp))
                                    }
                                }
                                HorizontalDivider(color = Color(0x22FFFFFF), thickness = 0.5.dp)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                IOSFeedbackHelper.vibrateLight(context)
                                currentStep = 2
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Text("Continue", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }

                // ==========================================
                // STEP 2: APPEARANCE (Light, Dark, Tinted)
                // ==========================================
                2 -> {
                    val iconTintStyle by viewModel.iconTintStyle.collectAsState()
                    var selectedStyle by remember { mutableStateOf(iconTintStyle) }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            SetupTopBar(onBack = { currentStep = 1 })

                            Text(
                                text = "Appearance",
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            Text(
                                text = "Choose your look and see how iOS 26 adjusts automatically.",
                                color = Color.Gray,
                                fontSize = 15.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                            )

                            // Phone Mockup Cards
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Light Phone Card
                                AppearancePhoneCard(
                                    title = "Light",
                                    isDark = false,
                                    isSelected = selectedStyle == "DEFAULT",
                                    onClick = {
                                        selectedStyle = "DEFAULT"
                                        viewModel.setIconTintStyle("DEFAULT")
                                        IOSFeedbackHelper.vibrateLight(context)
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                // Dark Phone Card
                                AppearancePhoneCard(
                                    title = "Dark",
                                    isDark = true,
                                    isSelected = selectedStyle == "DARK",
                                    onClick = {
                                        selectedStyle = "DARK"
                                        viewModel.setIconTintStyle("DARK")
                                        IOSFeedbackHelper.vibrateLight(context)
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                // Tinted Phone Card
                                AppearancePhoneCard(
                                    title = "Tinted",
                                    isDark = true,
                                    isTinted = true,
                                    isSelected = selectedStyle == "TINTED",
                                    onClick = {
                                        selectedStyle = "TINTED"
                                        viewModel.setIconTintStyle("TINTED")
                                        IOSFeedbackHelper.vibrateLight(context)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                IOSFeedbackHelper.vibrateLight(context)
                                currentStep = 3
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(bottom = 10.dp)
                        ) {
                            Text("Continue", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }

                // ==========================================
                // STEP 3: QUICK START (Futuristic Orb)
                // ==========================================
                3 -> {
                    val infiniteTransition = rememberInfiniteTransition(label = "orbPulse")
                    val orbScale by infiniteTransition.animateFloat(
                        initialValue = 0.94f,
                        targetValue = 1.06f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(2200, easing = FastOutSlowInEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "orbScale"
                    )
                    val orbRotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(12000, easing = LinearEasing),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "orbRotation"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SetupTopBar(onBack = { currentStep = 2 })

                            Text(
                                text = "Quick Start",
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            Text(
                                text = "Bring your current iPhone or iPad near this iPhone to sign in and set up automatically.",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp, start = 16.dp, end = 16.dp)
                            )
                        }

                        // Glowing Orb Graphic
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .graphicsLayer {
                                    scaleX = orbScale
                                    scaleY = orbScale
                                    rotationZ = orbRotation
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            Color(0xFF007AFF).copy(alpha = 0.8f),
                                            Color(0xFF5856D6).copy(alpha = 0.5f),
                                            Color(0xFF34C759).copy(alpha = 0.2f),
                                            Color.Transparent
                                        )
                                    ),
                                    radius = size.minDimension / 2f
                                )
                                drawCircle(
                                    brush = Brush.sweepGradient(
                                        colors = listOf(
                                            Color(0xFF30D158),
                                            Color(0xFF0A84FF),
                                            Color(0xFFBF5AF2),
                                            Color(0xFFFF375F),
                                            Color(0xFF30D158)
                                        )
                                    ),
                                    radius = size.minDimension / 2.6f
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.AllInclusive,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(54.dp)
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Button(
                                onClick = {
                                    IOSFeedbackHelper.vibrateLight(context)
                                    currentStep = 4
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text("Set Up Manually", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }

                            Text(
                                text = "Use Another Device",
                                color = IOSBlue,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clickable {
                                        IOSFeedbackHelper.vibrateLight(context)
                                        currentStep = 4
                                    }
                                    .padding(8.dp)
                            )
                        }
                    }
                }

                // ==========================================
                // STEP 4: WI-FI NETWORK (Real Wi-Fi Scan)
                // ==========================================
                4 -> {
                    val availableWifi by viewModel.availableWifiList.collectAsState()
                    val currentWifiSsid by viewModel.currentWifiSsid.collectAsState()
                    val isWifiScanning by viewModel.isWifiScanning.collectAsState()
                    val isWifiConnected by viewModel.isWifiConnected.collectAsState()

                    LaunchedEffect(Unit) {
                        viewModel.scanWifiNetworks(context)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            SetupTopBar(onBack = { currentStep = 3 })

                            Text(
                                text = "Wi-Fi Network",
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            Text(
                                text = "Choose a network to continue setup, or use cellular connection.",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("CHOOSE A NETWORK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isWifiScanning) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 1.5.dp, color = IOSBlue)
                                    }
                                    IconButton(
                                        onClick = { viewModel.scanWifiNetworks(context) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Rounded.Refresh, contentDescription = "Scan", tint = IOSBlue, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f, fill = false)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFF1C1C1E))
                            ) {
                                if (availableWifi.isEmpty()) {
                                    item {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(20.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = if (isWifiScanning) "Scanning for Wi-Fi networks..." else "No networks found nearby.",
                                                color = Color.Gray,
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                } else {
                                    items(availableWifi) { (ssid, isProtected) ->
                                        val isConnected = isWifiConnected && currentWifiSsid == ssid
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    IOSFeedbackHelper.vibrateLight(context)
                                                }
                                                .padding(horizontal = 16.dp, vertical = 14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Wifi,
                                                    contentDescription = null,
                                                    tint = if (isConnected) IOSBlue else Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Text(
                                                    text = ssid,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = if (isConnected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                            }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                if (isProtected) {
                                                    Icon(Icons.Rounded.Lock, contentDescription = "Locked", tint = Color.Gray, modifier = Modifier.size(15.dp))
                                                }
                                                if (isConnected) {
                                                    Icon(Icons.Rounded.Check, contentDescription = "Connected", tint = IOSBlue, modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                        HorizontalDivider(color = Color(0x22FFFFFF), thickness = 0.5.dp)
                                    }
                                }
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Button(
                                onClick = {
                                    IOSFeedbackHelper.vibrateLight(context)
                                    currentStep = 5
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text("Continue", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }

                            Text(
                                text = "Continue without Wi-Fi",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clickable {
                                        IOSFeedbackHelper.vibrateLight(context)
                                        currentStep = 5
                                    }
                                    .padding(6.dp)
                            )
                        }
                    }
                }

                // ==========================================
                // STEP 5: DATA & PRIVACY (Apple Privacy Icon)
                // ==========================================
                5 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        SetupTopBar(onBack = { currentStep = 4 })

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(18.dp),
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            // Authentic Data & Privacy Icon
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .background(IOSBlue),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_apple_privacy),
                                    contentDescription = "Data & Privacy",
                                    tint = Color.White,
                                    modifier = Modifier.size(42.dp)
                                )
                            }

                            Text(
                                text = "Data & Privacy",
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "This icon appears when Apple features ask to use your personal information.\n\nApple believes privacy is a fundamental human right, so every Apple product is designed to minimize the collection and use of your data, use on-device processing whenever possible, and provide transparency and control over your information.",
                                color = Color(0xFFD1D1D6),
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )

                            Text(
                                text = "Learn More...",
                                color = IOSBlue,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Button(
                            onClick = {
                                IOSFeedbackHelper.vibrateLight(context)
                                currentStep = 6
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(bottom = 10.dp)
                        ) {
                            Text("Continue", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }

                // ==========================================
                // STEP 6: FACE ID (Glowing 3D Holographic Mesh)
                // ==========================================
                6 -> {
                    var isScanning by remember { mutableStateOf(false) }
                    var scanCompleted by remember { mutableStateOf(false) }
                    val scanTransition = rememberInfiniteTransition(label = "scanLaser")
                    val scanProgress by scanTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1600, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "laserPos"
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SetupTopBar(onBack = { currentStep = 5 })

                            Text(
                                text = "Face ID",
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            Text(
                                text = if (scanCompleted) "Face ID is now set up." else "iPhone can recognize your unique facial features to unlock securely.",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }

                        // Glowing Emerald Face Mesh Graphic
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(Color(0xFF0F1A13))
                                .border(1.5.dp, if (scanCompleted) Color(0xFF34C759) else Color(0x3334C759), RoundedCornerShape(32.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Draw futuristic wireframe face oval
                                drawOval(
                                    color = if (scanCompleted) Color(0xFF34C759) else Color(0xFF30D158).copy(alpha = 0.5f),
                                    topLeft = Offset(size.width * 0.25f, size.height * 0.15f),
                                    size = androidx.compose.ui.geometry.Size(size.width * 0.5f, size.height * 0.7f),
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                                )

                                // Scanning laser beam
                                if (isScanning && !scanCompleted) {
                                    val laserY = size.height * (0.15f + 0.7f * scanProgress)
                                    drawLine(
                                        brush = Brush.horizontalGradient(
                                            listOf(Color.Transparent, Color(0xFF30D158), Color(0xFF69F0AE), Color.Transparent)
                                        ),
                                        start = Offset(0f, laserY),
                                        end = Offset(size.width, laserY),
                                        strokeWidth = 3f
                                    )
                                }
                            }

                            if (scanCompleted) {
                                Icon(Icons.Rounded.CheckCircle, contentDescription = "Done", tint = Color(0xFF34C759), modifier = Modifier.size(68.dp))
                            } else {
                                Icon(Icons.Rounded.Face, contentDescription = "Face ID", tint = Color(0xFF30D158), modifier = Modifier.size(64.dp))
                            }
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (!scanCompleted && !isScanning) {
                                        isScanning = true
                                        IOSFeedbackHelper.vibrateLight(context)
                                        coroutineScope.launch {
                                            delay(1600)
                                            scanCompleted = true
                                            isScanning = false
                                            viewModel.setFaceIdEnabled(true)
                                            IOSFeedbackHelper.vibrateSuccess(context)
                                        }
                                    } else {
                                        currentStep = 7
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = if (scanCompleted) "Continue" else (if (isScanning) "Scanning Face..." else "Set Up Face ID"),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            Text(
                                text = "Set Up Later in Settings",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clickable {
                                        IOSFeedbackHelper.vibrateLight(context)
                                        currentStep = 7
                                    }
                                    .padding(6.dp)
                            )
                        }
                    }
                }

                // ==========================================
                // STEP 7: PASSCODE (Authentic iOS Keypad)
                // ==========================================
                7 -> {
                    var enteredPin by remember { mutableStateOf("") }
                    val maxDigits = 6

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            SetupTopBar(onBack = { currentStep = 6 })

                            Text(
                                text = "Create a Passcode",
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            Text(
                                text = "A passcode protects your data and is used to unlock iPhone.",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
                            )

                            // 6 Dots
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                repeat(maxDigits) { index ->
                                    val isFilled = index < enteredPin.length
                                    Box(
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clip(CircleShape)
                                            .background(if (isFilled) Color.White else Color.Transparent)
                                            .border(1.5.dp, Color.White, CircleShape)
                                    )
                                }
                            }
                        }

                        // Circular Number Keypad (1 to 9, 0, delete)
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val keys = listOf(
                                listOf("1" to "", "2" to "ABC", "3" to "DEF"),
                                listOf("4" to "GHI", "5" to "JKL", "6" to "MNO"),
                                listOf("7" to "PQRS", "8" to "TUV", "9" to "WXYZ"),
                                listOf("" to "", "0" to "", "DEL" to "")
                            )

                            keys.forEach { row ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                                ) {
                                    row.forEach { (digit, sub) ->
                                        if (digit.isEmpty()) {
                                            Spacer(modifier = Modifier.size(68.dp))
                                        } else if (digit == "DEL") {
                                            Box(
                                                modifier = Modifier
                                                    .size(68.dp)
                                                    .clip(CircleShape)
                                                    .clickable {
                                                        if (enteredPin.isNotEmpty()) {
                                                            IOSFeedbackHelper.vibrateLight(context)
                                                            enteredPin = enteredPin.dropLast(1)
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Rounded.Backspace, contentDescription = "Delete", tint = Color.White, modifier = Modifier.size(22.dp))
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .size(68.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF2C2C2E))
                                                    .clickable {
                                                        if (enteredPin.length < maxDigits) {
                                                            IOSFeedbackHelper.vibrateLight(context)
                                                            enteredPin += digit
                                                            if (enteredPin.length == maxDigits) {
                                                                viewModel.setPasscode(enteredPin)
                                                                IOSFeedbackHelper.vibrateSuccess(context)
                                                                coroutineScope.launch {
                                                                    delay(300)
                                                                    currentStep = 8
                                                                }
                                                            }
                                                        }
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                    Text(text = digit, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Normal)
                                                    if (sub.isNotEmpty()) {
                                                        Text(text = sub, color = Color.Gray, fontSize = 9.sp, letterSpacing = 1.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Text(
                            text = "Passcode Options",
                            color = IOSBlue,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clickable {
                                    IOSFeedbackHelper.vibrateLight(context)
                                    currentStep = 8
                                }
                                .padding(bottom = 12.dp)
                        )
                    }
                }

                // ==========================================
                // STEP 8: TRANSFER YOUR DATA
                // ==========================================
                8 -> {
                    var selectedOption by remember { mutableIntStateOf(3) } // Default: Don't Transfer

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            SetupTopBar(onBack = { currentStep = 7 })

                            Text(
                                text = "Transfer Your Data",
                                color = Color.White,
                                fontSize = 30.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                            Text(
                                text = "Choose how you want to transfer apps and data to this iPhone.",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                            )

                            // 4 Transfer Cards
                            Column(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TransferOptionCard(
                                    title = "From iCloud Backup",
                                    subtitle = "Restore data & apps from your iCloud account",
                                    icon = Icons.Rounded.CloudDownload,
                                    isSelected = selectedOption == 0,
                                    onClick = {
                                        selectedOption = 0
                                        IOSFeedbackHelper.vibrateLight(context)
                                    }
                                )
                                TransferOptionCard(
                                    title = "From Another iPhone",
                                    subtitle = "Transfer directly using wireless device-to-device",
                                    icon = Icons.Rounded.Phonelink,
                                    isSelected = selectedOption == 1,
                                    onClick = {
                                        selectedOption = 1
                                        IOSFeedbackHelper.vibrateLight(context)
                                    }
                                )
                                TransferOptionCard(
                                    title = "From Android Phone",
                                    subtitle = "Move photos, messages, contacts from Android",
                                    icon = Icons.Rounded.SwapHoriz,
                                    isSelected = selectedOption == 2,
                                    onClick = {
                                        selectedOption = 2
                                        IOSFeedbackHelper.vibrateLight(context)
                                    }
                                )
                                TransferOptionCard(
                                    title = "Don't Transfer Apps & Data",
                                    subtitle = "Set up as a fresh, clean iPhone installation",
                                    icon = Icons.Rounded.CheckCircleOutline,
                                    isSelected = selectedOption == 3,
                                    onClick = {
                                        selectedOption = 3
                                        IOSFeedbackHelper.vibrateLight(context)
                                    }
                                )
                            }
                        }

                        Button(
                            onClick = {
                                IOSFeedbackHelper.vibrateLight(context)
                                currentStep = 9
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(bottom = 10.dp)
                        ) {
                            Text("Continue", fontSize = 17.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                        }
                    }
                }

                // ==========================================
                // STEP 9: WELCOME TO IPHONE (Apple Logo)
                // ==========================================
                9 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                            .pointerInput(Unit) {
                                detectVerticalDragGestures { _, dragAmount ->
                                    if (dragAmount < -30f) {
                                        IOSFeedbackHelper.vibrateSuccess(context)
                                        onComplete()
                                    }
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Spacer(modifier = Modifier.height(40.dp))

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(20.dp)
                        ) {
                            // Glowing Apple Logo
                            Box(
                                modifier = Modifier
                                    .size(90.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF1C1C1E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_apple_logo),
                                    contentDescription = "Apple",
                                    tint = Color.White,
                                    modifier = Modifier.size(52.dp)
                                )
                            }

                            Text(
                                text = "Welcome to iPhone",
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Text(
                                text = "iOS 26 is ready.\nPersonal intelligence, fluid gestures, and crafted elegance.",
                                color = Color.LightGray,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 22.sp
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.padding(bottom = 20.dp)
                        ) {
                            Button(
                                onClick = {
                                    IOSFeedbackHelper.vibrateSuccess(context)
                                    onComplete()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IOSBlue),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                            ) {
                                Text(
                                    text = "Get Started",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "Swipe up to open",
                                    color = Color.Gray,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clickable { onBack() }
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Back", tint = IOSBlue, modifier = Modifier.size(24.dp))
            Text("Back", color = IOSBlue, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun AppearancePhoneCard(
    title: String,
    isDark: Boolean,
    isTinted: Boolean = false,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) IOSBlue else Color(0x33FFFFFF),
                shape = RoundedCornerShape(16.dp)
            )
            .background(Color(0xFF1C1C1E))
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Mini Phone Mockup
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) Color(0xFF121214) else Color(0xFFE5E5EA))
                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(12.dp))
                .padding(6.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mini Dynamic Island
                Box(
                    modifier = Modifier
                        .width(28.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Color.Black)
                )

                // 2x2 Mini Icons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isTinted) Color(0xFF007AFF) else if (it == 0) Color(0xFF34C759) else Color(0xFFFF9500))
                        )
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    repeat(2) {
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isTinted) Color(0xFF007AFF) else if (it == 0) Color(0xFF007AFF) else Color(0xFFFF2D55))
                        )
                    }
                }

                // Mini Dock
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0x33000000))
                )
            }
        }

        Text(
            text = title,
            color = if (isSelected) Color.White else Color.Gray,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

@Composable
private fun TransferOptionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) IOSBlue else Color(0x22FFFFFF),
                shape = RoundedCornerShape(14.dp)
            )
            .background(if (isSelected) Color(0xFF242426) else Color(0xFF1C1C1E))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) IOSBlue else Color(0xFF2C2C2E)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(text = title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(text = subtitle, color = Color.Gray, fontSize = 12.sp, lineHeight = 16.sp)
            }
        }

        if (isSelected) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = IOSBlue, modifier = Modifier.size(20.dp))
        }
    }
}
