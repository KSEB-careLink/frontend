package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R

@Composable
fun Patient_Quiz(navController: NavController) {
    // ✏️값 조절용
    val logoSize      = 200.dp
    val logoOffsetY   = (1).dp
    val speakerTopGap = 16.dp
    val greyBoxTopGap = 16.dp
    val greyBoxHeight = 330.dp
    val greyBoxCorner = 12.dp
    val questionGap   = 16.dp
    val optionGap     = 12.dp
    val optionHeight  = 56.dp

    // 현재 route
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute  = navBackStack?.destination?.route

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
            // 1) 로고
            Box(
                Modifier
                    .offset(y = logoOffsetY)
                    .size(logoSize),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.rogo),
                    contentDescription = "로고",
                    modifier = Modifier.size(logoSize),
                    contentScale = ContentScale.Fit
                )
            }

            // 2) 내레이션
            Spacer(Modifier.height(speakerTopGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.VolumeUp,
                    contentDescription = "소리 재생",
                    modifier = Modifier.size(28.dp),
                    tint = Color.Black
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = buildAnnotatedString {
                        append("작년 봄, 손녀와 함께 전주에서 특별한 음식을 먹었을 때의 사진이네요!")
                    },
                    fontSize = 20.sp,
                    lineHeight = 24.sp
                )
            }

            // 3) 회색 박스
            Spacer(Modifier.height(greyBoxTopGap))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(greyBoxHeight)
                    .background(Color(0xFFEDE9F5), RoundedCornerShape(greyBoxCorner))
            )

            // 4) 질문
            Spacer(Modifier.height(questionGap))
            Text(
                text = "무엇을 드셨을까요?",
                fontSize = 28.sp,
                color = Color(0xFF00C4B4),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // 5) 선택지 2x2
            Spacer(Modifier.height(optionGap))
            Column(verticalArrangement = Arrangement.spacedBy(optionGap)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(optionGap)
                ) {
                    OptionButton(
                        text = "냉면",
                        onClick = { /*TODO*/ },
                        modifier = Modifier.weight(1f)
                    )
                    OptionButton(
                        text = "비빔밥",
                        onClick = { /*TODO*/ },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(optionGap)
                ) {
                    OptionButton(
                        text = "떡볶이",
                        onClick = { /*TODO*/ },
                        modifier = Modifier.weight(1f)
                    )
                    OptionButton(
                        text = "칼국수",
                        onClick = { /*TODO*/ },
                        modifier = Modifier.weight(1f)
                    )
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
        modifier = modifier
            .height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
    ) {
        Text(text = text, color = Color.White)
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPatient_Quiz() {
    Patient_Quiz(navController = rememberNavController())
}
