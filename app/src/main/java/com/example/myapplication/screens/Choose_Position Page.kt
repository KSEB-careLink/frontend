package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R

@Composable
fun ChoosePositionPage(navController: NavController) {
    // 이 두 값만 바꿔 보세요!
    val topPadding = 100.dp     // 이미지 그룹이 화면 상단에서 얼마나 아래로 내려올지
    val bottomPadding = 48.dp   // 버튼 그룹이 화면 바닥에서 얼마나 위로 올라올지

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // 1) 로고 + 텍스트 이미지
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.rogo),
                contentDescription = "로고",
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(R.drawable.ai_text),
                contentDescription = "텍스트 로고",
                modifier = Modifier.size(150.dp),
                contentScale = ContentScale.Fit
            )
        }

        // 2) 버튼 두 개
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = bottomPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { navController.navigate("patient") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),     // 버튼 높이를 바꾸고 싶으면 이 값을 조절
                shape = RoundedCornerShape(90.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("🧓 어르신으로 시작하기", color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))  // 버튼 사이 간격
            Button(
                onClick = { navController.navigate("guardian") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),     // 버튼 높이 동일하게 유지
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("👪 보호자로 시작하기", color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChoosePositionPage() {
    ChoosePositionPage(navController = rememberNavController())
}










