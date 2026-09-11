package com.example.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeBar(
    onGoHome: () -> Unit,
    onHoldForSiri: (() -> Unit)? = null,
    isDark: Boolean = false,
    modifier: Modifier = Modifier
) {
    val barColor = if (isDark) Color(0x99000000) else Color(0xCCFFFFFF)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(bottom = 6.dp, top = 6.dp)
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    // Swipe up to go home!
                    if (dragAmount < -15) {
                        onGoHome()
                    }
                }
            }
            .combinedClickable(
                onClick = onGoHome,
                onLongClick = { onHoldForSiri?.invoke() }
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(135.dp)
                .height(4.5.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(barColor)
        )
    }
}
