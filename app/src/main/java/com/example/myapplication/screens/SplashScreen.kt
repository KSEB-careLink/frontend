package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.R
import androidx.navigation.compose.rememberNavController

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        delay(2000)
        navController.navigate("home") {
            popUpTo("splash") { inclusive = true }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // 배경 이미지: un.png 전체화면
        Image(
            painter = painterResource(id = R.drawable.un),
            contentDescription = "Background Image",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        // 가운데 rogo.png와 ai_text.png를 겹치듯 배치
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // 기준이 되는 rogo.png
            Image(
                painter = painterResource(id = R.drawable.rogo),
                contentDescription = "Rogo Image",
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )

            // rogo.png 아래에 ai_text.png를 겹치게 배치 (y값으로 미세조정 가능)
            Image(
                painter = painterResource(id = R.drawable.ai_text),
                contentDescription = "AI Text Image",
                modifier = Modifier
                    .size(150.dp)
                    .offset(y = 50.dp), // 겹치게 하려면 값을 조정
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










