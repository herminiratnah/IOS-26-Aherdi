package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import com.example.util.RootExecutor
import com.example.model.CallRecord
import com.example.model.ContactItem
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

enum class PhoneTab {
    FAVORITES,
    RECENTS,
    CONTACTS,
    KEYPAD,
    VOICEMAIL
}

@Composable
fun PhoneApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val phonePermissionsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        viewModel.loadDeviceContacts(context)
        viewModel.loadDeviceCallLogs(context)
    }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                RootExecutor.execute("pm grant ${context.packageName} android.permission.CALL_PHONE")
                RootExecutor.execute("pm grant ${context.packageName} android.permission.READ_PHONE_STATE")
                RootExecutor.execute("pm grant ${context.packageName} android.permission.READ_CONTACTS")
                RootExecutor.execute("pm grant ${context.packageName} android.permission.WRITE_CONTACTS")
                RootExecutor.execute("pm grant ${context.packageName} android.permission.READ_CALL_LOG")
            } catch (ignored: Exception) {}
        }
        phonePermissionsLauncher.launch(
            arrayOf(
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.WRITE_CONTACTS,
                Manifest.permission.READ_CALL_LOG
            )
        )
        viewModel.loadDeviceContacts(context)
        viewModel.loadDeviceCallLogs(context)
    }

    val callRecords by viewModel.callRecords.collectAsState()
    val realContacts by viewModel.contactsList.collectAsState()
    val isInCall by viewModel.isInCall.collectAsState()
    val callContactName by viewModel.callContactName.collectAsState()
    val callPhoneNumber by viewModel.callPhoneNumber.collectAsState()
    val callDurationSeconds by viewModel.callDurationSeconds.collectAsState()
    val isCallMuted by viewModel.isCallMuted.collectAsState()
    val isCallSpeaker by viewModel.isCallSpeaker.collectAsState()
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()

    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
    val cellularBars by viewModel.cellularSignalBars.collectAsState()
    val networkType by viewModel.networkType.collectAsState()

    var selectedTab by remember { mutableStateOf(PhoneTab.KEYPAD) }
    var recentsFilter by remember { mutableStateOf("All") } // "All" or "Missed"
    var dialpadNumber by remember { mutableStateOf("") }
    var showAddContactDialog by remember { mutableStateOf(false) }
    var pendingContactPhone by remember { mutableStateOf("") }

    if (showAddContactDialog) {
        NewContactDialog(
            initialPhone = pendingContactPhone,
            onDismiss = { showAddContactDialog = false },
            onSave = { name, phone, email ->
                viewModel.addNewContact(context, name = name, phone = phone, email = email)
                showAddContactDialog = false
            }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isInCall) Color(0xFF1C1C1E) else Color.White)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status bar with real-time telemetry
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
                isDarkIcons = !isInCall
            )

            if (isInCall) {
                // In-App Active Call Screen
                ActiveCallScreen(
                    contactName = callContactName,
                    phoneNumber = callPhoneNumber,
                    durationSeconds = callDurationSeconds,
                    isMuted = isCallMuted,
                    isSpeaker = isCallSpeaker,
                    onToggleMute = { viewModel.toggleMuteCall() },
                    onToggleSpeaker = { viewModel.toggleSpeakerCall() },
                    onEndCall = { viewModel.endCall(context) }
                )
            } else {
                // Main Phone Views
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        PhoneTab.RECENTS -> {
                            RecentsView(
                                records = callRecords,
                                filter = recentsFilter,
                                onFilterChange = { recentsFilter = it },
                                onCall = { name, number ->
                                    viewModel.makeRealPhoneCall(context, number, name)
                                }
                            )
                        }
                        PhoneTab.KEYPAD -> {
                            KeypadView(
                                enteredNumber = dialpadNumber,
                                onNumberChange = { dialpadNumber = it },
                                onCall = {
                                    if (dialpadNumber.isNotBlank()) {
                                        viewModel.makeRealPhoneCall(context, dialpadNumber)
                                    }
                                },
                                onAddContact = { num ->
                                    pendingContactPhone = num
                                    showAddContactDialog = true
                                }
                            )
                        }
                        PhoneTab.CONTACTS -> {
                            ContactsView(
                                contactsList = realContacts,
                                onAddContact = {
                                    pendingContactPhone = ""
                                    showAddContactDialog = true
                                },
                                onCall = { name, number ->
                                    viewModel.makeRealPhoneCall(context, number, name)
                                }
                            )
                        }
                        PhoneTab.FAVORITES -> {
                            FavoritesView(
                                onCall = { name, number ->
                                    viewModel.makeRealPhoneCall(context, number, name)
                                }
                            )
                        }
                        PhoneTab.VOICEMAIL -> {
                            VoicemailView()
                        }
                    }
                }

                // Bottom Tab Bar matching iOS Phone
                HorizontalDivider(color = IOSSystemGray5, thickness = 0.5.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(IOSSystemGray6)
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PhoneTabItem(
                        icon = Icons.Rounded.Star,
                        label = "Favorites",
                        isSelected = selectedTab == PhoneTab.FAVORITES,
                        onClick = { selectedTab = PhoneTab.FAVORITES }
                    )
                    PhoneTabItem(
                        icon = Icons.Rounded.AccessTime,
                        label = "Recents",
                        isSelected = selectedTab == PhoneTab.RECENTS,
                        onClick = { selectedTab = PhoneTab.RECENTS }
                    )
                    PhoneTabItem(
                        icon = Icons.Rounded.AccountCircle,
                        label = "Contacts",
                        isSelected = selectedTab == PhoneTab.CONTACTS,
                        onClick = { selectedTab = PhoneTab.CONTACTS }
                    )
                    PhoneTabItem(
                        icon = Icons.Rounded.Dialpad,
                        label = "Keypad",
                        isSelected = selectedTab == PhoneTab.KEYPAD,
                        onClick = { selectedTab = PhoneTab.KEYPAD }
                    )
                    PhoneTabItem(
                        icon = Icons.Rounded.Voicemail,
                        label = "Voicemail",
                        isSelected = selectedTab == PhoneTab.VOICEMAIL,
                        onClick = { selectedTab = PhoneTab.VOICEMAIL }
                    )
                }
            }

            // Home indicator bar
            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = !isInCall
            )
        }
    }
}

