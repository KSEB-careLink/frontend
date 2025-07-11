package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R

@Composable
fun GuardianBasicInfoScreen() {
    var relationship by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf("다정한") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 이미지
        Image(
            painter = painterResource(id = R.drawable.rogo), // drawable에 아이콘 추가해야 함
            contentDescription = "Logo",
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 제목
        Text("기본 정보 입력", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(32.dp))

        // 1번 질문
        Text("1. 보호 대상을 부르는 호칭을 알려주세요", fontSize = 16.sp, color = Color.Black)
        Spacer(modifier = Modifier.height(8.dp))
        TextField(
            value = relationship,
            onValueChange = { relationship = it },
            placeholder = { Text("기타") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 2번 질문
        Text("2. 원하는 말투", fontSize = 16.sp, color = Color.Black)
        Text("(의상 문장 생성, 보호자 음성에 이용)", fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf("다정한", "밝은", "차분한").forEach { option ->
                val selected = tone == option
                Button(
                    onClick = { tone = option },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFF9C27B0) else Color(0xFFE1BEE7),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
                ) {
                    Text(option)
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // 완료 버튼
        Button(
            onClick = { /* 완료 로직 추가 */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DD0E1)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text("완료", color = Color.White, fontSize = 16.sp)
        }
    }
}
