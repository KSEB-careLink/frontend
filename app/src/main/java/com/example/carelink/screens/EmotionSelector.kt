package com.example.carelink.screens

import android.widget.Toast
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun EmotionSelector() {
    val context = LocalContext.current

    val emotions = listOf("😊", "😐", "😢", "😡", "😴")

    Row(modifier = Modifier.padding(16.dp)) {
        emotions.forEach { emoji ->
            Button(
                onClick = {
                    Toast.makeText(context, "오늘 기분이 기록되었습니다", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text(text = emoji)
            }
        }
    }
}