@Composable
fun RecentsView(
    records: List<CallRecord>,
    filter: String,
    onFilterChange: (String) -> Unit,
    onCall: (String, String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // Top "All" / "Missed" segment control matching Image 3
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(IOSSystemGray5)
                    .padding(2.dp)
            ) {
                listOf("All", "Missed").forEach { tab ->
                    val isSelected = filter == tab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isSelected) Color.White else Color.Transparent)
                            .clickable { onFilterChange(tab) }
                            .padding(horizontal = 24.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = tab,
                            color = Color.Black,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        // Title "Recents"
        Text(
            text = "Recents",
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            letterSpacing = (-0.5).sp,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        val displayedRecords = if (filter == "Missed") records.filter { it.isMissed } else records

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(displayedRecords) { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCall(item.contactName, item.phoneNumber) }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Contact avatar
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(item.avatarColor)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.contactName.take(1),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.contactName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (item.isMissed) IOSRed else Color.Black
                        )
                        Text(
                            text = item.callType,
                            fontSize = 13.sp,
                            color = IOSSystemGray
                        )
                    }

                    Text(
                        text = item.timeLabel,
                        fontSize = 13.sp,
                        color = IOSSystemGray,
                        modifier = Modifier.padding(end = 10.dp)
                    )

                    // Info button
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "Info",
                        tint = IOSBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                HorizontalDivider(
                    color = IOSSystemGray5,
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(start = 52.dp)
                )
            }
        }
    }
}

