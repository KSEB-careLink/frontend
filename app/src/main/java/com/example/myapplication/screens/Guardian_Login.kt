// app/src/main/java/com/example/myapplication/screens/GuardianLoginScreen.kt
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.R
import com.example.myapplication.ui.auth.AuthState
import com.example.myapplication.ui.auth.AuthViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun GuardianLoginScreen(
    navController: NavController,
    viewModel: AuthViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // 1) 폼 입력 상태
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 2) ViewModel 상태 구독
    val state by viewModel.state.collectAsState()

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val screenW: Dp = maxWidth
        val screenH: Dp = maxHeight

        val rogoSize = screenW * 0.5f
        val textSize = screenW * 0.3f
        val rogoY    = screenH * 0.10f
        val textY    = screenH * 0.25f
        val formY    = screenH * 0.37f
        val fieldHeight  = screenH * 0.07f
        val fieldSpacing = screenH * 0.02f
        val buttonsY     = screenH * 0.70f
        val buttonHeight = screenH * 0.08f
        val buttonWidth  = 0.8f
        val buttonSpacing= screenH * 0.02f

        // 로고 & 텍스트
        Image(
            painter = painterResource(R.drawable.rogo),
            contentDescription = "로고",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(rogoSize)
                .align(Alignment.TopCenter)
                .offset(y = rogoY)
        )
        Image(
            painter = painterResource(R.drawable.ai_text),
            contentDescription = "텍스트 로고",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(textSize)
                .align(Alignment.TopCenter)
                .offset(y = textY)
        )

        // 로그인 폼
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = formY)
                .fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.Start
        ) {
            Text("보호자 로그인", fontSize = 32.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Spacer(Modifier.height(fieldSpacing))

            Text("이메일 주소", fontSize = 16.sp)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("example@mail.com") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fieldHeight),
                singleLine = true
            )

            Spacer(Modifier.height(fieldSpacing))

            Text("비밀번호", fontSize = 16.sp)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("••••••••") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fieldHeight),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        }

        // 버튼 그룹
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
                    // 간단 검증
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // 로그인 + 토큰 검증
                    coroutineScope.launch {
                        try {
                            Firebase.auth
                                .signInWithEmailAndPassword(email, password)
                                .await()
                            viewModel.verifyToken()
                        } catch (e: Exception) {
                            Toast.makeText(context, "로그인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(buttonWidth)
                    .height(buttonHeight),
                shape = RoundedCornerShape(buttonHeight / 2),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("로그인", color = Color.White, fontSize = 16.sp)
            }

            Button(
                onClick = { navController.navigate("guardianSignup") },
                modifier = Modifier
                    .fillMaxWidth(buttonWidth)
                    .height(buttonHeight),
                shape = RoundedCornerShape(buttonHeight / 2),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("회원가입", color = Color.White, fontSize = 16.sp)
            }
        }
    }

    // 3) ViewModel 상태에 따른 화면 로직
    when (state) {
        is AuthState.Loading -> {
            // 전체 화면에 로딩 표시
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AuthState.VerifiedRole -> {
            // 역할에 따라 네비게이트
            val role = (state as AuthState.VerifiedRole).role
            LaunchedEffect(role) {
                val dest = if (role == "guardian") "code" else "patientHome"
                navController.navigate(dest) {
                    popUpTo("login") { inclusive = true }
                }
            }
        }
        is AuthState.Error -> {
            // 에러 토스트 후 상태 초기화 (resetState() 추가 구현 필요)
            LaunchedEffect(state) {
                Toast.makeText(context, (state as AuthState.Error).error, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
        }
        else -> { /* Idle 상태 */ }
    }
}


