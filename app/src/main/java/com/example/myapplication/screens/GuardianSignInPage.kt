package com.example.myapplication.screens

import android.util.Patterns
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.example.myapplication.network.RetrofitInstance
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun GuardianSignInPage(navController: NavController) {
    // Firebase Auth & CoroutineScope
    val auth = Firebase.auth
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // 레이아웃 상수
    val imageGroupTopPadding = 80.dp
    val formTopPadding = 300.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // 로고
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

        // 폼
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = formTopPadding)
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
                placeholder = { Text("최소 6자 이상 입력해주세요") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true
            )
        }

        // 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-140).dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1) 가입하기: Firebase + 백엔드
            Button(
                onClick = {
                    // 1) 빈 값 체크
                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // 2) 이메일 포맷 검증
                    val trimmedEmail = email.trim()
                    if (!Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                        Toast.makeText(context, "유효한 이메일 주소를 입력하세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    // 3) Firebase 회원가입
                    auth.createUserWithEmailAndPassword(trimmedEmail, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // 4) 백엔드 보호자 가입
                                coroutineScope.launch {
                                    try {
                                        val res = RetrofitInstance.api.signupGuardian(trimmedEmail, password)
                                        if (res.isSuccessful) {
                                            Toast.makeText(context, "가입 성공!", Toast.LENGTH_SHORT).show()
                                            navController.navigate("G_login") {
                                                popUpTo("GuardianSignInPage") { inclusive = true }
                                            }
                                        } else {
                                            Toast.makeText(context, "서버 오류: ${res.code()}", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "통신 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "Firebase 가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                },
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(text = "가입하기", color = Color.White, fontSize = 16.sp)
            }

            // 2) 로그인으로 이동
            Button(
                onClick = { navController.navigate("G_login") },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(text = "로그인", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewGuardianSignInPage() {
    GuardianSignInPage(navController = rememberNavController())
}

