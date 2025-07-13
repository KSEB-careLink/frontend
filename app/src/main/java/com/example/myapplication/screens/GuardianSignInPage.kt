// app/src/main/java/com/example/myapplication/screens/GuardianSignUpScreen.kt
package com.example.myapplication.screens

import android.util.Patterns
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.example.myapplication.ui.auth.AuthState
import com.example.myapplication.ui.auth.AuthViewModel
import androidx.compose.foundation.background

@Composable
fun GuardianSignUpScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val ctx = LocalContext.current

    // 1) 입력 필드 상태
    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 2) ViewModel 상태 구독
    val state by authViewModel.state.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenW = maxWidth
        val screenH = maxHeight

        val logoSize     = screenW * 0.5f
        val textLogoSize = screenW * 0.3f

        val logoY    = screenH * 0.10f
        val textY    = screenH * 0.25f
        val formY    = screenH * 0.35f
        val buttonsY = screenH * 0.80f

        val fieldHeight  = screenH * 0.07f
        val fieldSpacer  = screenH * 0.02f

        val btnHeight    = screenH * 0.08f
        val btnWidthFrac = 0.45f
        val btnSpacer    = screenH * 0.02f

        // 배경 레이아웃
        Box(modifier = Modifier.fillMaxSize()) {
            // 1) 로고
            Image(
                painter = painterResource(R.drawable.rogo),
                contentDescription = "로고",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(logoSize)
                    .align(Alignment.TopCenter)
                    .offset(y = logoY)
            )

            // 2) 텍스트 로고
            Image(
                painter = painterResource(R.drawable.ai_text),
                contentDescription = "텍스트 로고",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(textLogoSize)
                    .align(Alignment.TopCenter)
                    .offset(y = textY)
            )

            // 3) 입력 폼
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = formY)
                    .fillMaxWidth(0.9f),
                horizontalAlignment = Alignment.Start
            ) {
                Spacer(Modifier.height(fieldSpacer))
                Text(
                    "회원 가입",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(fieldSpacer))

                Text("이름", fontSize = 16.sp)
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fieldHeight),
                    singleLine = true,
                    placeholder = { Text("이름") }
                )
                Spacer(Modifier.height(fieldSpacer))

                Text("이메일 주소 (아이디)", fontSize = 16.sp)
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fieldHeight),
                    singleLine = true,
                    placeholder = { Text("example@mail.com") }
                )
                Spacer(Modifier.height(fieldSpacer))

                Text("비밀번호", fontSize = 16.sp)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fieldHeight),
                    singleLine = true,
                    placeholder = { Text("최소 6자 이상") }
                )
            }

            // 4) 버튼 그룹
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = buttonsY)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(btnSpacer, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = {
                        // 유효성 검사
                        when {
                            name.isBlank() || email.isBlank() || password.isBlank() -> {
                                Toast.makeText(ctx, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                            }
                            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                                Toast.makeText(ctx, "유효한 이메일을 입력하세요.", Toast.LENGTH_SHORT).show()
                            }
                            password.length < 6 -> {
                                Toast.makeText(ctx, "비밀번호를 6자 이상 입력해주세요.", Toast.LENGTH_SHORT).show()
                            }
                            else -> {
                                // 공통 signup 호출 (role="guardian")
                                authViewModel.signup(
                                    name     = name.trim(),
                                    email    = email.trim(),
                                    password = password,
                                    role     = "guardian"
                                )
                            }
                        }
                    },
                    modifier = Modifier
                        .width(screenW * btnWidthFrac)
                        .height(btnHeight),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
                    enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 6
                ) {
                    Text("가입하기", color = Color.White, fontSize = 16.sp)
                }

                Button(
                    onClick = { navController.navigate("G_login") },
                    modifier = Modifier
                        .width(screenW * btnWidthFrac)
                        .height(btnHeight),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                ) {
                    Text("로그인", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        // 5) 상태에 따른 오버레이 처리
        when (state) {
            is AuthState.Loading -> {
                // 화면 중앙에 로딩 인디케이터
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }

            is AuthState.GuardianSignedUp -> {
                // 가입 성공 → 로그인 화면으로 이동
                LaunchedEffect(state) {
                    Toast
                        .makeText(ctx, (state as AuthState.GuardianSignedUp).message, Toast.LENGTH_SHORT)
                        .show()
                    authViewModel.resetState()
                    navController.navigate("G_login") {
                        popUpTo("GuardianSignUp") { inclusive = true }
                    }
                }
            }

            is AuthState.Error -> {
                // 에러 → 토스트 후 상태 초기화
                LaunchedEffect(state) {
                    Toast.makeText(ctx, (state as AuthState.Error).error, Toast.LENGTH_LONG).show()
                    authViewModel.resetState()
                }
            }

            else -> { /* Idle 상태 */ }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewGuardianSignUp() {
    GuardianSignUpScreen(navController = rememberNavController())
}



