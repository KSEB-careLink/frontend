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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.compose.ui.tooling.preview.Preview
import com.example.myapplication.R
import androidx.navigation.compose.rememberNavController

@Composable
fun Recode2(navController: NavController) {
    // ✏️ 조절값
    val topSpacer             = 80.dp    // 로고 위 여백
    val betweenLogoAndTitle   = 24.dp    // 로고-타이틀 간격
    val betweenTitleAndSub    = 8.dp     // 타이틀-서브텍스트 간격
    val greyBoxTopGap         = 16.dp    // 서브텍스트-회색박스 간격
    val greyBoxHeight         = 400.dp   // 회색박스 높이
    val greyBoxCorner         = 12.dp    // 회색박스 모서리
    val bottomButtonOffsetY   = 48.dp    // 버튼을 얼마나 위로 올릴지

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
        ) {
            Spacer(modifier = Modifier.height(topSpacer))

            // 1) 로고
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.rogo),
                contentDescription = "로고",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(betweenLogoAndTitle))

            // 2) 타이틀
            Text(
                text = "녹음중.....",
                fontSize =  32.sp,
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
