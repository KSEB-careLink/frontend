// app/src/main/java/com/example/myapplication/screens/PatientSignUpScreen.kt
package com.example.myapplication.screens

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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

@Composable
fun PatientSignUpScreen(
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    val auth    = Firebase.auth
    val context = LocalContext.current

    // 입력 필드 상태
    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // ViewModel 상태 구독
    val state by authViewModel.state.collectAsState()

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 24.dp)
    ) {
        // 이미지 그룹
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp),
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

        // 입력 폼
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 300.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = "회원 가입",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(24.dp))

            Text("이름", fontSize = 16.sp)
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

            Text("이메일 주소 (아이디)", fontSize = 16.sp)
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
                placeholder = { Text("최소 6자 이상 입력해주세요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true
            )
        }

        // 버튼 그룹
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-140).dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = {
                    // 1) 빈 값 체크
                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // 2) 이메일 포맷 검증
                    if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                        Toast.makeText(context, "유효한 이메일 주소를 입력하세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // 3) Firebase 가입 → ViewModel.signup 호출
                    auth.createUserWithEmailAndPassword(email.trim(), password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                authViewModel.signup(
                                    name     = name.trim(),
                                    email    = email.trim(),
                                    password = password,
                                    role     = "patient"
                                )
                            } else {
                                Toast.makeText(
                                    context,
                                    "Firebase 가입 실패: ${task.exception?.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                },
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 6,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(text = "가입하기", color = Color.White, fontSize = 16.sp)
            }

            Button(
                onClick = { navController.navigate("p_login") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(text = "로그인", color = Color.White, fontSize = 16.sp)
            }
        }

        // 상태별 오버레이 및 내비게이션
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
            is AuthState.PatientSignedUp -> {
                LaunchedEffect(state) {
                    Toast.makeText(
                        context,
                        (state as AuthState.PatientSignedUp).message,
                        Toast.LENGTH_SHORT
                    ).show()
                    authViewModel.resetState()
                    navController.navigate("p_login") {
                        popUpTo("PatientSignUp") { inclusive = true }
                    }
                }
            }
            is AuthState.Error -> {
                LaunchedEffect(state) {
                    Toast.makeText(
                        context,
                        (state as AuthState.Error).error,
                        Toast.LENGTH_LONG
                    ).show()
                    authViewModel.resetState()
                }
            }
            else -> { /* Idle */ }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPatientSignUp() {
    PatientSignUpScreen(navController = rememberNavController())
}







