package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R

@Composable
fun ChoosePositionPage(navController: NavController) {
    // 박스를 탭하면 환자 로그인 페이지로 이동
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { navController.navigate("patient") },
        contentAlignment = Alignment.Center
    ) {

        // 로고 이미지 (y축으로 위로 20dp 이동)
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "Rogo Image",
            modifier = Modifier
                .size(200.dp)
                .offset(y = (-20).dp),
            contentScale = ContentScale.Fit
        )

        // AI 텍스트 이미지 (로고 아래에 겹치듯 배치)
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

@Preview(showBackground = true)
@Composable
fun PreviewChoosePositionPage() {
    val navController = rememberNavController()
    ChoosePositionPage(navController = navController)
}
