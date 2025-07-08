package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun Login(navController: NavController) {
    // ★ 위치 조절 변수 ★
    val imageGroupTopPadding = 80.dp    // 로고 겹치기 그룹이 상단에서 얼마나 내려올지
    val formTopPadding = 320.dp         // 이메일/비밀번호 폼이 화면 상단에서 얼마나 내려올지


    // 입력값 상태
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // 1) 가운데 로고 겹치기
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = imageGroupTopPadding)
                .sizeIn(minWidth = 0.dp, minHeight = 0.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.rogo),
                contentDescription = "로고",
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = (-20).dp), // 로고를 위로 20dp 이동
                contentScale = ContentScale.Fit
            )
            Image(
                painter = painterResource(id = R.drawable.ai_text),
                contentDescription = "텍스트 로고",
                modifier = Modifier
                    .size(150.dp)
                    .offset(y = 90.dp),   // 텍스트를 아래로 90dp 이동
                contentScale = ContentScale.Fit
            )
        }

        // 2) 로그인 폼 (타이틀 + 입력란)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = formTopPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            // 타이틀
            Text(
                text = "로그인",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // 이메일
            Text(text = "이메일 주소", fontSize = 16.sp)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("example@mail.com") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 비밀번호
            Text(text = "비밀번호", fontSize = 16.sp)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("••••••••") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        }

        // 3) 화면 하단에 두 개의 버튼을 Column으로 묶기
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)        // BoxScope.align
                .padding(bottom = 170.dp)               // 아래 여백
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = { /* 로그인 처리 */ },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),                  // 버튼 높이
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("로그인", color = Color.White, fontSize = 16.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { navController.navigate("patient") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),                  // 버튼 높이
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("회원가입", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewLogin() {
    Login(navController = rememberNavController())
}
