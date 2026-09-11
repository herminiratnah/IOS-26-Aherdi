package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

enum class AppStoreTab {
    TODAY,
    GAMES,
    APPS,
    ARCADE,
    SEARCH
}

@Composable
fun AppStoreApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    var selectedTab by remember { mutableStateOf(AppStoreTab.TODAY) }

    var mvInstalled by remember { mutableStateOf(false) }
    var notionInstalled by remember { mutableStateOf(false) }
    var waInstalled by remember { mutableStateOf(true) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status bar
            IOSStatusBar(
                dynamicIslandData = dynamicIslandData,
                onDismissDynamicIsland = { viewModel.dismissDynamicIsland() },
                onOpenControlCenter = { viewModel.toggleControlCenter() },
                onOpenNotificationCenter = { viewModel.toggleNotificationCenter() },
                isDarkIcons = true
            )

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header with Date and Profile Avatar matching Image 12
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TUESDAY, 15 JULY",
                                color = IOSSystemGray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Today",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(IOSBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("AA", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Game of the Day Card matching Image 12
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.monument_valley_art),
                            contentDescription = "Monument Valley 3",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        // Top header text
                        Column(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(18.dp)
                        ) {
                            Text(
                                text = "GAME OF THE DAY",
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Monument Valley 3",
                                color = Color.White,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "A journey through impossible architecture",
                                color = Color.White.copy(alpha = 0.9f),
                                fontSize = 14.sp
                            )
                        }

                        // Bottom frosted app row
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color(0xD91C1C1E))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color(0xFF2C3E50)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.VideogameAsset,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                    Column {
                                        Text(
                                            text = "Monument Valley 3",
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Puzzles & Mazes",
                                            color = Color.Gray,
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Button(
                                    onClick = { mvInstalled = !mvInstalled },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0x55FFFFFF)),
                                    shape = RoundedCornerShape(18.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (mvInstalled) "OPEN" else "GET",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Apps We Love Section
                item {
                    Text(
                        text = "Apps We Love",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    AppStoreRowItem(
                        icon = Icons.Rounded.EditNote,
                        iconBg = Color.Black,
                        title = "Notion - Notes & Docs",
                        category = "Productivity",
                        isInstalled = notionInstalled,
                        onToggleInstall = { notionInstalled = !notionInstalled }
                    )
                    HorizontalDivider(color = IOSSystemGray5, thickness = 0.5.dp, modifier = Modifier.padding(start = 58.dp))

                    AppStoreRowItem(
                        icon = Icons.Rounded.ChatBubble,
                        iconBg = IOSGreen,
                        title = "WhatsApp Messenger",
                        category = "Social Networking",
                        isInstalled = waInstalled,
                        onToggleInstall = { waInstalled = !waInstalled }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }

            // Bottom Navigation Bar matching Image 12
            HorizontalDivider(color = IOSSystemGray5, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IOSSystemGray6)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppStoreBottomTabItem(
                    icon = Icons.Rounded.AutoAwesomeMotion,
                    label = "Today",
                    isSelected = selectedTab == AppStoreTab.TODAY,
                    onClick = { selectedTab = AppStoreTab.TODAY }
                )
                AppStoreBottomTabItem(
                    icon = Icons.Rounded.RocketLaunch,
                    label = "Games",
                    isSelected = selectedTab == AppStoreTab.GAMES,
                    onClick = { selectedTab = AppStoreTab.GAMES }
                )
                AppStoreBottomTabItem(
                    icon = Icons.Rounded.Layers,
                    label = "Apps",
                    isSelected = selectedTab == AppStoreTab.APPS,
                    onClick = { selectedTab = AppStoreTab.APPS }
                )
                AppStoreBottomTabItem(
                    icon = Icons.Rounded.SportsEsports,
                    label = "Arcade",
                    isSelected = selectedTab == AppStoreTab.ARCADE,
                    onClick = { selectedTab = AppStoreTab.ARCADE }
                )
                AppStoreBottomTabItem(
                    icon = Icons.Rounded.Search,
                    label = "Search",
                    isSelected = selectedTab == AppStoreTab.SEARCH,
                    onClick = { selectedTab = AppStoreTab.SEARCH }
                )
            }

            // Home indicator
            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = true
            )
        }
    }
}

@Composable
fun AppStoreRowItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    category: String,
    isInstalled: Boolean,
    onToggleInstall: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.Black)
                Text(category, fontSize = 12.sp, color = IOSSystemGray)
            }
        }

        Button(
            onClick = onToggleInstall,
            colors = ButtonDefaults.buttonColors(containerColor = IOSSystemGray5),
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isInstalled) "OPEN" else "GET",
                color = IOSBlue,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun AppStoreBottomTabItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) IOSBlue else Color.Gray,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) IOSBlue else Color.Gray
        )
    }
}
