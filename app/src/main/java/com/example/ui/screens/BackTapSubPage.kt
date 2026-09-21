package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.model.BackTapAction
import com.example.ui.theme.*
import com.example.util.IOSFeedbackHelper
import com.example.viewmodel.IOSViewModel

@Composable
fun BackTapSubPage(
    viewModel: IOSViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val enabled by viewModel.backTapEnabled.collectAsState()
    val doubleAction by viewModel.backTapDoubleAction.collectAsState()
    val tripleAction by viewModel.backTapTripleAction.collectAsState()

    val availableActions = remember {
        listOf(
            BackTapAction.FLASHLIGHT,
            BackTapAction.SCREENSHOT,
            BackTapAction.CONTROL_CENTER,
            BackTapAction.NOTIFICATION_CENTER,
            BackTapAction.LOCK_SCREEN,
            BackTapAction.MUTE,
            BackTapAction.NONE
        )
    }

    var activePicker by remember { mutableStateOf<String?>(null) } // "double" or "triple" or null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(IOSSystemGray6)
    ) {
        // Navigation TopBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.clickable {
                    if (activePicker != null) activePicker = null else onBack()
                },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBackIosNew,
                    contentDescription = "Back",
                    tint = IOSBlue,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (activePicker != null) "Ketuk Belakang" else "Aksesibilitas",
                    color = IOSBlue,
                    fontSize = 17.sp
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = when (activePicker) {
                    "double" -> "Ketuk Dua Kali"
                    "triple" -> "Ketuk Tiga Kali"
                    else -> "Ketuk Belakang"
                },
                color = Color.Black,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(60.dp))
        }

        if (activePicker == "double" || activePicker == "triple") {
            // Action Picker List
            val currentSelected = if (activePicker == "double") doubleAction else tripleAction
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                    ) {
                        Column {
                            availableActions.forEachIndexed { index, action ->
                                val isSelected = currentSelected == action
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            IOSFeedbackHelper.vibrateLight(context)
                                            if (activePicker == "double") {
                                                viewModel.setBackTapDoubleAction(action, context)
                                            } else {
                                                viewModel.setBackTapTripleAction(action, context)
                                            }
                                            activePicker = null
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(action.label, fontSize = 16.sp, color = Color.Black)
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = "Selected",
                                            tint = IOSBlue,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (index < availableActions.size - 1) {
                                    HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Main Back Tap Menu
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Ketuk dua kali atau tiga kali pada bagian belakang ponsel Anda untuk melakukan tindakan dengan cepat.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                    )
                }

                // Enable Switch
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Ketuk Bagian Belakang", fontSize = 16.sp, color = Color.Black)
                            Switch(
                                checked = enabled,
                                onCheckedChange = {
                                    IOSFeedbackHelper.vibrateLight(context)
                                    viewModel.setBackTapEnabled(it, context)
                                },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = IOSGreen)
                            )
                        }
                    }
                }

                if (enabled) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                        ) {
                            Column {
                                // Double Tap Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activePicker = "double" }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Ketuk Dua Kali", fontSize = 16.sp, color = Color.Black)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(doubleAction.label, fontSize = 16.sp, color = Color.Gray)
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = Color(0xFFC7C7CC),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = IOSBorderGray, thickness = 0.5.dp)

                                // Triple Tap Row
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { activePicker = "triple" }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Ketuk Tiga Kali", fontSize = 16.sp, color = Color.Black)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(tripleAction.label, fontSize = 16.sp, color = Color.Gray)
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowForwardIos,
                                            contentDescription = null,
                                            tint = Color(0xFFC7C7CC),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Test Simulator Button Card
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .clickable {
                                    IOSFeedbackHelper.vibrateMedium(context)
                                    viewModel.triggerBackTapAction(doubleAction, context)
                                }
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Uji Coba Ketukan Sekarang (${doubleAction.label})",
                                color = IOSBlue,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
