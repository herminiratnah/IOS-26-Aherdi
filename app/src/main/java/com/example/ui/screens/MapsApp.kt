package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.components.IOSWebEngineView
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

@Composable
fun MapsApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
    val cellularBars by viewModel.cellularSignalBars.collectAsState()
    val networkType by viewModel.networkType.collectAsState()
    val airplaneMode by viewModel.airplaneMode.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var is3D by remember { mutableStateOf(false) }
    var isNavigating by remember { mutableStateOf(false) }
    var isLiveSafariMap by remember { mutableStateOf(true) }
    var mapZoom by remember { mutableFloatStateOf(1f) }
    var mapOffset by remember { mutableStateOf(Offset.Zero) }

    val categories = listOf(
        Pair("Restaurants", Icons.Rounded.Restaurant),
        Pair("Gas Stations", Icons.Rounded.LocalGasStation),
        Pair("Coffee", Icons.Rounded.LocalCafe),
        Pair("Groceries", Icons.Rounded.ShoppingCart),
        Pair("Parking", Icons.Rounded.LocalParking)
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFE8ECEF))
            .navigationBarsPadding()
    ) {
        // Live Safari Map or Vector Canvas
        if (isLiveSafariMap) {
            IOSWebEngineView(
                initialUrl = "https://www.google.com/maps",
                title = "Maps Live",
                showHeaderBar = false,
                modifier = Modifier.fillMaxSize().padding(top = 90.dp, bottom = 120.dp)
            )
        } else {
            // Map Canvas
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            mapZoom = (mapZoom * zoom).coerceIn(0.5f, 3f)
                            mapOffset += pan
                        }
                    }
            ) {
            val width = size.width
            val height = size.height

            // Background land
            drawRect(color = Color(0xFFF4F3F0))

            // River / Ocean
            val waterPath = Path().apply {
                moveTo(0f, height * 0.25f + mapOffset.y)
                cubicTo(
                    width * 0.3f, height * 0.2f + mapOffset.y,
                    width * 0.7f, height * 0.35f + mapOffset.y,
                    width, height * 0.3f + mapOffset.y
                )
                lineTo(width, height * 0.45f + mapOffset.y)
                cubicTo(
                    width * 0.6f, height * 0.5f + mapOffset.y,
                    width * 0.4f, height * 0.35f + mapOffset.y,
                    0f, height * 0.4f + mapOffset.y
                )
                close()
            }
            drawPath(path = waterPath, color = Color(0xFFA5C9EB))

            // Park / Greenery
            drawRoundRect(
                color = Color(0xFFCCEADA),
                topLeft = Offset(width * 0.15f + mapOffset.x, height * 0.48f + mapOffset.y),
                size = androidx.compose.ui.geometry.Size(width * 0.35f, height * 0.2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f)
            )

            // Primary Highways & Roads
            drawLine(
                color = Color(0xFFFFD166),
                start = Offset(0f, height * 0.55f + mapOffset.y),
                end = Offset(width, height * 0.55f + mapOffset.y),
                strokeWidth = 14f * mapZoom
            )
            drawLine(
                color = Color.White,
                start = Offset(0f, height * 0.55f + mapOffset.y),
                end = Offset(width, height * 0.55f + mapOffset.y),
                strokeWidth = 10f * mapZoom
            )

            drawLine(
                color = Color(0xFFFFD166),
                start = Offset(width * 0.5f + mapOffset.x, 0f),
                end = Offset(width * 0.5f + mapOffset.x, height),
                strokeWidth = 14f * mapZoom
            )
            drawLine(
                color = Color.White,
                start = Offset(width * 0.5f + mapOffset.x, 0f),
                end = Offset(width * 0.5f + mapOffset.x, height),
                strokeWidth = 10f * mapZoom
            )

            // Secondary Roads
            for (i in 1..4) {
                drawLine(
                    color = Color.White,
                    start = Offset(0f, (height * 0.18f * i) + mapOffset.y),
                    end = Offset(width, (height * 0.18f * i) + mapOffset.y),
                    strokeWidth = 6f * mapZoom
                )
                drawLine(
                    color = Color.White,
                    start = Offset((width * 0.22f * i) + mapOffset.x, 0f),
                    end = Offset((width * 0.22f * i) + mapOffset.x, height),
                    strokeWidth = 6f * mapZoom
                )
            }

            // Route highlight if navigating
            if (isNavigating) {
                val routePath = Path().apply {
                    moveTo(width * 0.5f + mapOffset.x, height * 0.55f + mapOffset.y)
                    lineTo(width * 0.72f + mapOffset.x, height * 0.55f + mapOffset.y)
                    lineTo(width * 0.72f + mapOffset.x, height * 0.36f + mapOffset.y)
                }
                drawPath(
                    path = routePath,
                    color = Color(0xFF007AFF),
                    style = Stroke(width = 12f * mapZoom)
                )

                // Destination marker
                drawCircle(
                    color = Color(0xFFFF3B30),
                    radius = 16f,
                    center = Offset(width * 0.72f + mapOffset.x, height * 0.36f + mapOffset.y)
                )
            }

            // Current user location pulse
            drawCircle(
                color = Color(0x33007AFF),
                radius = 28f * mapZoom,
                center = Offset(width * 0.5f + mapOffset.x, height * 0.55f + mapOffset.y)
            )
            drawCircle(
                color = Color.White,
                radius = 14f * mapZoom,
                center = Offset(width * 0.5f + mapOffset.x, height * 0.55f + mapOffset.y)
            )
            drawCircle(
                color = Color(0xFF007AFF),
                radius = 10f * mapZoom,
                center = Offset(width * 0.5f + mapOffset.x, height * 0.55f + mapOffset.y)
            )
        }
    }

        // Top Controls: Status Bar + Search Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
        ) {
            IOSStatusBar(
                dynamicIslandData = dynamicIslandData,
                onDismissDynamicIsland = { viewModel.dismissDynamicIsland() },
                onOpenControlCenter = { viewModel.toggleControlCenter() },
                onOpenNotificationCenter = { viewModel.toggleNotificationCenter() },
                currentTime = currentTime,
                batteryLevel = batteryLevel,
                isBatteryCharging = isBatteryCharging,
                isWifiConnected = isWifiConnected,
                wifiSignalLevel = wifiSignalLevel,
                cellularBars = cellularBars,
                networkType = networkType,
                isAirplaneMode = airplaneMode,
                isDarkIcons = true
            )

            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .shadow(4.dp, RoundedCornerShape(14.dp))
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color.White)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "Search Maps" else searchQuery,
                        color = if (searchQuery.isBlank()) Color.Gray else Color.Black,
                        fontSize = 16.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = "Voice Search",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Floating Action Buttons on Right
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Live Safari Web Map Toggle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(if (isLiveSafariMap) Color(0xFF007AFF) else Color.White)
                    .clickable { isLiveSafariMap = !isLiveSafariMap },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Language,
                    contentDescription = "Safari Web Map Live",
                    tint = if (isLiveSafariMap) Color.White else Color(0xFF007AFF),
                    modifier = Modifier.size(22.dp)
                )
            }

            // 3D toggle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable { is3D = !is3D },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (is3D) "2D" else "3D",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF007AFF)
                )
            }

            // Recenter Location
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
                    .clickable {
                        mapOffset = Offset.Zero
                        mapZoom = 1f
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MyLocation,
                    contentDescription = "Location",
                    tint = Color(0xFF007AFF),
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // Bottom Card: Explore / Navigation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(10.dp, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color.White)
                    .padding(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Pill handle
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(0xFFD1D1D6))
                            .align(Alignment.CenterHorizontally)
                    )

                    if (isNavigating) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "18 min (8.4 km)",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34C759)
                                )
                                Text(
                                    text = "Fastest route now • Grand Indonesia",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            Button(
                                onClick = {
                                    isNavigating = false
                                    viewModel.dismissDynamicIsland()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF3B30)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("End", fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Jakarta, Indonesia",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                                Text(
                                    text = "Clear • 28°C",
                                    fontSize = 13.sp,
                                    color = Color.Gray
                                )
                            }
                            Button(
                                onClick = {
                                    isNavigating = true
                                    viewModel.showDynamicIslandNotification("Turn Right in 200m", "Jl. M.H. Thamrin", "navigation", 0xFF007AFF)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Rounded.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Text("Directions", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Categories row
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(categories) { (name, icon) ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF2F2F7))
                                        .clickable {
                                            searchQuery = name
                                        }
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = name,
                                        tint = Color(0xFF007AFF),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.Black)
                                }
                            }
                        }
                    }
                }
            }

            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = true
            )
        }
    }
}
