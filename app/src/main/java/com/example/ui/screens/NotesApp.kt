package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import com.example.model.NoteItem
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

@Composable
fun NotesApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val currentTime by viewModel.currentTimeFormatted.collectAsState()
    val batteryLevel by viewModel.batteryLevel.collectAsState()
    val isBatteryCharging by viewModel.isBatteryCharging.collectAsState()
    val isWifiConnected by viewModel.isWifiConnected.collectAsState()
    val wifiSignalLevel by viewModel.wifiSignalLevel.collectAsState()
    val cellularBars by viewModel.cellularSignalBars.collectAsState()
    val networkType by viewModel.networkType.collectAsState()

    var editingNote by remember { mutableStateOf<NoteItem?>(null) }
    var editTitle by remember { mutableStateOf("") }
    var editBody by remember { mutableStateOf("") }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
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
                isDarkIcons = true
            )

            if (editingNote != null) {
                // Note Editor View
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                viewModel.updateNote(editingNote!!.id, editTitle, editBody)
                                editingNote = null
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null, tint = IOSYellow, modifier = Modifier.size(24.dp))
                            Text("Notes", color = IOSYellow, fontSize = 17.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.updateNote(editingNote!!.id, editTitle, editBody)
                                editingNote = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IOSYellow)
                        ) {
                            Text("Done", color = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    TextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        placeholder = { Text("Title", fontSize = 24.sp, fontWeight = FontWeight.Bold) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    )

                    TextField(
                        value = editBody,
                        onValueChange = { editBody = it },
                        placeholder = { Text("Note content...", fontSize = 16.sp) },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 22.sp)
                    )
                }
            } else {
                // Notes List View matching Image 11
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    // Header with Folders link
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
                                tint = IOSYellow,
                                modifier = Modifier.size(24.dp)
                            )
                            Text("Folders", color = IOSYellow, fontSize = 17.sp)
                        }
                        Icon(
                            imageVector = Icons.Rounded.MoreHoriz,
                            contentDescription = "More",
                            tint = IOSYellow,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Title "Notes"
                    Text(
                        text = "Notes",
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
                            .background(Color(0xFFE5E5EA))
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

                    // Notes Group Card
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                    ) {
                        LazyColumn {
                            items(notes) { note ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editingNote = note
                                            editTitle = note.title
                                            editBody = note.snippet
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = note.title,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = note.date,
                                            fontSize = 14.sp,
                                            color = IOSSystemGray
                                        )
                                        Text(
                                            text = note.snippet,
                                            fontSize = 14.sp,
                                            color = Color.Gray,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                HorizontalDivider(
                                    color = IOSSystemGray5,
                                    thickness = 0.5.dp,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Bar with Note Count and New Note button
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
                    text = "${notes.size} Notes",
                    fontSize = 11.sp,
                    color = IOSSystemGray
                )
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "New Note",
                    tint = IOSYellow,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            val newNote = viewModel.createNote("New Note", "")
                            editingNote = newNote
                            editTitle = "New Note"
                            editBody = ""
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
