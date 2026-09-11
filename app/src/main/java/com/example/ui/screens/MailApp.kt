package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MailItem
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.components.IOSWebEngineView
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

@Composable
fun MailApp(
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
    val mailList by viewModel.mailItems.collectAsState()

    var activeMailTab by remember { mutableStateOf("Inbox") } // "Inbox" or "Webmail (Safari)"
    var webmailProvider by remember { mutableStateOf("https://mail.google.com") }
    var selectedMail by remember { mutableStateOf<MailItem?>(null) }

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

            // Mail Tabs: Inboxes / Live Webmail
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFFE5E5EA))
                        .padding(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (activeMailTab == "Inbox") Color.White else Color.Transparent)
                            .clickable { activeMailTab = "Inbox" }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("Inboxes", color = if (activeMailTab == "Inbox") Color.Black else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (activeMailTab == "Webmail (Safari)") Color.White else Color.Transparent)
                            .clickable { activeMailTab = "Webmail (Safari)" }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Language, contentDescription = null, tint = if (activeMailTab == "Webmail (Safari)") IOSBlue else Color.Gray, modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Safari Webmail", color = if (activeMailTab == "Webmail (Safari)") Color.Black else Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            if (activeMailTab == "Webmail (Safari)") {
                Column(modifier = Modifier.weight(1f)) {
                    // Quick provider pills
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            Pair("Gmail", "https://mail.google.com"),
                            Pair("Outlook", "https://outlook.live.com"),
                            Pair("iCloud", "https://www.icloud.com/mail")
                        ).forEach { (label, url) ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (webmailProvider == url) IOSBlue else Color(0xFFF2F2F7))
                                    .clickable { webmailProvider = url }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(label, color = if (webmailProvider == url) Color.White else Color.Black, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    IOSWebEngineView(
                        initialUrl = webmailProvider,
                        title = "Live Webmail",
                        modifier = Modifier.weight(1f),
                        showHeaderBar = true
                    )
                }
            } else if (selectedMail != null) {
                // Mail Detail View
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { selectedMail = null }) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = IOSBlue)
                        }
                        Text(selectedMail!!.time, color = IOSSystemGray, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = selectedMail!!.subject,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "From: ${selectedMail!!.sender}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = selectedMail!!.preview,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        color = Color.DarkGray
                    )
                }
            } else {
                // Mail List View matching Image 10
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    // Top Bar with "Mailboxes" and Compose
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowLeft,
                                contentDescription = null,
                                tint = IOSBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Text("Mailboxes", color = IOSBlue, fontSize = 17.sp)
                        }
                        Text("Edit", color = IOSBlue, fontSize = 17.sp)
                    }

                    // Large Title "All Inboxes"
                    Text(
                        text = "All Inboxes",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )

                    // Search Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(IOSSystemGray6)
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Search,
                                contentDescription = "Search",
                                tint = IOSSystemGray,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Search",
                                color = IOSSystemGray,
                                fontSize = 16.sp
                            )
                        }
                    }

                    // Mail Items matching Image 10
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(mailList) { mail ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMail = mail }
                                    .padding(vertical = 10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                // Unread blue dot
                                if (mail.isUnread) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 6.dp, end = 6.dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(IOSBlue)
                                    )
                                } else {
                                    Spacer(modifier = Modifier.width(14.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = mail.sender,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                        Text(
                                            text = mail.time,
                                            fontSize = 14.sp,
                                            color = IOSSystemGray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mail.subject,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = mail.preview,
                                        fontSize = 14.sp,
                                        color = IOSSystemGray,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = IOSSystemGray5,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 14.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Bar
            HorizontalDivider(color = IOSSystemGray5, thickness = 0.5.dp)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(IOSSystemGray6)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = "Updated Just Now",
                    fontSize = 11.sp,
                    color = IOSSystemGray
                )
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Compose",
                    tint = IOSBlue,
                    modifier = Modifier.size(22.dp)
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
