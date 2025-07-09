package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.R
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

@Composable
fun Main_Page(navController: NavController) {

    // 로고 그룹 상단 패딩 값
    val imageGroupTopPadding = 32.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // 1) 이미지 그룹: 로고(rogo)와 텍스트(ai_text)를 겹쳐서 중앙에 배치
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
                    .offset(y = (-20).dp),
                contentScale = ContentScale.Fit
            )
            Image(
                painter = painterResource(id = R.drawable.ai_text),
                contentDescription = "텍스트 로고",
                modifier = Modifier
                    .size(150.dp)
                    .offset(y = 90.dp),
                contentScale = ContentScale.Fit
            )
        }

        // 2) 장치 리스트 버튼들
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val deviceNames = listOf("장치 1", "장치 2", "장치 3")
            deviceNames.forEach { name ->
                Button(
                    onClick = { /* TODO: 장치 상세로 이동 */ },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(66.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = name,
                            color = Color.White,
                            fontSize = 18.sp
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "Next",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // 3) 하단 환자의 기기 추가 버튼
        Button(
            onClick = { navController.navigate("addDevice") },
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .padding(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "환자의 기기 추가",
                color = Color.White,
                fontSize = 18.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMainPage() {
    // 더미 NavController는 Helpers로 만들거나 그냥 null 처리
    Main_Page(navController = rememberNavController())
}
