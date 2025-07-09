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
    val imageGroupTopPadding = 220.dp      // 이미지(로고+텍스트) 그룹이 화면 상단에서 얼마나 내려올지
    val buttonGroupTopPadding = 500.dp    // 버튼 그룹이 화면 상단에서 얼마나 내려올지

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // 1) 이미지 그룹: 로고와 텍스트 이미지를 겹쳐서 중앙에 배치
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = imageGroupTopPadding),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.rogo),
                contentDescription = "로고",
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = (-20).dp),   // 로고를 위로 20dp 이동 (조절 가능)
                contentScale = ContentScale.Fit
            )
            Image(
                painter = painterResource(id = R.drawable.ai_text),
                contentDescription = "텍스트 로고",
                modifier = Modifier
                    .size(150.dp)
                    .offset(y = 90.dp),     // 텍스트를 아래로 90dp 이동 (조절 가능)
                contentScale = ContentScale.Fit
            )
        }

        // 2) 버튼 그룹: TopCenter에 붙이고, top padding으로 위치 조절
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = buttonGroupTopPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { navController.navigate("p_login") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),    // 버튼 높이 (조절 가능)
                shape = RoundedCornerShape(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
            ) {
                Text("🧓 어르신으로 시작하기", color = Color.White)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("G_login") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(80.dp),
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












