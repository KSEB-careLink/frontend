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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Density
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import androidx.compose.ui.graphics.Color

@Composable
fun ChoosePositionPage(navController: NavController) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 화면 크기를 Dp 단위로 가져오기
        val screenW: Dp = maxWidth
        val screenH: Dp = maxHeight

        // — 이미지 크기 (화면 너비의 비율)
        val rogoSize = screenW * 0.5f    // 로고: 너비 50%
        val textSize = screenW * 0.3f    // 텍스트: 너비 30%

        // — Y 오프셋 (화면 높이의 비율)
        val rogoY = screenH * 0.25f      // 로고는 높이 25% 지점
        val textY = screenH * 0.40f      // 텍스트는 높이 38% 지점
        val buttonsY = screenH * 0.55f   // 버튼 그룹은 높이 60% 지점

        Box(modifier = Modifier.fillMaxSize()) {
            // 1) 로고
            Image(
                painter = painterResource(id = R.drawable.rogo),
                contentDescription = "로고",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(rogoSize)
                    .align(Alignment.TopCenter)
                    .offset(y = rogoY)
            )

            // 2) 텍스트 로고
            Image(
                painter = painterResource(id = R.drawable.ai_text),
                contentDescription = "텍스트 로고",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(textSize)
                    .align(Alignment.TopCenter)
                    .offset(y = textY)
            )

            // 3) 버튼 그룹
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = buttonsY)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp) // 버튼 사이 고정 dp
            ) {
                val buttonHeight = screenH * 0.10f  // 버튼 높이: 화면 높이의 10%
                Button(
                    onClick = { navController.navigate("p_login") },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)           // 버튼 너비: 화면 너비의 80%
                        .height(buttonHeight),
                    shape = RoundedCornerShape(buttonHeight / 2),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("🧓 어르신으로 시작하기", color = Color.White)
                }
                Button(
                    onClick = { navController.navigate("G_login") },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(buttonHeight / 2),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
                ) {
                    Text("👪 보호자로 시작하기", color = Color.White)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChoosePositionPage() {
    ChoosePositionPage(navController = rememberNavController())
}













