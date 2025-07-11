package com.example.myapplication.screens

import android.widget.Toast
import androidx.compose.foundation.Image
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.example.myapplication.network.RetrofitInstance
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.BoxWithConstraints

@Composable
fun Guardian_Login(navController: NavController) {
    // Firebase Auth & CoroutineScope
    val auth = Firebase.auth
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // 화면 크기(Dp)
        val screenW: Dp = maxWidth
        val screenH: Dp = maxHeight

        // 이미지 크기 (화면 너비 비율)
        val rogoSize = screenW * 0.5f
        val textSize = screenW * 0.3f

        // Y 오프셋 (화면 높이 비율)
        val rogoY = screenH * 0.10f
        val textY = screenH * 0.25f
        val formY = screenH * 0.37f

        // 폼 필드 높이 및 간격
        val fieldHeight = screenH * 0.07f
        val fieldSpacing = screenH * 0.02f

        // 버튼 그룹 Y 오프셋, 크기, 간격
        val buttonsY = screenH * 0.70f
        val buttonHeight = screenH * 0.08f
        val buttonWidthFraction = 0.8f
        val buttonSpacing = screenH * 0.02f

        // 실제 배치
        Box(modifier = Modifier.fillMaxSize()) {
            // 1) 로고
            Image(
                painter = painterResource(id = R.drawable.rogo),
                contentDescription = "로고",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(rogoSize)
                    .align(Alignment.TopCenter)
                    .offset(y = rogoY)
            )

            // 2) 텍스트 로고
            Image(
                painter = painterResource(id = R.drawable.ai_text),
                contentDescription = "텍스트 로고",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(textSize)
                    .align(Alignment.TopCenter)
                    .offset(y = textY)
            )

            // 3) 로그인 폼
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = formY)
                    .fillMaxWidth(0.9f),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "보호자 로그인",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(fieldSpacing))

                // 이메일
                Text(text = "이메일 주소", fontSize = 16.sp)
                OutlinedTextField(
                    value = remember { mutableStateOf("") }.value,
                    onValueChange = { /* state 관리 필요 */ },
                    placeholder = { Text("example@mail.com") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fieldHeight),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(fieldSpacing))

                // 비밀번호
                Text(text = "비밀번호", fontSize = 16.sp)
                OutlinedTextField(
                    value = remember { mutableStateOf("") }.value,
                    onValueChange = { /* state 관리 필요 */ },
                    placeholder = { Text("••••••••") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fieldHeight),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )
            }

            // 4) 버튼 그룹
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = buttonsY)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(buttonSpacing)
            ) {
                Button(
                    onClick = {
                        // 로그인 로직…
                    },
                    modifier = Modifier
                        .fillMaxWidth(buttonWidthFraction)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(buttonHeight / 2),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                ) {
                    Text("로그인", color = Color.White, fontSize = 16.sp)
                }
                Button(
                    onClick = { navController.navigate("guardian") },
                    modifier = Modifier
                        .fillMaxWidth(buttonWidthFraction)
                        .height(buttonHeight),
                    shape = RoundedCornerShape(buttonHeight / 2),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                ) {
                    Text("회원가입", color = Color.White, fontSize = 16.sp)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLogin2() {
    Guardian_Login(navController = rememberNavController())
}

