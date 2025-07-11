package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(3000)
        navController.navigate("choose") {
            popUpTo("splash") { inclusive = true }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 1) 화면의 너비·높이를 Dp 단위로 가져온다
        val screenW = maxWidth
        val screenH = maxHeight

        // 2) 화면 너비 대비 이미지 크기를 비율로 결정
        val rogoSize = screenW * 0.5f     // 너비의 50%
        val textSize = screenW * 0.3f     // 너비의 30%

        // 3) 화면 높이 대비 배치 Y 좌표를 비율로 결정
        val rogoY = screenH * 0.35f       // 높이의 35% 지점
        val textY = screenH * 0.50f       // 높이의 50% 지점

        Box(modifier = Modifier.fillMaxSize()) {
            // 로고
            Image(
                painter = painterResource(id = R.drawable.rogo),
                contentDescription = "Rogo",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(rogoSize)
                    .align(Alignment.TopCenter)   // 부모 Box의 위쪽 중앙에 붙이고
                    .offset(y = rogoY)            // 위에서 rogoY 만큼 아래로 이동
            )

            // AI 텍스트
            Image(
                painter = painterResource(id = R.drawable.ai_text),
                contentDescription = "AI Text",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(textSize)
                    .align(Alignment.TopCenter)
                    .offset(y = textY)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    val dummyNavController = rememberNavController()
    SplashScreen(navController = dummyNavController)
}










