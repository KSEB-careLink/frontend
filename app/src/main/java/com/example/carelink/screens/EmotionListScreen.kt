package com.example.carelink.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.carelink.viewmodel.EmotionViewModel
import androidx.compose.foundation.shape.RoundedCornerShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmotionListScreen(
    navController: NavHostController,
    viewModel: EmotionViewModel
) {
    val emotionMap by viewModel.emotionRecords.collectAsState()
    val backgroundColor = Color(0xFFE6F4EA)

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Text(
                            text = "감정 기록 보기",
                            color = Color(0xFF2C5F2D),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 1.sp,

                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = backgroundColor
                    )
                )
                Divider(
                    color = Color.Black,
                    thickness = 1.dp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        containerColor = backgroundColor
    ) { padding ->
        LazyColumn(
            contentPadding = padding,
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            if (emotionMap.isNotEmpty()) {
                item {
                    EmotionGraph(emotionMap)
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            if (emotionMap.isEmpty()) {
                item {
                    Text(
                        "감정 기록이 없습니다.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFF4C4C4C)
                        )
                    )
                }
            } else {
                items(emotionMap.toList().sortedByDescending { it.first }) { (date, emoji) ->
                    EmotionRecordCard(date, emoji)
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun EmotionRecordCard(date: String, emoji: String) {
    val (cardColor, textColor) = when (emoji) {
        "😊", "😁", "😄" -> Color(0xFFFFF9C4) to Color(0xFF795548)
        "😢", "😭", "☹️" -> Color(0xFFB2EBF2) to Color(0xFF01579B)
        "😡", "😠" -> Color(0xFFFFCDD2) to Color(0xFFB71C1C)
        "😐", "😶" -> Color(0xFFE0E0E0) to Color(0xFF616161)
        else -> Color(0xFFD8F3DC) to Color(0xFF1B4332)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CalendarMonth,
                contentDescription = "날짜 아이콘",
                tint = textColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "$date : $emoji",
                fontSize = 18.sp,
                color = textColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

// 감정 점수 매핑 함수
fun mapEmojiToScore(emoji: String): Int {
    return when (emoji) {
        "😊", "😁", "😄" -> 5
        "😐", "😶" -> 3
        "😢", "😭", "☹️" -> 1
        "😡", "😠" -> 2
        else -> 3
    }
}

// 감정 그래프 UI 함수
@Composable
fun EmotionGraph(emotionMap: Map<String, String>) {
    val sorted = emotionMap.toList().sortedBy { it.first }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "감정 변화 그래프",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF2C5F2D),
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .background(Color.White, shape = RoundedCornerShape(12.dp))
                .padding(8.dp)
        ) {
            val pointCount = sorted.size
            if (pointCount < 2) return@Canvas

            val maxScore = 5f
            val widthPerPoint = size.width / (pointCount - 1)
            val heightPerScore = size.height / maxScore

            val points = sorted.mapIndexed { index, (_, emoji) ->
                val score = mapEmojiToScore(emoji).toFloat()
                Offset(
                    x = index * widthPerPoint,
                    y = size.height - (score * heightPerScore)
                )
            }

            for (i in 0 until points.lastIndex) {
                drawLine(
                    color = Color(0xFF2C5F2D),
                    start = points[i],
                    end = points[i + 1],
                    strokeWidth = 4f
                )
            }

            // 점 찍기
            points.forEach { point ->
                drawCircle(
                    color = Color(0xFF66BB6A),
                    radius = 6f,
                    center = point
                )
            }

            // 점선 그리기 (가이드 라인)
            for (i in 1 until 5) {
                val y = i * heightPerScore
                drawLine(
                    color = Color.LightGray,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                )
            }
        }
    }
}





