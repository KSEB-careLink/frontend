package com.example.myapplication.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.R
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource

@Composable
fun Code2(navController: NavController) {
    var code by remember { mutableStateOf("") }
    val context = LocalContext.current


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top // 🔹 로고를 맨 위로
    ) {
        // 🔹 상단 로고 이미지 추가
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            modifier = Modifier
                .size(160.dp)
                .padding(top = 40.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("보호자 코드 입력", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        // 검정 박스 6개에 코드 표시
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until 6) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF333333), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = code.getOrNull(i)?.toString() ?: "",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 텍스트 입력창
        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = { Text("6자리 코드") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 연결 버튼
        Button(
            onClick = {
                if (code.length == 6) {
                    Toast.makeText(context, "입력한 코드: $code", Toast.LENGTH_SHORT).show()
                    // TODO: 서버로 연동 요청 보내기
                    // 예: POST /link-patient-to-guardian
                    // 성공 시 navController.navigate("main")
                } else {
                    Toast.makeText(context, "6자리를 모두 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("보호자와 연결", color = Color.White, fontSize = 16.sp)
        }
    }
}
