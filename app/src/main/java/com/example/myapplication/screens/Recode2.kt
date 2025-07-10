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
import androidx.compose.ui.layout.ContentScale              // ← 이 줄을 추가!
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.R
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.Image                 // ← Image 도 import

@Composable
fun Recode2(navController: NavController) {
    // ✏️ 조절값
    val topPadding           = 80.dp
    val betweenLogoAndTitle  = 12.dp
    val betweenTitleAndSub   = 8.dp
    val greyBoxTopGap        = 16.dp
    val greyBoxHeight        = 400.dp
    val greyBoxCorner        = 12.dp
    val bottomButtonOffsetY   = 48.dp
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding, start = 24.dp, end = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 1) 로고
            Image(
                painter = painterResource(id = R.drawable.rogo),
                contentDescription = "로고",
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(betweenLogoAndTitle))

            // 2) 타이틀
            Text(
                text = "녹음중...",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(betweenTitleAndSub))

            // 3) 서브텍스트
            Text(
                text = "최대한 생동감 있게 텍스트를 읽어주세요",
                fontSize = 16.sp,
                color = Color.DarkGray
            )

            Spacer(modifier = Modifier.height(greyBoxTopGap))

            // 4) 회색 박스 + 플레이스홀더 텍스트
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(greyBoxHeight)
                    .background(
                        color = Color(0xFFCCCCCC),
                        shape = RoundedCornerShape(greyBoxCorner)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "소설 텍스트 나옴......",
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
        }

        // 5) 하단 버튼 (약간 위로 띄워서 배치)
        Button(
            onClick = { /* TODO: 녹음 완료 후 navController.navigate(...) */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-bottomButtonOffsetY)),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("녹음 완료", color = Color.White, fontSize = 18.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRecordingScreen() {
    Recode2(navController = rememberNavController())
}


