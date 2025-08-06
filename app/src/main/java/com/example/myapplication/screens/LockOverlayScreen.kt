package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.R

@Composable
fun LockOverlayScreen(onButtonClick: () -> Unit) {
    val context = LocalContext.current

    // ① 앱 실행 중 최초 한번, 채널 생성
    LaunchedEffect(Unit) {
        LockOverlayNotificationHelper.createChannel(context)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0x80000000))
    ) {
        Card(
            Modifier
                .fillMaxWidth(0.9f)
                .aspectRatio(0.6f)
                .align(Alignment.Center),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFDFF6E4))
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(Modifier.height(1.dp))
                Image(
                    painter = painterResource(R.drawable.rogo),
                    contentDescription = "CareLink 로고",
                    modifier = Modifier.size(120.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "“오늘도 함께”",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "“소중한 하루를 채워보세요!”",
                        fontSize = 16.sp
                    )
                }
                Button(
                    onClick = {
                        // ② 버튼 클릭 시 Notification → 풀스크린 인텐트로 오버레이 띄우기
                        LockOverlayNotificationHelper.showOverlayNotification(context)
                    },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFCEBB6)),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "carelink 앱으로 바로 이동",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )
                }
            }
        }
    }
}


