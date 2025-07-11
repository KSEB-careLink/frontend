package com.example.myapplication.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R

@Composable
fun Code(navController: NavController) {
    // 코드 박스 개수
    val codeLength = 5

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(80.dp))

            // 1) 로고
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.rogo),
                contentDescription = "로고",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2) 타이틀
            Text(
                text = "코드 주고 받기",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 3) 코드 입력 박스 (비활성 상태)
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(codeLength) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                color = Color(0xFF333333),
                                shape = RoundedCornerShape(8.dp)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 4) 유효기간
            Text(
                text = "코드 유효 기간 xx/xx/xx",
                fontSize = 14.sp,
                color = Color.Black
            )
        }

        // 5) 하단 버튼
        Button(
            onClick = { navController.navigate("main") },
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp)
                .align(Alignment.BottomCenter)
                 .offset(y = (-250).dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("보호자 pin 코드 입력", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCodeExchangePage() {
    Code(navController = rememberNavController())
}


