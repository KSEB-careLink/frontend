package com.example.myapplication.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.delay

@Composable
fun AutoScrollingText(
    text: String,
    modifier: Modifier = Modifier,
    scrollSpeedPxPerSecond: Float = 30f,
    autoScroll: Boolean = true // ← 새 인자 추가
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .verticalScroll(scrollState)
            .background(Color(0xFFCCCCCC), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(
            text = text,
            fontSize = 16.sp,
            color = Color.White
        )
    }

    if (autoScroll) {
        LaunchedEffect(Unit) {
            while (true) {
                val maxScroll = scrollState.maxValue
                val current = scrollState.value
                val target = (current + scrollSpeedPxPerSecond).toInt().coerceAtMost(maxScroll)

                scrollState.animateScrollTo(target)

                if (target == maxScroll) {
                    delay(1000)
                    scrollState.animateScrollTo(0)
                }
                delay(1000)
            }
        }
    }
}


