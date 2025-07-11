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
fun  Patient_Sentence(navController: NavController) {
    // ✏️ 조절용 값들
    val logoSize = 200.dp
    val logoOffsetY = (1).dp
    val speakerTopGap = 16.dp
    val greyBoxTopGap = 16.dp
    val greyBoxHeight = 330.dp
    val greyBoxCorner = 12.dp
    val recallBtnHeight = 56.dp
    val recallBtnGap = 12.dp

    // 현재 route 확인
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
            // 1) 로고만 겹치기
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

            // 2) 소리 아이콘 + “기억 나시나요?”
            Spacer(modifier = Modifier.height(speakerTopGap))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Start),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "소리 재생",
                    modifier = Modifier.size(24.dp),
                    tint = Color.Black
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buildAnnotatedString {
                        append("기억 ")
                        withStyle(SpanStyle(color = Color(0xFF00C4B4))) {
                            append("나시나요?")
                        }
                    },
                    fontSize = 24.sp
                )
            }

            // 3) 빈 회색 배경 박스
            Spacer(modifier = Modifier.height(greyBoxTopGap))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(greyBoxHeight)
                    .background(
                        color = Color(0xFFEDE9F5),
                        shape = RoundedCornerShape(greyBoxCorner)
                    )
            )

            // 4) 회상문장 버튼 2개
            Spacer(modifier = Modifier.height(recallBtnGap))
            Button(
                onClick = { /* TODO: 회상문장1 */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(recallBtnHeight),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text(
                    text = "(회상문장1) ex: 작년 크리스마스 기념 가족여행을 갔을 때의 사진이네요!",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(recallBtnGap))
            Button(
                onClick = { /* TODO: 회상문장2 */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(recallBtnHeight),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text(
                    text = "(회상문장2) ex: 손녀딸 경민이가 포크를 들고 있네요!",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPatient_() {
    Patient_Sentence(navController = rememberNavController())
}


