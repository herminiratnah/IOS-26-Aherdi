package com.example.ui.screens

import androidx.compose.animation.*
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import java.util.UUID
import com.example.ui.theme.*
import com.example.viewmodel.IOSViewModel

data class WalletCard(
    val id: String,
    val title: String,
    val subtitle: String,
    val balance: String,
    val last4: String,
    val gradientColors: List<Color>
)

data class TransactionItem(
    val merchant: String,
    val category: String,
    val amount: String,
    val date: String,
    val isIncome: Boolean = false
)

@Composable
fun WalletApp(
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

    var isNfcActive by remember { mutableStateOf(false) }
    var showAddCardDialog by remember { mutableStateOf(false) }

    val cards = remember {
        mutableStateListOf(
            WalletCard(
                "1", "Apple Card", "Titanium", "$1,450.80", "4242",
                listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5), Color(0xFFFFFFFF))
            ),
            WalletCard(
                "2", "Apple Cash", "Available Balance", "$240.00", "0192",
                listOf(Color(0xFF111111), Color(0xFF333333), Color(0xFF000000))
            ),
            WalletCard(
                "3", "Chase Sapphire", "Preferred", "$3,890.00", "8831",
                listOf(Color(0xFF0D47A1), Color(0xFF1976D2), Color(0xFF002171))
            ),
            WalletCard(
                "4", "MRT Jakarta Pass", "Transit", "Rp 75.000", "5512",
                listOf(Color(0xFF2E7D32), Color(0xFF388E3C), Color(0xFF1B5E20))
            )
        )
    }

    val transactions = listOf(
        TransactionItem("Apple Store", "Electronics", "-$129.00", "Today"),
        TransactionItem("Starbucks Reserve", "Food & Drink", "-$6.50", "Today"),
        TransactionItem("Daily Cash", "Rewards", "+$3.87", "Yesterday", true),
        TransactionItem("Grab Transport", "Transportation", "-$4.20", "Yesterday"),
        TransactionItem("Whole Foods", "Groceries", "-$54.30", "12 Jul")
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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
                isDarkIcons = false
            )

            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wallet",
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    onClick = { showAddCardDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2C2C2E))
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Card",
                        tint = Color.White
                    )
                }
            }

            if (isNfcActive) {
                // Apple Pay NFC reader active modal
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF34C759).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Contactless,
                                contentDescription = "Hold near reader",
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(50.dp)
                            )
                        }

                        Text(
                            text = "Hold Near Reader",
                            color = Color.White,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Double click side button or verify with Face ID",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )

                        Spacer(modifier = Modifier.height(30.dp))

                        Button(
                            onClick = {
                                isNfcActive = false
                                viewModel.showDynamicIslandNotification("Apple Pay", "Payment Approved $12.50", "apple_pay", 0xFF34C759)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF34C759)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Simulate NFC Tap", fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = { isNfcActive = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Cards Stack
                    items(cards) { card ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .shadow(6.dp, RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.linearGradient(card.gradientColors))
                                .clickable {
                                    isNfcActive = true
                                    viewModel.showDynamicIslandNotification("Apple Pay Ready", card.title, "wallet")
                                }
                                .padding(18.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = card.title,
                                        color = if (card.id == "1") Color.Black else Color.White,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.Contactless,
                                        contentDescription = null,
                                        tint = if (card.id == "1") Color.Black else Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Column {
                                        Text(
                                            text = card.subtitle,
                                            color = if (card.id == "1") Color.DarkGray else Color.Gray,
                                            fontSize = 11.sp
                                        )
                                        Text(
                                            text = card.balance,
                                            color = if (card.id == "1") Color.Black else Color.White,
                                            fontSize = 22.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "•••• ${card.last4}",
                                        color = if (card.id == "1") Color.DarkGray else Color.Gray,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    // Transactions Section
                    item {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "LATEST TRANSACTIONS",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    items(transactions) { tx ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF1C1C1E))
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2C2C2E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (tx.isIncome) Icons.Rounded.ArrowDownward else Icons.Rounded.ShoppingBag,
                                    contentDescription = null,
                                    tint = if (tx.isIncome) Color(0xFF34C759) else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.merchant,
                                    color = Color.White,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "${tx.category} • ${tx.date}",
                                    color = Color.Gray,
                                    fontSize = 12.sp
                                )
                            }

                            Text(
                                text = tx.amount,
                                color = if (tx.isIncome) Color(0xFF34C759) else Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            if (showAddCardDialog) {
                AlertDialog(
                    onDismissRequest = { showAddCardDialog = false },
                    title = { Text("Add Card to Apple Pay", color = Color.White, fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Position your credit or debit card in the camera frame, or enter details manually.", color = Color.Gray, fontSize = 13.sp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(100.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF2C2C2E)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                cards.add(
                                    WalletCard(
                                        UUID.randomUUID().toString(),
                                        "Bank Mandiri Visa",
                                        "Debit",
                                        "Rp 12.500.000",
                                        "1129",
                                        listOf(Color(0xFFB71C1C), Color(0xFFD32F2F), Color(0xFF7F0000))
                                    )
                                )
                                showAddCardDialog = false
                                viewModel.showDynamicIslandNotification("Card Added", "Bank Mandiri Visa ready for Apple Pay", "card")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF))
                        ) {
                            Text("Add Card")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAddCardDialog = false }) {
                            Text("Cancel", color = Color.Gray)
                        }
                    },
                    containerColor = Color(0xFF1C1C1E)
                )
            }

            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = false
            )
        }
    }
}
