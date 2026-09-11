package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.HomeBar
import com.example.ui.components.IOSStatusBar
import com.example.ui.theme.IOSOrange
import com.example.viewmodel.IOSViewModel

@Composable
fun CalculatorApp(
    viewModel: IOSViewModel,
    modifier: Modifier = Modifier
) {
    val dynamicIslandData by viewModel.dynamicIsland.collectAsState()

    var displayText by remember { mutableStateOf("0") }
    var previousValue by remember { mutableStateOf<Double?>(null) }
    var currentOperation by remember { mutableStateOf<String?>(null) }
    var isNewEntry by remember { mutableStateOf(true) }

    fun onNumber(digit: String) {
        if (isNewEntry || displayText == "0") {
            displayText = digit
            isNewEntry = false
        } else {
            if (displayText.length < 9) {
                displayText += digit
            }
        }
    }

    fun onOperator(op: String) {
        val current = displayText.toDoubleOrNull() ?: 0.0
        if (previousValue == null) {
            previousValue = current
        } else if (currentOperation != null) {
            val result = when (currentOperation) {
                "+" -> previousValue!! + current
                "-" -> previousValue!! - current
                "×" -> previousValue!! * current
                "÷" -> if (current != 0.0) previousValue!! / current else 0.0
                else -> current
            }
            previousValue = result
            displayText = if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
        }
        currentOperation = op
        isNewEntry = true
    }

    fun onEquals() {
        val current = displayText.toDoubleOrNull() ?: 0.0
        if (previousValue != null && currentOperation != null) {
            val result = when (currentOperation) {
                "+" -> previousValue!! + current
                "-" -> previousValue!! - current
                "×" -> previousValue!! * current
                "÷" -> if (current != 0.0) previousValue!! / current else 0.0
                else -> current
            }
            displayText = if (result % 1.0 == 0.0) result.toLong().toString() else result.toString()
            previousValue = null
            currentOperation = null
            isNewEntry = true
        }
    }

    fun onClear() {
        displayText = "0"
        previousValue = null
        currentOperation = null
        isNewEntry = true
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Status bar
            IOSStatusBar(
                dynamicIslandData = dynamicIslandData,
                onDismissDynamicIsland = { viewModel.dismissDynamicIsland() },
                onOpenControlCenter = { viewModel.toggleControlCenter() },
                onOpenNotificationCenter = { viewModel.toggleNotificationCenter() },
                isDarkIcons = false
            )

            // Number Display Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Text(
                    text = displayText,
                    color = Color.White,
                    fontSize = if (displayText.length > 7) 50.sp else 74.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.End,
                    maxLines = 1
                )
            }

            // Keypad Grid
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Row 1: AC, +/-, %, ÷
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CalcKey(text = if (displayText != "0") "C" else "AC", bg = Color(0xFFA5A5A5), textColor = Color.Black) { onClear() }
                    CalcKey(text = "±", bg = Color(0xFFA5A5A5), textColor = Color.Black) {
                        val v = displayText.toDoubleOrNull() ?: 0.0
                        displayText = (-v).toString().removeSuffix(".0")
                    }
                    CalcKey(text = "%", bg = Color(0xFFA5A5A5), textColor = Color.Black) {
                        val v = displayText.toDoubleOrNull() ?: 0.0
                        displayText = (v / 100.0).toString()
                    }
                    CalcKey(text = "÷", bg = IOSOrange, isSelected = currentOperation == "÷") { onOperator("÷") }
                }

                // Row 2: 7, 8, 9, ×
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CalcKey(text = "7") { onNumber("7") }
                    CalcKey(text = "8") { onNumber("8") }
                    CalcKey(text = "9") { onNumber("9") }
                    CalcKey(text = "×", bg = IOSOrange, isSelected = currentOperation == "×") { onOperator("×") }
                }

                // Row 3: 4, 5, 6, -
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CalcKey(text = "4") { onNumber("4") }
                    CalcKey(text = "5") { onNumber("5") }
                    CalcKey(text = "6") { onNumber("6") }
                    CalcKey(text = "−", bg = IOSOrange, isSelected = currentOperation == "-") { onOperator("-") }
                }

                // Row 4: 1, 2, 3, +
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    CalcKey(text = "1") { onNumber("1") }
                    CalcKey(text = "2") { onNumber("2") }
                    CalcKey(text = "3") { onNumber("3") }
                    CalcKey(text = "+", bg = IOSOrange, isSelected = currentOperation == "+") { onOperator("+") }
                }

                // Row 5: 0, ., =
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    // Wide 0 Key
                    Box(
                        modifier = Modifier
                            .width(162.dp)
                            .height(76.dp)
                            .clip(RoundedCornerShape(38.dp))
                            .background(Color(0xFF333333))
                            .clickable { onNumber("0") }
                            .padding(start = 28.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("0", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Normal)
                    }
                    CalcKey(text = ".") {
                        if (!displayText.contains(".")) {
                            displayText += "."
                            isNewEntry = false
                        }
                    }
                    CalcKey(text = "=", bg = IOSOrange) { onEquals() }
                }
            }

            // Home indicator
            HomeBar(
                onGoHome = { viewModel.closeApp() },
                isDark = false
            )
        }
    }
}

@Composable
fun CalcKey(
    text: String,
    bg: Color = Color(0xFF333333),
    textColor: Color = Color.White,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val keyBg = if (isSelected) Color.White else bg
    val keyText = if (isSelected) IOSOrange else textColor

    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .background(keyBg)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = keyText,
            fontSize = 32.sp,
            fontWeight = FontWeight.Normal
        )
    }
}
