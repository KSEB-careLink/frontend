// Recode2.kt
package com.example.myapplication.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.example.myapplication.audio.AudioRecorder
import androidx.compose.foundation.Image

@Composable
fun Recode2(navController: NavController) {
    val context = LocalContext.current
    var isRecording by remember { mutableStateOf(false) }
    var audioPath by remember { mutableStateOf<String?>(null) }

    // 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Toast.makeText(context, "녹음 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Composable 로딩 시 권한 요청
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    // 녹음 매니저
    val recorder = remember { AudioRecorder(context) }

    // UI 파라미터
    val topPadding = 80.dp
    val betweenLogoAndTitle = 12.dp
    val betweenTitleAndSub = 8.dp
    val greyBoxTopGap = 16.dp
    val greyBoxHeight = 400.dp
    val greyBoxCorner = 12.dp
    val bottomButtonOffsetY = 48.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding, start = 24.dp, end = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 로고
            Image(
                painter = painterResource(id = R.drawable.rogo),
                contentDescription = "로고",
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )

            Spacer(Modifier.height(betweenLogoAndTitle))

            // 타이틀
            Text(
                text = if (!isRecording) "녹음 전" else "녹음 중",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(Modifier.height(betweenTitleAndSub))

            // 서브텍스트
            Text(
                text = if (!isRecording)
                    "버튼을 눌러 녹음을 시작하세요"
                else
                    "최대한 생동감 있게 텍스트를 읽어주세요",
                fontSize = 16.sp,
                color = Color.DarkGray
            )

            Spacer(Modifier.height(greyBoxTopGap))

            // 텍스트 박스
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

        // 녹음 버튼
        Button(
            onClick = {
                if (!isRecording) {
                    // 녹음 시작
                    audioPath = recorder.startRecording()
                    isRecording = true
                } else {
                    // 녹음 종료
                    val savedPath = recorder.stopRecording()
                    isRecording = false
                    Toast.makeText(context, "녹음 저장: $savedPath", Toast.LENGTH_LONG).show()
                    // 녹음 완료 후 네비게이션
                    navController.navigate("recode")
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-bottomButtonOffsetY)),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text(
                text = if (!isRecording) "녹음 시작" else "녹음 완료",
                color = Color.White,
                fontSize = 18.sp
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRecordingScreen() {
    Recode2(navController = rememberNavController())
}


