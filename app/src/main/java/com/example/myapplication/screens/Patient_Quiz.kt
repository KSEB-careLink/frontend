package com.example.myapplication.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.foundation.Image

@Composable
fun Patient_Quiz(navController: NavController) {
    // — 레이아웃 조절용 값들 —
    val logoSize      = 150.dp
    val logoOffsetY   = 1.dp
    val speakerGap    = 16.dp
    val greyGap       = 16.dp
    val greyHeight    = 330.dp
    val greyCorner    = 12.dp
    val questionGap   = 16.dp
    val optionGap     = 12.dp

    // — 문제/정답 데이터 —
    val options       = listOf("냉면", "비빔밥", "떡볶이", "칼국수")
    val correctAnswer = "냉면"

    // — 상태: 선택된 보기 & 결과 화면 표시 여부 —
    var selected   by remember { mutableStateOf<String?>(null) }
    var showResult by remember { mutableStateOf(false) }

    // — 현재 route 체크 (탭 색상용) —
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            val navColors = NavigationBarItemDefaults.colors(
                indicatorColor       = Color.Transparent,
                selectedIconColor    = Color(0xFF00C4B4),
                unselectedIconColor  = Color(0xFF888888),
                selectedTextColor    = Color(0xFF00C4B4),
                unselectedTextColor  = Color(0xFF888888)
            )
            NavigationBar {
                listOf(
                    "sentence" to "회상문장",
                    "quiz"     to "회상퀴즈",
                    "alert"    to "긴급알림"
                ).forEach { (route, label) ->
                    NavigationBarItem(
                        icon      = { Icon(Icons.Default.Star, contentDescription = label) },
                        label     = { Text(label) },
                        selected  = currentRoute == route,
                        onClick   = {
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        },
                        colors    = navColors
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier           = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // 1) 로고: 질문 화면에서만 노출
            if (!showResult) {
                Image(
                    painter            = painterResource(R.drawable.rogo),
                    contentDescription = "로고",
                    modifier           = Modifier
                        .size(logoSize)
                        .offset(y = logoOffsetY),
                    contentScale       = ContentScale.Fit
                )
                Spacer(Modifier.height(speakerGap))
            }

            if (!showResult) {
                // ───────── 질문 화면 ─────────
                Row(
                    modifier           = Modifier.fillMaxWidth(),
                    verticalAlignment  = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "소리",
                        modifier    = Modifier.size(28.dp),
                        tint        = Color.Black
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "작년 봄, 손녀와 함께 전주에서 특별한 음식을 먹었을 때의 사진이네요!",
                        fontSize   = 20.sp,
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

                Text(
                    "무엇을 드셨을까요?",
                    fontSize = 28.sp,
                    color    = Color(0xFF00C4B4)
                )

                Spacer(Modifier.height(questionGap))

                // ★ 질문 화면용 2×2 보기 그리드
                Column(
                    modifier           = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(optionGap)
                ) {
                    options.chunked(2).forEach { rowItems ->
                        Row(
                            modifier              = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(optionGap)
                        ) {
                            rowItems.forEach { text ->
                                OptionButton(
                                    text    = text,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selected   = text
                                        showResult = true
                                    }
                                )
                            }
                        }
                    }
                }
                // ───────────────────────────────

            } else {
                // ───────── 결과 화면 ─────────
                Spacer(Modifier.height(162.dp))

                val isCorrect = selected == correctAnswer

                if (isCorrect) {
                    // 정답
                    Text(
                        "정답이에요!",
                        fontSize = 32.sp,
                        color    = Color(0xFF00A651)
                    )
                    Spacer(Modifier.height(10.dp))
                    Image(
                        painter            = painterResource(R.drawable.ch),
                        contentDescription = "정답 이미지",
                        modifier           = Modifier.size(300.dp),
                        contentScale       = ContentScale.Fit
                    )
                    Spacer(Modifier.height(1.dp))
                    Text(
                        "정말 잘 기억하셨어요😊",
                        fontSize = 20.sp,
                        color    = Color(0xFF00C4B4)
                    )
                } else {
                    // 오답
                    Text(
                        "정답이 아니에요!",
                        fontSize = 32.sp,
                        color    = Color(0xFFE2101A)
                    )
                    Spacer(Modifier.height(16.dp))
                    Image(
                        painter            = painterResource(R.drawable.wr),
                        contentDescription = "오답 이미지",
                        modifier           = Modifier.size(300.dp),
                        contentScale       = ContentScale.Fit
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "다시 기억해볼까요?",
                        fontSize = 20.sp,
                        color    = Color(0xFF00C4B4)
                    )
                }

                Spacer(Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),            // ← 여기도 동일!
                    verticalArrangement = Arrangement.spacedBy(optionGap)
                ) {
                    options.chunked(2).forEach { rowItems ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(optionGap)
                        ) {
                            rowItems.forEach { text ->
                                OptionButton(
                                    text = text,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        selected = text
                                        showResult = true
                                    }
                                )
                            }
                        }
                    }
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





