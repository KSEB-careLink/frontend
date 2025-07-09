package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.myapplication.R
import androidx.navigation.compose.rememberNavController


@Composable
fun PatientSignInPage(
    navController: NavController
) {
    // ★ 위치 조절 변수
    val imageGroupTopPadding = 80.dp
    val formTopPadding = 300.dp
    val buttonBottomPadding = 0.1.dp

    // 입력 상태
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // 1) 로고 겹치기
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = imageGroupTopPadding)
                .sizeIn(minWidth = 0.dp, minHeight = 0.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.rogo),
                contentDescription = "로고",
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = (-20).dp),
                contentScale = ContentScale.Fit
            )
            Image(
                painter = painterResource(R.drawable.ai_text),
                contentDescription = "텍스트 로고",
                modifier = Modifier
                    .size(150.dp)
                    .offset(y = 90.dp),
                contentScale = ContentScale.Fit
            )
        }

        // 2) 회원 가입 폼
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = formTopPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {Spacer(Modifier.height(24.dp))
            Text(
                text = "회원 가입",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(24.dp))

            Text(text = "이름", fontSize = 16.sp)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("이름") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            Text(text = "이메일 주소 (아이디)", fontSize = 16.sp)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("example@mail.com") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true
            )
            Spacer(Modifier.height(16.dp))

            Text(text = "비밀번호", fontSize = 16.sp)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("••••••••") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-140).dp),   // 필요시 위치 조정
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 가입하기
            Button(
                onClick = { navController.navigate("signup") },
                modifier = Modifier
                    .weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "가입하기",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }

            // 로그인
            Button(
                onClick = { navController.navigate("p_login") },
                modifier = Modifier
                    .weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(
                    text = "로그인",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPatientSignInPage() {
    PatientSignInPage(navController = rememberNavController())
}



