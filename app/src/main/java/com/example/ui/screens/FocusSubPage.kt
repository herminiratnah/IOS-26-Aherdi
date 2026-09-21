package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.FocusMode
import com.example.ui.theme.*
import com.example.util.IOSFeedbackHelper
import com.example.viewmodel.IOSViewModel

@Composable
fun FocusSubPage(
    viewModel: IOSViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val currentFocus by viewModel.currentFocusMode.collectAsState()
    var shareAcrossDevices by remember { mutableStateOf(true) }

    val focusModes = remember {
        listOf(
            FocusMode.DO_NOT_DISTURB,
            FocusMode.WORK,
            FocusMode.PERSONAL,
            FocusMode.SLEEP
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        // iOS Navigation Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable { onBack() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = IOSBlue,
                    modifier = Modifier.size(20.dp)
                )
                Text(text = "Pengaturan", color = IOSBlue, fontSize = 17.sp)
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Fokus",
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(60.dp))
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header Description
            item {
                Text(
                    text = "Fokus membantu Anda tetap berkonsentrasi saat Anda membutuhkan waktu untuk menjauh dari perangkat.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            // Focus Mode Selection Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                ) {
                    Column {
                        // OFF Option
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    IOSFeedbackHelper.vibrateLight(context)
                                    viewModel.setFocusMode(FocusMode.OFF, context)
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF8E8E93)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.NotificationsOff,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text("Fokus Mati", fontSize = 16.sp, color = Color.Black)
                            }
                            if (currentFocus == FocusMode.OFF) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = "Active",
                                    tint = IOSBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                        focusModes.forEachIndexed { index, mode ->
                            val isActive = currentFocus == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        IOSFeedbackHelper.vibrateLight(context)
                                        viewModel.setFocusMode(mode, context)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color(mode.colorLong)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when (mode) {
                                                FocusMode.DO_NOT_DISTURB -> Icons.Rounded.NightlightRound
                                                FocusMode.WORK -> Icons.Rounded.Work
                                                FocusMode.PERSONAL -> Icons.Rounded.Person
                                                FocusMode.SLEEP -> Icons.Rounded.Bedtime
                                                else -> Icons.Rounded.Notifications
                                            },
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Column {
                                        Text(mode.label, fontSize = 16.sp, color = Color.Black, fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal)
                                        Text(
                                            text = if (isActive) "Sedang Aktif" else "Ketuk untuk mengaktifkan",
                                            fontSize = 12.sp,
                                            color = if (isActive) Color(mode.colorLong) else Color.Gray
                                        )
                                    }
                                }
                                if (isActive) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = "Active",
                                        tint = Color(mode.colorLong),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            if (index < focusModes.size - 1) {
                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                            }
                        }
                    }
                }
            }

            // Sync Settings Card
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Bagikan di Semua Perangkat", fontSize = 16.sp, color = Color.Black)
                                Text("Mengaktifkan Fokus di perangkat ini akan mengaktifkannya di perangkat Anda lainnya.", fontSize = 12.sp, color = Color.Gray)
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Switch(
                                checked = shareAcrossDevices,
                                onCheckedChange = { shareAcrossDevices = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IOSGreen)
                            )
                        }
                    }
                }
            }
        }
    }
}
