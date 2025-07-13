// app/src/main/java/com/example/myapplication/screens/PatientLoginScreen.kt
package com.example.myapplication.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background       // for overlay
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.example.myapplication.ui.auth.AuthState
import com.example.myapplication.ui.auth.AuthViewModel
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun PatientLoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val auth = Firebase.auth
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 입력 상태
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // ViewModel 상태 구독
    val state by authViewModel.state.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // 로고 영역
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
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

        // 폼 영역
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 320.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "어르신 로그인",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(24.dp))

            Text("이메일 주소", fontSize = 16.sp)
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

            Text("비밀번호", fontSize = 16.sp)
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

        // 버튼 영역
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 170.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        try {
                            // Firebase 로그인
                            auth.signInWithEmailAndPassword(email.trim(), password).await()
                            // 토큰 검증
                            authViewModel.verifyToken()
                        } catch (e: Exception) {
                            Toast.makeText(context, "로그인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("로그인", color = Color.White, fontSize = 16.sp)
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("patientSignUp") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("회원가입", color = Color.White, fontSize = 16.sp)
            }
        }

        // 상태 기반 오버레이 & 내비게이션
        when (state) {
            is AuthState.Loading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color(0x88000000)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = Color.White)
                }
            }
            is AuthState.VerifiedRole -> {
                LaunchedEffect(state) {
                    val role = (state as AuthState.VerifiedRole).role
                    if (role == "patient") {
                        navController.navigate("patientHome") {
                            popUpTo("patientLogin") { inclusive = true }
                        }
                    } else {
                        Toast.makeText(context, "권한 오류: role=$role", Toast.LENGTH_SHORT).show()
                    }
                    authViewModel.resetState()
                }
            }
            is AuthState.Error -> {
                LaunchedEffect(state) {
                    Toast.makeText(context, (state as AuthState.Error).error, Toast.LENGTH_LONG).show()
                    authViewModel.resetState()
                }
            }
            else -> { /* Idle */ }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPatientLogin() {
    PatientLoginScreen(navController = rememberNavController())
}


