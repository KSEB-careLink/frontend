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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.util.Log
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.tasks.await

@Composable
fun GuardianSignUpScreen(navController: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

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

        // 로고 이미지
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
                "보호자 회원가입",
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
                    if (name.isBlank() || email.isBlank() || password.isBlank()) {
                        Toast.makeText(ctx, "모든 필드를 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(ctx, "유효한 이메일을 입력하세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (password.length < 6) {
                        Toast.makeText(ctx, "비밀번호를 6자 이상 입력해주세요.", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    scope.launch {
                        isLoading = true
                        try {
                            val json = JSONObject().apply {
                                put("email", email)
                                put("password", password)
                                put("name", name)
                                put("role", "guardian")
                            }

                            val requestBody = json.toString()
                                .toRequestBody("application/json".toMediaType())

                            val request = Request.Builder()
                                .url("${BuildConfig.BASE_URL}/auth/signup")
                                .post(requestBody)
                                .build()

                            val response = withContext(Dispatchers.IO) {
                                client.newCall(request).execute()
                            }

                            val responseBody = withContext(Dispatchers.IO) { response.body?.string() }
                            val jsonRes = JSONObject(responseBody ?: "{}")
                            val uid = jsonRes.optString("uid", "")
                            val joinCode = jsonRes.optString("joinCode", "")

                            if (response.isSuccessful && uid.isNotBlank() && joinCode.isNotBlank()) {
                                // Firebase 커스텀 토큰 요청
                                val tokenReqJson = JSONObject().apply {
                                    put("uid", uid)
                                }
                                val tokenReqBody = tokenReqJson.toString().toRequestBody("application/json".toMediaType())
                                val tokenReq = Request.Builder()
                                    .url("${BuildConfig.BASE_URL}/auth/generateFirebaseToken")
                                    .post(tokenReqBody)
                                    .build()

                                val tokenRes = withContext(Dispatchers.IO) {
                                    client.newCall(tokenReq).execute()
                                }

                                val tokenBody = withContext(Dispatchers.IO) { tokenRes.body?.string() }
                                val tokenJson = JSONObject(tokenBody ?: "{}")
                                val customToken = tokenJson.optString("token")

                                if (tokenRes.isSuccessful && customToken.isNotBlank()) {
                                    try {
                                        Firebase.auth.signInWithCustomToken(customToken).await()
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(ctx, "회원가입 및 로그인 성공!", Toast.LENGTH_SHORT).show()
                                            navController.navigate("code/$joinCode") {
                                                popUpTo("GuardianSignUp") { inclusive = true }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(ctx, "Firebase 로그인 실패: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(ctx, "Firebase 토큰 발급 실패", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }

                        } catch (e: Exception) {
                            Log.e("GuardianSignUp", "회원가입 중 오류", e)
                            Toast.makeText(ctx, "네트워크 오류 또는 서버 오류", Toast.LENGTH_LONG).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.weight(1f),
                enabled = name.isNotBlank() && email.isNotBlank() && password.length >= 6,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("가입하기", color = Color.White, fontSize = 16.sp)
            }

            Button(
                onClick = { navController.navigate("G_login") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("로그인", color = Color.White, fontSize = 16.sp)
            }
        }

        if (isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}









