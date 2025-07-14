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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect

@Composable
fun GuardianLoginScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val firebaseAuth = Firebase.auth

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // ViewModel 상태 구독
    val state by authViewModel.state.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        val screenW = maxWidth
        val screenH = maxHeight

        val logoSize   = screenW * 0.5f
        val textSize   = screenW * 0.3f
        val logoY      = screenH * 0.10f
        val textY      = screenH * 0.25f
        val formY      = screenH * 0.37f
        val fieldH     = screenH * 0.07f
        val fieldSpacer= screenH * 0.02f
        val buttonsY   = screenH * 0.70f
        val btnH       = screenH * 0.08f
        val btnWFrac   = 0.8f
        val btnSpacer  = screenH * 0.02f

        // 1) 로고 & 텍스트
        Image(
            painter = painterResource(R.drawable.rogo),
            contentDescription = "로고",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(logoSize)
                .align(Alignment.TopCenter)
                .offset(y = logoY)
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

        // 2) 로그인 폼
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
            Spacer(Modifier.height(fieldSpacer))

            Text("이메일 주소", fontSize = 16.sp)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("example@mail.com") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fieldH)
            )
            Spacer(Modifier.height(fieldSpacer))

            Text("비밀번호", fontSize = 16.sp)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("••••••••") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fieldH)
            )
        }

        // 3) 버튼 그룹
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = buttonsY)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(btnSpacer)
        ) {
            Button(
                onClick = {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(ctx, "이메일과 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    scope.launch {
                        try {
                            // A) Firebase 로그인
                            firebaseAuth
                                .signInWithEmailAndPassword(email.trim(), password)
                                .await()
                            // B) 백엔드 토큰 검증
                            authViewModel.verifyToken()
                        } catch (e: Exception) {
                            Toast.makeText(ctx, "로그인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(btnWFrac)
                    .height(btnH),
                shape = RoundedCornerShape(btnH / 2),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("로그인", color = Color.White, fontSize = 16.sp)
            }

            Button(
                onClick = { navController.navigate("guardianSignUp") },
                modifier = Modifier
                    .fillMaxWidth(btnWFrac)
                    .height(btnH),
                shape = RoundedCornerShape(btnH / 2),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("회원가입", color = Color.White, fontSize = 16.sp)
            }
        }
    }

    // 4) ViewModel 상태 처리
    when (state) {
        is AuthState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is AuthState.VerifiedRole -> {
            LaunchedEffect(state) {
                val role = (state as AuthState.VerifiedRole).role
                val dest = if (role == "guardian") "guardianHome" else "patientHome"
                navController.navigate(dest) {
                    popUpTo("guardianLogin") { inclusive = true }
                }
                authViewModel.resetState()
            }
        }
        is AuthState.Error -> {
            LaunchedEffect(state) {
                Toast.makeText(ctx, (state as AuthState.Error).error, Toast.LENGTH_LONG).show()
                authViewModel.resetState()
            }
        }
        else -> { /* Idle */ }
    }
}





