package com.example.myapplication.screens

import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.myapplication.components.refreshAndSaveToken
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.tasks.await

@Composable
fun PatientSignUpScreen(navController: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }
    val auth = Firebase.auth

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val logoSize = screenW * 0.5f
        val textLogoSize = screenW * 0.3f
        val logoY = screenH * 0.10f
        val textY = screenH * 0.25f
        val formY = screenH * 0.35f
        val fieldHeight = screenH * 0.07f
        val fieldSpacer = screenH * 0.02f

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
                .size(textLogoSize)
                .align(Alignment.TopCenter)
                .offset(y = textY)
        )

        Column(
            Modifier
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
                onValueChange = { name = it.trim() },
                placeholder = { Text("이름") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fieldHeight)
            )
            Spacer(Modifier.height(fieldSpacer))

            Text("이메일 주소", fontSize = 16.sp)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                placeholder = { Text("example@mail.com 형식으로 입력하세요") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fieldHeight)
            )
            Spacer(Modifier.height(fieldSpacer))

            Text("비밀번호", fontSize = 16.sp)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it.trim() },
                placeholder = { Text("6글자 이상 입력하세요") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fieldHeight)
            )
        }

        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 140.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (name.isBlank() || email.isBlank() || password.length < 6) {
                        Toast.makeText(ctx, "모든 필드를 올바르게 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        isLoading = true
                        try {
                            // 1) 백엔드에 회원가입 요청
                            val signupJson = JSONObject().apply {
                                put("email", email)
                                put("password", password)
                                put("name", name)
                                put("role", "patient")
                            }
                            val signupReq = Request.Builder()
                                .url("${BuildConfig.BASE_URL}/auth/signup")
                                .post(
                                    signupJson.toString()
                                        .toRequestBody("application/json".toMediaType())
                                )
                                .build()
                            val signupRes = withContext(Dispatchers.IO) {
                                client.newCall(signupReq).execute()
                            }
                            if (!signupRes.isSuccessful) {
                                throw Exception("signup 실패: ${signupRes.code}")
                            }

                            // 2) Firebase 이메일/비번 로그인
                            auth.signInWithEmailAndPassword(email, password).await()
                            val user = auth.currentUser
                                ?: throw Exception("Firebase user is null")

                            // 3) ID 토큰 강제 갱신 + 저장
                            val idToken = user.refreshAndSaveToken(ctx)

                            // 4) 화면 전환
                            Toast.makeText(ctx, "회원가입 및 로그인 성공!", Toast.LENGTH_SHORT).show()
                            navController.navigate("code2") {
                                popUpTo("patient") { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Log.e("PatientSignUp", "오류", e)
                            Toast.makeText(ctx, "오류: ${e.message}", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches() && password.length >= 6,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("가입하기", color = Color.White, fontSize = 16.sp)
            }

            Button(
                onClick = { navController.navigate("p_login") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("로그인", color = Color.White, fontSize = 16.sp)
            }
        }

        if (isLoading) {
            Box(
                Modifier.fillMaxSize().background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}















