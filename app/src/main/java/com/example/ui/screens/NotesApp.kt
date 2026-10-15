package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.NoteItem
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.*
import com.example.util.IOSFeedbackHelper
import com.example.viewmodel.IOSViewModel
import kotlinx.coroutines.delay

@Composable
fun NotesApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
    var searchQuery by remember { mutableStateOf("") }

    // Enhanced Note Tools
    var isDrawingOpen by remember { mutableStateOf(false) }
    var isRecordingAudio by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }
    var hasAudioRecording by remember { mutableStateOf(false) }
    var isAudioPlaying by remember { mutableStateOf(false) }
    var isChecklistMode by remember { mutableStateOf(false) }
    val checklistItems = remember { mutableStateListOf<Pair<String, Boolean>>() }

    // Live Audio Recording Timer
    LaunchedEffect(isRecordingAudio) {
        if (isRecordingAudio) {
            recordingSeconds = 0
            while (isRecordingAudio) {
                delay(1000)
                recordingSeconds++
            }
        }
    }

    val filteredNotes = remember(notes, searchQuery) {
        if (searchQuery.isBlank()) notes
        else notes.filter { it.title.contains(searchQuery, ignoreCase = true) || it.body.contains(searchQuery, ignoreCase = true) }
    }

    BackHandler(enabled = editingNote != null) {
        editingNote = null
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Spacer for Single Global Status Bar
            Spacer(modifier = Modifier.height(44.dp))

            if (editingNote != null) {
                // ================= NOTE EDITOR VIEW =================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // Top Bar in Editor
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable {
                                IOSFeedbackHelper.vibrateLight(context)
                                viewModel.updateNote(editingNote!!.id, editTitle, editBody)
                                editingNote = null
                            }
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = null, tint = IOSYellow, modifier = Modifier.size(26.dp))
                            Text("Catatan", color = IOSYellow, fontSize = 17.sp)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // AirDrop / Share Sheet
                            IconButton(onClick = {
                                IOSFeedbackHelper.vibrateLight(context)
                                viewModel.openShareSheet(title = editTitle)
                            }) {
                                Icon(Icons.Rounded.Share, contentDescription = "Bagikan", tint = IOSYellow, modifier = Modifier.size(20.dp))
                            }

                            // Delete Note
                            IconButton(onClick = {
                                IOSFeedbackHelper.vibrateMedium(context)
                                viewModel.deleteNote(editingNote!!.id)
                                editingNote = null
                            }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Hapus", tint = Color(0xFFFF3B30), modifier = Modifier.size(20.dp))
                            }

                            Button(
                                onClick = {
                                    IOSFeedbackHelper.vibrateLight(context)
                                    viewModel.updateNote(editingNote!!.id, editTitle, editBody)
                                    editingNote = null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IOSYellow),
                                shape = RoundedCornerShape(16.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp)
                            ) {
                                Text("Selesai", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Editor Toolbar (Checklist, Drawing, Voice Memo)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(IOSSystemGray6)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checklist button
                        IconButton(onClick = {
                            isChecklistMode = !isChecklistMode
                            if (checklistItems.isEmpty()) {
                                checklistItems.add("Tugas pertama" to false)
                                checklistItems.add("Tugas kedua" to true)
                                checklistItems.add("Tinjau catatan bersama tim" to false)
                            }
                        }) {
                            Icon(
                                Icons.Rounded.CheckCircleOutline,
                                contentDescription = "Daftar Tugas",
                                tint = if (isChecklistMode) IOSYellow else Color.Gray
                            )
                        }

                        // Audio Recording / Voice Memo
                        IconButton(onClick = {
                            IOSFeedbackHelper.vibrateLight(context)
                            if (isRecordingAudio) {
                                isRecordingAudio = false
                                hasAudioRecording = true
                                viewModel.showDynamicIslandNotification("Catatan Suara", "Rekaman disimpan (${recordingSeconds}s)", "mic", 0xFFFFCC00)
                            } else {
                                isRecordingAudio = true
                            }
                        }) {
                            Icon(
                                Icons.Rounded.Mic,
                                contentDescription = "Rekam Suara",
                                tint = if (isRecordingAudio) Color(0xFFFF3B30) else Color.Gray
                            )
                        }

                        // Pencil / Drawing Canvas
                        IconButton(onClick = {
                            IOSFeedbackHelper.vibrateLight(context)
                            isDrawingOpen = true
                        }) {
                            Icon(Icons.Rounded.Draw, contentDescription = "Gambar", tint = Color.Gray)
                        }
                    }

                    // Recording Banner
                    AnimatedVisibility(visible = isRecordingAudio) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFF3B30).copy(alpha = 0.12f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFFF3B30)))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Merekam Memo Suara... 00:${String.format("%02d", recordingSeconds)}", color = Color(0xFFFF3B30), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("Ketuk mikrofon untuk berhenti", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    // Embedded Voice Note Card
                    if (hasAudioRecording) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF2F2F7))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(IOSYellow)
                                        .clickable { isAudioPlaying = !isAudioPlaying },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isAudioPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Memo Audio Rekaman", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text(if (isAudioPlaying) "Memutar audio..." else "Durasi 00:${String.format("%02d", recordingSeconds.coerceAtLeast(4))}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = IOSYellow, modifier = Modifier.size(28.dp))
                        }
                    }

                    // Title
                    TextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        placeholder = { Text("Judul", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Gray) },
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                    )

                    // Interactive Checklist items
                    if (isChecklistMode) {
                        Column(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            checklistItems.forEachIndexed { idx, item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            IOSFeedbackHelper.vibrateLight(context)
                                            checklistItems[idx] = item.first to !item.second
                                        },
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .border(1.5.dp, if (item.second) IOSYellow else Color.Gray, CircleShape)
                                            .background(if (item.second) IOSYellow else Color.Transparent),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (item.second) {
                                            Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = item.first,
                                        fontSize = 15.sp,
                                        color = if (item.second) Color.Gray else Color.Black,
                                        textDecoration = if (item.second) TextDecoration.LineThrough else null
                                    )
                                }
                            }
                        }
                    }

                    // Body
                    TextField(
                        value = editBody,
                        onValueChange = { editBody = it },
                        placeholder = { Text("Tulis isi catatan di sini...", fontSize = 16.sp, color = Color.Gray) },
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, lineHeight = 22.sp, color = Color.Black)
                    )
                }
            } else {
                // ================= NOTES LIST VIEW =================
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Catatan",
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .clickable {
                                    val newNote = viewModel.createNote("Catatan Baru", "")
                                    editingNote = newNote
                                    editTitle = "Catatan Baru"
                                    editBody = ""
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Tambah Catatan", tint = IOSYellow, modifier = Modifier.size(22.dp))
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari catatan...", color = Color.Gray, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Hapus", tint = Color.Gray, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFFE5E5EA),
                            unfocusedContainerColor = Color(0xFFE5E5EA),
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // iCloud Category Pill
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(Icons.Rounded.CloudQueue, contentDescription = null, tint = IOSYellow, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("iCloud", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                    }

                    // Notes Cards
                    if (filteredNotes.isEmpty()) {
                        Box(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Tidak ada catatan", color = Color.Gray, fontSize = 16.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                        ) {
                            items(filteredNotes, key = { it.id }) { note ->
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            editingNote = note
                                            editTitle = note.title
                                            editBody = note.body
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Text(
                                        text = note.title.ifEmpty { "Tanpa Judul" },
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = note.dateLabel,
                                            fontSize = 13.sp,
                                            color = Color.Gray
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = note.body.ifEmpty { "Tidak ada teks tambahan" },
                                            fontSize = 13.sp,
                                            color = Color.Gray.copy(alpha = 0.8f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                HorizontalDivider(color = IOSSystemGray5, thickness = 0.5.dp, modifier = Modifier.padding(start = 16.dp))
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
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(24.dp))
                Text(
                    text = "${filteredNotes.size} Catatan",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = "Catatan Baru",
                    tint = IOSYellow,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            val newNote = viewModel.createNote("Catatan Baru", "")
                            editingNote = newNote
                            editTitle = "Catatan Baru"
                            editBody = ""
                        }
                )
            }

            // Home indicator
            HomeBar(
                onGoHome = { viewModel.closeApp() },
                onOpenAppSwitcher = { viewModel.openAppSwitcher() },
                isDark = true
            )
        }

        // Drawing Canvas Modal
        if (isDrawingOpen) {
            NotesDrawingDialog(
                onDismiss = { isDrawingOpen = false },
                onSave = {
                    isDrawingOpen = false
                    editBody += "\n[Sketsa Gambar Disisipkan]"
                }
            )
        }
    }
}

// iOS Pencil / Sketch Dialog
@Composable
fun NotesDrawingDialog(
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    val paths = remember { mutableStateListOf<Pair<Path, Color>>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    var selectedColor by remember { mutableStateOf(Color.Black) }

    val colors = listOf(Color.Black, Color(0xFFFF9500), Color(0xFF007AFF), Color(0xFFFF3B30), Color(0xFF34C759))

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Sketsa Catatan", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 18.sp)
                TextButton(onClick = { paths.clear() }) {
                    Text("Hapus Semua", color = Color(0xFFFF3B30), fontSize = 12.sp)
                }
            }
        },
        text = {
            Column {
                // Color Palette
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    colors.forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(c)
                                .border(if (selectedColor == c) 2.5.dp else 0.dp, IOSYellow, CircleShape)
                                .clickable { selectedColor = c }
                        )
                    }
                }

                // Drawing Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF9F9FB))
                        .border(1.dp, Color.LightGray, RoundedCornerShape(12.dp))
                        .pointerInput(selectedColor) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val path = Path().apply { moveTo(offset.x, offset.y) }
                                    currentPath = path
                                    paths.add(path to selectedColor)
                                },
                                onDrag = { change, _ ->
                                    currentPath?.lineTo(change.position.x, change.position.y)
                                },
                                onDragEnd = {
                                    currentPath = null
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        paths.forEach { (path, col) ->
                            drawPath(
                                path = path,
                                color = col,
                                style = Stroke(width = 5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = IOSYellow)
            ) {
                Text("Sisipkan", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.Gray)
            }
        }
    )
}
