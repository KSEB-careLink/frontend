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
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.example.myapplication.network.RetrofitInstance
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun GuardianSignInPage(navController: NavController) {
    val auth    = Firebase.auth
    val scope   = rememberCoroutineScope()
    val ctx     = LocalContext.current

    var name     by remember { mutableStateOf("") }
    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenW = maxWidth
        val screenH = maxHeight

        // 이미지 크기 (너비 비율)
        val logoSize     = screenW * 0.5f
        val textLogoSize = screenW * 0.3f

        // Y 오프셋 (높이 비율)
        val logoY    = screenH * 0.10f
        val textY    = screenH * 0.25f
        val formY    = screenH * 0.35f
        val buttonsY = screenH * 0.80f

        // 폼 필드 높이 및 간격
        val fieldHeight  = screenH * 0.07f
        val fieldSpacer  = screenH * 0.02f

        // 버튼 크기 및 간격
        val btnHeight    = screenH * 0.08f
        val btnWidthFrac = 0.45f
        val btnSpacer    = screenH * 0.02f

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
                        // ← 여기가 그대로 유지됩니다!
                        if (name.isBlank() || email.isBlank() || password.isBlank()) {
                            Toast.makeText(ctx, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
                            Toast.makeText(ctx, "유효한 이메일을 입력하세요.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        auth.createUserWithEmailAndPassword(email.trim(), password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    scope.launch {
                                        try {
                                            val res = RetrofitInstance.api.signupGuardian(email.trim(), password)
                                            if (res.isSuccessful) {
                                                Toast.makeText(ctx, "가입 성공!", Toast.LENGTH_SHORT).show()
                                                navController.navigate("G_login") {
                                                    popUpTo("GuardianSignInPage") { inclusive = true }
                                                }
                                            } else {
                                                Toast.makeText(ctx, "서버 오류: ${res.code()}", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(ctx, "통신 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } else {
                                    Toast.makeText(ctx, "Firebase 가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    },
                    modifier = Modifier
                        .width(screenW * btnWidthFrac)  // 여기만 바뀌어요
                        .height(btnHeight),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
                    enabled = name.isNotBlank() && email.isNotBlank() && password.isNotBlank()
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
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewGuardianSignInPage() {
    GuardianSignInPage(navController = rememberNavController())
}


