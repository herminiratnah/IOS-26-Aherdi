package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ReminderItem
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

@Composable
fun RemindersApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.loadReminders(context)
    }

    val reminders by viewModel.reminders.collectAsState()
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
    val cellularBars by viewModel.cellularSignalBars.collectAsState()
    val networkType by viewModel.networkType.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") } // "Today", "Scheduled", "All", "Flagged", "Completed"
    var showAddDialog by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }
    var newNotes by remember { mutableStateOf("") }
    var newDueDate by remember { mutableStateOf("Today") }

    val todayCount = reminders.count { !it.isCompleted && it.dueDate.contains("Today", ignoreCase = true) }
    val scheduledCount = reminders.count { !it.isCompleted && it.dueDate.contains("Scheduled", ignoreCase = true) }
    val allCount = reminders.count { !it.isCompleted }
    val completedCount = reminders.count { it.isCompleted }

    val filteredList = when (selectedFilter) {
        "Today" -> reminders.filter { it.dueDate.contains("Today", ignoreCase = true) }
        "Scheduled" -> reminders.filter { it.dueDate.contains("Scheduled", ignoreCase = true) }
        "Completed" -> reminders.filter { it.isCompleted }
        else -> reminders
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Status Bar
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

            // Header & Search
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Reminders",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                IconButton(onClick = { showAddDialog = true }) {
                    Icon(
                        imageVector = Icons.Rounded.AddCircle,
                        contentDescription = "New Reminder",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Category summary cards (2x2 grid)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ReminderCategoryCard(
                                title = "Today",
                                count = todayCount,
                                icon = Icons.Rounded.CalendarToday,
                                iconBg = Color(0xFF007AFF),
                                isSelected = selectedFilter == "Today",
                                modifier = Modifier.weight(1f),
                                onClick = { selectedFilter = if (selectedFilter == "Today") "All" else "Today" }
                            )
                            ReminderCategoryCard(
                                title = "Scheduled",
                                count = scheduledCount,
                                icon = Icons.Rounded.Event,
                                iconBg = Color(0xFFFF3B30),
                                isSelected = selectedFilter == "Scheduled",
                                modifier = Modifier.weight(1f),
                                onClick = { selectedFilter = if (selectedFilter == "Scheduled") "All" else "Scheduled" }
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ReminderCategoryCard(
                                title = "All",
                                count = allCount,
                                icon = Icons.Rounded.Inbox,
                                iconBg = Color(0xFF1C1C1E),
                                isSelected = selectedFilter == "All",
                                modifier = Modifier.weight(1f),
                                onClick = { selectedFilter = "All" }
                            )
                            ReminderCategoryCard(
                                title = "Completed",
                                count = completedCount,
                                icon = Icons.Rounded.CheckCircle,
                                iconBg = Color(0xFF34C759),
                                isSelected = selectedFilter == "Completed",
                                modifier = Modifier.weight(1f),
                                onClick = { selectedFilter = if (selectedFilter == "Completed") "All" else "Completed" }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = if (selectedFilter == "All") "MY REMINDERS" else selectedFilter.uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(start = 6.dp, top = 8.dp)
                    )
                }

                // Reminders list card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Column {
                            if (filteredList.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Tidak ada pengingat", color = Color.Gray, fontSize = 15.sp)
                                }
                            } else {
                                filteredList.forEachIndexed { index, item ->
                                    ReminderRow(
                                        reminder = item,
                                        onToggle = { viewModel.toggleReminderCompleted(context, item.id) },
                                        onDelete = { viewModel.deleteReminder(context, item.id) }
                                    )
                                    if (index < filteredList.size - 1) {
                                        HorizontalDivider(
                                            color = Color(0xFFE5E5EA),
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(start = 52.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bottom action bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showAddDialog = true }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AddCircle,
                        contentDescription = "New",
                        tint = Color(0xFF007AFF),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "New Reminder",
                        color = Color(0xFF007AFF),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "${reminders.count { !it.isCompleted }} items",
                    color = Color.Gray,
                    fontSize = 13.sp
                )
            }

            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = true
            )
        }

        // Add Reminder Dialog
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("New Reminder", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newTitle,
                            onValueChange = { newTitle = it },
                            label = { Text("Title") },
                            placeholder = { Text("e.g. Call client") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = newNotes,
                            onValueChange = { newNotes = it },
                            label = { Text("Notes (Optional)") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Today", "Scheduled", "Tomorrow").forEach { tag ->
                                FilterChip(
                                    selected = newDueDate == tag,
                                    onClick = { newDueDate = tag },
                                    label = { Text(tag) }
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (newTitle.isNotBlank()) {
                                viewModel.addReminder(context, newTitle, newNotes, newDueDate)
                                newTitle = ""
                                newNotes = ""
                                showAddDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                    ) {
                        Text("Add")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun ReminderCategoryCard(
    title: String,
    count: Int,
    icon: ImageVector,
    iconBg: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE5EEFF) else Color.White
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = count.toString(),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ReminderRow(
    reminder: ReminderItem,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Circle Checkbox
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (reminder.isCompleted) Color(0xFF007AFF) else Color.Transparent)
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (reminder.isCompleted) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(Color.Transparent)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Transparent,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF8E8E93)),
                            modifier = Modifier.fillMaxSize()
                        ) {}
                    }
                }
            }

            Column {
                Text(
                    text = reminder.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal,
                    color = if (reminder.isCompleted) Color.Gray else Color.Black,
                    textDecoration = if (reminder.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )
                if (reminder.notes.isNotBlank() || reminder.dueDate.isNotBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (reminder.dueDate.isNotBlank()) {
                            Text(
                                text = reminder.dueDate,
                                fontSize = 12.sp,
                                color = if (reminder.dueDate == "Today") Color(0xFF007AFF) else Color.Gray
                            )
                        }
                        if (reminder.notes.isNotBlank()) {
                            Text(
                                text = "• ${reminder.notes}",
                                fontSize = 12.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = "Delete",
                tint = Color.LightGray,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
