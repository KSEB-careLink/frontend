package com.example.myapplication.screens

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.clickable
import androidx.compose.material3.Divider

@Composable
fun Recode(
    navController: NavController,
    voices: List<String>,                   // ① 외부에서 전달된 음성 리스트
    onSelectVoice: (String) -> Unit         // ② 음성 선택 콜백
) {

    val topGroupPadding = 80.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = topGroupPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1) 로고, 타이틀, 음성 선택 버튼
            Image(
                painter = painterResource(id = R.drawable.rogo),
                contentDescription = "로고",
                modifier = Modifier.size(200.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "보호자 음성 등록",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { /* TODO: 파일 선택 로직 호출 */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("목소리 선택", color = Color.White, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2) 음성 리스트
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 200.dp)  // 필요에 따라 크기 조정
                    .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                voices.forEachIndexed { index, name ->
                    // 선택 가능한 리스트 아이템
                    Text(
                        text = "${index + 1}. $name",
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectVoice(name) }
                            .padding(vertical = 8.dp)
                    )
                    if (index < voices.lastIndex) Divider()
                }
            }
        }

        // 3) 하단 녹음하러 가기 버튼
        Button(
            onClick = { navController.navigate("record") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-100).dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("녹음하러 가기", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRecode() {
    // 임시 더미 데이터
    val dummy = listOf("기본", "A", "B")
    Recode(
        navController = rememberNavController(),
        voices = dummy,
        onSelectVoice = { /* TODO: 선택 처리 */ }
    )
}

