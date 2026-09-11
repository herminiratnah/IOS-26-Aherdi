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
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.MessageThread
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

@Composable
fun MessagesApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadDeviceSms(context)
        viewModel.markAllMessagesRead()
    }

    val threads by viewModel.messagesThreads.collectAsState()
    val activeThreadId by viewModel.activeThreadId.collectAsState()
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()

    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
    val cellularBars by viewModel.cellularSignalBars.collectAsState()
    val networkType by viewModel.networkType.collectAsState()

    var isViewingThread by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isComposingNewMessage by remember { mutableStateOf(false) }
    var composeRecipient by remember { mutableStateOf("") }
    var composeBody by remember { mutableStateOf("") }

    val activeThread = threads.find { it.id == activeThreadId } ?: threads.firstOrNull()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status bar (Dark icons on white background)
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
                isDarkIcons = true
            )

            if (isViewingThread && activeThread != null) {
                // Conversation Detail View
                ChatDetailView(
                    thread = activeThread,
                    onBack = { isViewingThread = false },
                    onSendMessage = { text ->
                        viewModel.sendRealSms(activeThread.id, text, context)
                    },
                    onCallContact = {
                        viewModel.makeRealPhoneCall(context, activeThread.id)
                    }
                )
            } else {
                // Conversation List View (Matches Image 2)
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    // Top Navigation Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { /* Filter */ }) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = "Filter",
                                tint = IOSBlue
                            )
                        }
                        IconButton(onClick = { isComposingNewMessage = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Compose",
                                tint = IOSBlue
                            )
                        }
                    }

                    // Large Title "Messages"
                    Text(
                        text = "Messages",
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )

                    // iOS Search Bar
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

                    // Message List matching Image 2
                    val filteredThreads = threads.filter {
                        it.contactName.contains(searchQuery, ignoreCase = true) ||
                                it.lastMessage.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        items(filteredThreads) { thread ->
                            MessageThreadItem(
                                thread = thread,
                                onClick = {
                                    viewModel.selectMessageThread(thread.id)
                                    isViewingThread = true
                                }
                            )
                            HorizontalDivider(
                                color = IOSSystemGray5,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(start = 68.dp)
                            )
                        }
                    }
                }
            }

            // Compose New Message Dialog
            if (isComposingNewMessage) {
                AlertDialog(
                    onDismissRequest = { isComposingNewMessage = false },
                    title = { Text("Pesan Baru (SMS)", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = composeRecipient,
                                onValueChange = { composeRecipient = it },
                                label = { Text("Nomor Telepon / Kontak") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = composeBody,
                                onValueChange = { composeBody = it },
                                label = { Text("Isi Pesan") },
                                minLines = 3,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (composeRecipient.isNotBlank() && composeBody.isNotBlank()) {
                                    viewModel.sendRealSms(composeRecipient.trim(), composeBody.trim(), context)
                                    composeRecipient = ""
                                    composeBody = ""
                                    isComposingNewMessage = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IOSBlue)
                        ) {
                            Text("Kirim SMS")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isComposingNewMessage = false }) {
                            Text("Batal", color = Color.Gray)
                        }
                    }
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
fun MessageThreadItem(
    thread: MessageThread,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Blue unread indicator dot
        if (thread.unreadCount > 0) {
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(IOSBlue)
            )
        } else {
            Spacer(modifier = Modifier.width(14.dp))
        }

        // Contact Avatar Circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color(thread.avatarColor)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = thread.contactName.take(1),
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name, Last Message, Time
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = thread.contactName,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Black
                )
                Text(
                    text = thread.time,
                    fontSize = 14.sp,
                    color = IOSSystemGray
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = thread.lastMessage,
                fontSize = 15.sp,
                color = IOSSystemGray,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun ChatDetailView(
    thread: MessageThread,
    onBack: () -> Unit,
    onSendMessage: (String) -> Unit,
    onCallContact: () -> Unit
) {
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        // Chat Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(IOSSystemGray6)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = IOSBlue
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(thread.avatarColor)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = thread.contactName.take(1),
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = thread.contactName,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Black
                )
            }

            IconButton(onClick = onCallContact) {
                Icon(
                    imageVector = Icons.Rounded.Call,
                    contentDescription = "Call",
                    tint = IOSBlue
                )
            }
        }

        // Messages Bubble List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 14.dp),
            reverseLayout = false,
            contentPadding = PaddingValues(vertical = 10.dp)
        ) {
            items(thread.messages) { msg ->
                val isMe = msg.isFromMe
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Box(
                        modifier = Modifier
                            .widthIn(max = 270.dp)
                            .clip(
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = if (isMe) 18.dp else 4.dp,
                                    bottomEnd = if (isMe) 4.dp else 18.dp
                                )
                            )
                            .background(if (isMe) IOSBlue else IOSSystemGray5)
                            .padding(horizontal = 14.dp, vertical = 9.dp)
                    ) {
                        Text(
                            text = msg.text,
                            color = if (isMe) Color.White else Color.Black,
                            fontSize = 16.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        }

        // Bottom Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(IOSSystemGray5),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add",
                    tint = IOSSystemGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = { Text("iMessage", color = IOSSystemGray) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 40.dp),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = IOSSystemGray4,
                    unfocusedBorderColor = IOSSystemGray4,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White
                )
            )

            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) IOSBlue else IOSSystemGray4)
                    .clickable {
                        if (inputText.isNotBlank()) {
                            onSendMessage(inputText)
                            inputText = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Send,
                    contentDescription = "Send",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
