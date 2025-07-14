package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import kotlinx.coroutines.delay
import androidx.compose.material.icons.filled.Star

@Composable
fun Patient_Quiz(navController: NavController) {
    // — 레이아웃 값들 —
    val speakerGap  = 16.dp
    val greyGap     = 16.dp
    val greyHeight  = 450.dp
    val greyCorner  = 12.dp
    val questionGap = 16.dp
    val optionGap   = 12.dp

    // — 문제/정답 데이터 —
    val options       = listOf("냉면", "비빔밥", "떡볶이", "칼국수")
    val correctAnswer = "냉면"

    // — 상태들 —
    var selected     by remember { mutableStateOf<String?>(null) }
    var showResult   by remember { mutableStateOf(false) }
    var elapsedTime  by remember { mutableStateOf(0L) }       // 초 단위 타이머
    var questionTime by remember { mutableStateOf<Long?>(null) } // 제출 시 타임

    // 타이머: showResult==false 일 때만 1초마다 갱신
    LaunchedEffect(showResult) {
        if (!showResult) {
            elapsedTime = 0L
            val start = System.currentTimeMillis()
            while (true) {
                delay(1000)
                elapsedTime = (System.currentTimeMillis() - start) / 1000
            }
        }
    }

    // 네비게이션 바 상태
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            val navColors = NavigationBarItemDefaults.colors(
                indicatorColor      = Color.Transparent,
                selectedIconColor   = Color(0xFF00C4B4),
                unselectedIconColor = Color(0xFF888888),
                selectedTextColor   = Color(0xFF00C4B4),
                unselectedTextColor = Color(0xFF888888)
            )
            NavigationBar {
                listOf(
                    "sentence" to "회상문장",
                    "quiz"     to "회상퀴즈",
                    "alert"    to "긴급알림"
                ).forEach { (route, label) ->
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Star, contentDescription = label) },
                        label = { Text(label) },
                        selected = currentRoute == route,
                        onClick = {
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        },
                        colors = navColors
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // ───────── 타이머 ─────────
            if (!showResult) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Timer, contentDescription = "타이머", modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = String.format("%02d:%02d", elapsedTime / 60, elapsedTime % 60),
                        fontSize = 20.sp
                    )
                }
                Spacer(Modifier.height(speakerGap))
            }

            if (!showResult) {
                // ───────── 질문 화면 ─────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = "소리", modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "작년 봄, 손녀와 함께 전주에서 특별한 음식을 먹었을 때의 사진이네요!",
                        fontSize = 20.sp,
                        lineHeight = 24.sp
                    )
                }

                Spacer(Modifier.height(greyGap))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(greyHeight)
                        .background(Color(0xFFEDE9F5), RoundedCornerShape(greyCorner))
                )

                Spacer(Modifier.height(questionGap))

                Text("무엇을 드셨을까요?", fontSize = 28.sp, color = Color(0xFF00C4B4))

                Spacer(Modifier.height(questionGap))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(optionGap)
                ) {
                    options.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(optionGap)
                        ) {
                            rowItems.forEach { text ->
                                OptionButton(
                                    text = text,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selected = text
                                        questionTime = elapsedTime
                                        showResult = true
                                    }
                                )
                            }
                        }
                    }
                }

            } else {
                // ───────── 결과 화면 ─────────
                Spacer(Modifier.height(100.dp))
                val isCorrect = selected == correctAnswer

                Text(
                    text = if (isCorrect) "정답이에요!" else "정답이 아니에요!",
                    fontSize = 32.sp,
                    color = if (isCorrect) Color(0xFF00A651) else Color(0xFFE2101A)
                )
                Spacer(Modifier.height(16.dp))

                if (questionTime != null) {
                    Text("풀이 시간: ${questionTime}초", fontSize = 18.sp)
                    Spacer(Modifier.height(16.dp))
                }

                Image(
                    painter = painterResource(if (isCorrect) R.drawable.ch else R.drawable.wr),
                    contentDescription = null,
                    modifier = Modifier.size(300.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    text = if (isCorrect) "정말 잘 기억하셨어요😊" else "다시 기억해볼까요?",
                    fontSize = 20.sp
                )

                Spacer(Modifier.height(questionGap))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(optionGap)
                ) {
                    options.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(optionGap)
                        ) {
                            rowItems.forEach { text ->
                                OptionButton(
                                    text = text,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selected = text
                                        questionTime = elapsedTime
                                        showResult = true
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (isCorrect) {
                            // TODO: 다음 문제로
                        } else {
                            selected = null
                            showResult = false
                        }
                    },
                    modifier = Modifier
                        .width(130.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                ) {
                    Text(if (isCorrect) "다음 문제로" else "다시 풀기", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun OptionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
    ) {
        Text(text, color = Color.White)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPatient_Quiz() {
    Patient_Quiz(navController = rememberNavController())
}









