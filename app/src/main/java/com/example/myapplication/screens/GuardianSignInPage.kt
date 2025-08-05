package com.example.myapplication.screens

import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun GuardianSignUpScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var agreePhoto by remember { mutableStateOf(false) }
    var agreeProfile by remember { mutableStateOf(false) }
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
        val logoY = screenH * 0.04f   // 상단 로고 약간 위로
        val textY = screenH * 0.18f   // 텍스트 로고 위로
        val formY = screenH * 0.25f   // 입력 폼 더 위로 이동
        val fieldHeight = screenH * 0.07f
        val fieldSpacer = screenH * 0.02f

        // 로고
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

        // 입력 폼 + 동의 체크박스까지
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = formY)
                .fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(fieldSpacer))
            Text(
                "보호자 회원가입", fontSize = 32.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(fieldSpacer))

            Text("이름", fontSize = 16.sp)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.trim() },
                placeholder = { Text("이름") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(fieldHeight)
            )
            Spacer(Modifier.height(fieldSpacer))

            Text("이메일 주소", fontSize = 16.sp)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it.trim() },
                placeholder = { Text("example@mail.com 형식으로 입력하세요") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().height(fieldHeight)
            )
            Spacer(Modifier.height(fieldSpacer))

            Text("비밀번호", fontSize = 16.sp)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it.trim() },
                placeholder = { Text("6글자 이상 입력하세요") },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier.fillMaxWidth().height(fieldHeight)
            )
            Spacer(Modifier.height(fieldSpacer))

            // 동의 문구 및 체크박스
            Text("본 앱에서는 다음 정보를 수집·이용합니다", fontSize = 14.sp)
            Spacer(Modifier.height(fieldSpacer / 2))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("사진 및 메모리 정보", fontSize = 16.sp)
                Checkbox(checked = agreePhoto, onCheckedChange = { agreePhoto = it })
            }
            Spacer(Modifier.height(fieldSpacer / 2))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("환자 프로필 정보", fontSize = 16.sp)
                Checkbox(checked = agreeProfile, onCheckedChange = { agreeProfile = it })
            }
            Spacer(Modifier.height(fieldSpacer))

            // 버튼
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (name.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches() || password.length < 6 || !agreePhoto || !agreeProfile) {
                            Toast.makeText(
                                context,
                                "모든 필드 입력 및 동의가 필요합니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        scope.launch {
                            isLoading = true
                            try {
                                // 서버 회원가입 & joinCode
                                val signupJson = JSONObject().apply {
                                    put("email", email)
                                    put("password", password)
                                    put("name", name)
                                    put("role", "guardian")
                                }
                                val req = Request.Builder()
                                    .url("${BuildConfig.BASE_URL}/auth/signup")
                                    .post(signupJson.toString().toRequestBody("application/json".toMediaType()))
                                    .build()
                                val res = withContext(Dispatchers.IO) { client.newCall(req).execute() }
                                val body = res.body?.string().orEmpty()
                                if (!res.isSuccessful) throw Exception("signup 실패 ${res.code}")
                                val joinCode = JSONObject(body).optString("joinCode").takeIf { it.isNotBlank() }
                                    ?: throw Exception("joinCode 누락")

                                // Firebase 로그인 & 토큰 갱신
                                Firebase.auth.signInWithEmailAndPassword(email, password).await()
                                Firebase.auth.currentUser?.getIdToken(true)?.await()?.token
                                    ?: throw Exception("ID 토큰 획득 실패")

                                Toast.makeText(context, "회원가입 및 로그인 성공!", Toast.LENGTH_SHORT).show()
                                navController.navigate("code/$joinCode") {
                                    popUpTo("GuardianSignUp") { inclusive = true }
                                }
                            } catch (e: Exception) {
                                Log.e("GuardianSignUp", "오류", e)
                                Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_LONG).show()
                            } finally {
                                isLoading = false
                            }
                        }
                    },
                    modifier = Modifier.weight(1f).height(fieldHeight),
                    enabled = name.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches() && password.length >= 6 && agreePhoto && agreeProfile,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                ) {
                    Text("가입하기", color = Color.White, fontSize = 16.sp)
                }
                Button(
                    onClick = { navController.navigate("G_login") },
                    modifier = Modifier.weight(1f).height(fieldHeight),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                ) {
                    Text("로그인", color = Color.White, fontSize = 16.sp)
                }
            }
        }

        // 로딩
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