@Composable
fun NewContactDialog(
    initialPhone: String,
    onDismiss: () -> Unit,
    onSave: (name: String, phone: String, email: String) -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf(initialPhone) }
    var email by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val fullName = "$firstName $lastName".trim()
                    if (fullName.isNotBlank() || phoneNumber.isNotBlank()) {
                        onSave(fullName.ifBlank { phoneNumber }, phoneNumber, email)
                    }
                }
            ) {
                Text("Done", color = IOSBlue, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = IOSBlue, fontSize = 16.sp)
            }
        },
        title = {
            Text(
                "New Contact",
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(IOSSystemGray5),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = IOSSystemGray,
                        modifier = Modifier.size(38.dp)
                    )
                }

                OutlinedTextField(
                    value = firstName,
                    onValueChange = { firstName = it },
                    label = { Text("First Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = lastName,
                    onValueChange = { lastName = it },
                    label = { Text("Last Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it },
                    label = { Text("Phone") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email (Optional)") },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White
    )
}

@Composable
fun KeypadView(
    enteredNumber: String,
    onNumberChange: (String) -> Unit,
    onCall: () -> Unit,
    onAddContact: (String) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Entered Number Display
        Text(
            text = enteredNumber.ifEmpty { " " },
            fontSize = 34.sp,
            fontWeight = FontWeight.Normal,
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 4.dp)
        )

        // Add Number prompt
        if (enteredNumber.isNotEmpty()) {
            Text(
                text = "Add Number",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = IOSBlue,
                modifier = Modifier
                    .clickable { onAddContact(enteredNumber) }
                    .padding(bottom = 12.dp)
            )
        } else {
            Spacer(modifier = Modifier.height(28.dp))
        }

        // 3x4 Dialpad Keys
        val keys = listOf(
            Pair("1", ""), Pair("2", "ABC"), Pair("3", "DEF"),
            Pair("4", "GHI"), Pair("5", "JKL"), Pair("6", "MNO"),
            Pair("7", "PQRS"), Pair("8", "TUV"), Pair("9", "WXYZ"),
            Pair("*", ""), Pair("0", "+"), Pair("#", "")
        )

        Column(
            modifier = Modifier.width(280.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (row in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (col in 0 until 3) {
                        val key = keys[row * 3 + col]
                        KeypadCircleButton(
                            digit = key.first,
                            letters = key.second,
                            onClick = { onNumberChange(enteredNumber + key.first) }
                        )
                    }
                }
            }

            // Bottom row: symmetric 3 columns matching the 3 columns above!
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Column 1: Empty slot spacer (76dp)
                Box(modifier = Modifier.size(76.dp))

                // Column 2: Center Green Call Button (76dp)
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(IOSGreen)
                        .clickable { onCall() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Call,
                        contentDescription = "Call",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                // Column 3: Right Backspace delete button (76dp)
                Box(
                    modifier = Modifier.size(76.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (enteredNumber.isNotEmpty()) {
                        IconButton(
                            onClick = { onNumberChange(enteredNumber.dropLast(1)) },
                            modifier = Modifier.size(56.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Backspace,
                                contentDescription = "Delete",
                                tint = IOSSystemGray,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun KeypadCircleButton(
    digit: String,
    letters: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(IOSSystemGray6)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = digit,
                fontSize = 30.sp,
                fontWeight = FontWeight.Normal,
                color = Color.Black
            )
            if (letters.isNotEmpty()) {
                Text(
                    text = letters,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ActiveCallScreen(
    contactName: String,
    phoneNumber: String,
    durationSeconds: Int,
    isMuted: Boolean,
    isSpeaker: Boolean,
    onToggleMute: () -> Unit,
    onToggleSpeaker: () -> Unit,
    onEndCall: () -> Unit
) {
    val min = durationSeconds / 60
    val sec = durationSeconds % 60
    val formattedDuration = String.format("%02d:%02d", min, sec)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 40.dp)
        ) {
            Text(
                text = contactName.ifEmpty { phoneNumber },
                fontSize = 32.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formattedDuration,
                fontSize = 16.sp,
                color = Color.LightGray
            )
        }

        // 2x3 Calling Actions Grid
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CallActionButton(
                    icon = Icons.Rounded.MicOff,
                    label = "mute",
                    isActive = isMuted,
                    onClick = onToggleMute
                )
                CallActionButton(
                    icon = Icons.Rounded.Dialpad,
                    label = "keypad",
                    isActive = false,
                    onClick = {}
                )
                CallActionButton(
                    icon = Icons.Rounded.VolumeUp,
                    label = "speaker",
                    isActive = isSpeaker,
                    onClick = onToggleSpeaker
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CallActionButton(
                    icon = Icons.Rounded.Add,
                    label = "add call",
                    isActive = false,
                    onClick = {}
                )
                CallActionButton(
                    icon = Icons.Rounded.Videocam,
                    label = "FaceTime",
                    isActive = false,
                    onClick = {}
                )
                CallActionButton(
                    icon = Icons.Rounded.AccountCircle,
                    label = "contacts",
                    isActive = false,
                    onClick = {}
                )
            }
        }

        // End Call Button
        Box(
            modifier = Modifier
                .padding(bottom = 30.dp)
                .size(74.dp)
                .clip(CircleShape)
                .background(IOSRed)
                .clickable { onEndCall() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.CallEnd,
                contentDescription = "End Call",
                tint = Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
    }
}

@Composable
fun CallActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(if (isActive) Color.White else Color(0x33FFFFFF))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = Color.White,
            fontSize = 12.sp
        )
    }
}

@Composable
fun ContactsView(
    contactsList: List<ContactItem> = emptyList(),
    onAddContact: () -> Unit,
    onCall: (String, String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredContacts = remember(contactsList, searchQuery) {
        if (searchQuery.isBlank()) contactsList
        else contactsList.filter {
            it.name.contains(searchQuery, ignoreCase = true) || it.phoneNumber.contains(searchQuery)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Contacts",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            IconButton(
                onClick = onAddContact,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = "Add Contact",
                    tint = IOSBlue,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Search Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(IOSSystemGray6)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    tint = IOSSystemGray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                androidx.compose.foundation.text.BasicTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text("Search", color = IOSSystemGray, fontSize = 15.sp)
                        }
                        innerTextField()
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            if (filteredContacts.isNotEmpty()) {
                items(filteredContacts) { c ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCall(c.name, c.phoneNumber) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(c.avatarColor)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = c.name.take(1).uppercase(),
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = c.name,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.Black
                            )
                            Text(
                                text = c.phoneNumber,
                                fontSize = 13.sp,
                                color = IOSSystemGray
                            )
                        }
                        IconButton(
                            onClick = { onCall(c.name, c.phoneNumber) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Call,
                                contentDescription = "Call",
                                tint = IOSGreen,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    HorizontalDivider(
                        color = IOSSystemGray5,
                        thickness = 0.5.dp,
                        modifier = Modifier.padding(start = 52.dp)
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isBlank()) "Belum ada kontak. Ketuk + untuk menambahkan." else "Tidak ada kontak ditemukan.",
                            color = IOSSystemGray,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoritesView(onCall: (String, String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Favorites", fontSize = 34.sp, fontWeight = FontWeight.Bold, color = Color.Black)
        Spacer(modifier = Modifier.height(16.dp))
        listOf(
            Pair("Mama", "0811-2233-4455"),
            Pair("Raka", "0812-9988-7711"),
            Pair("Nadia", "0812-3344-7788")
        ).forEach { f ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCall(f.first, f.second) }
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.Star, contentDescription = null, tint = IOSYellow)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(f.first, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Mobile", fontSize = 13.sp, color = IOSSystemGray)
                }
            }
            HorizontalDivider(color = IOSSystemGray5, thickness = 0.5.dp)
        }
    }
}

@Composable
fun VoicemailView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Voicemail,
            contentDescription = null,
            tint = IOSSystemGray,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text("No Voicemail", fontSize = 18.sp, color = IOSSystemGray, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun PhoneTabItem(
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
            tint = if (isSelected) IOSBlue else IOSSystemGray,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            color = if (isSelected) IOSBlue else IOSSystemGray
        )
    }
}
