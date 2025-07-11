package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 배경 이미지: un.png 전체화면
//        Image(
//            painter = painterResource(id = R.drawable.un),
//            contentDescription = "Background Image",
//            contentScale = ContentScale.Crop,
//            modifier = Modifier.fillMaxSize()
//        )

        // 가운데 rogo.png와 ai_text.png를 겹치듯 배치
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 기준이 되는 rogo.png (y축으로 조금 올림)
            Image(
                painter = painterResource(id = R.drawable.rogo),
                contentDescription = "Rogo Image",
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = (-20).dp), // 로고를 위로 20dp 이동
                contentScale = ContentScale.Fit
            )

            // rogo.png 아래에 ai_text.png를 겹치게 배치 (필요시 y값 조정)
            Image(
                painter = painterResource(id = R.drawable.ai_text),
                contentDescription = "AI Text Image",
                modifier = Modifier
                    .size(150.dp)
                    .offset(y = 90.dp),
                contentScale = ContentScale.Fit
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












