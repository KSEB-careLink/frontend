package com.example.myapplication.screens

import android.util.Log
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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import androidx.compose.ui.text.TextStyle

@Composable
fun PatientSignUpScreen(navController: NavController) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }
    val auth = Firebase.auth

    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var agreePhoto by remember { mutableStateOf(false) }
    var agreeProfile by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // 약관 다이얼로그 표시 여부
    var showPhotoTerms by remember { mutableStateOf(false) }
    var showProfileTerms by remember { mutableStateOf(false) }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val logoSize = screenW * 0.5f
        val textLogoSize = screenW * 0.3f
        val logoY = screenH * 0.04f
        val textY = screenH * 0.18f
        val formY = screenH * 0.25f
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

        // 입력 폼 및 체크박스
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = formY)
                .fillMaxWidth(0.9f),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(fieldSpacer))
            Text(
                "어르신 회원가입",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(Modifier.height(fieldSpacer))

            Text("이름", fontSize = 16.sp)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.trim() },
                placeholder = { Text("이름을 입력하세요") },
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
                textStyle = TextStyle(color = Color.Black),
                placeholder = { Text("example@mail.com 형식으로 입력하세요", color = Color.Gray) },
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
                textStyle = TextStyle(color = Color.Black),
                placeholder = { Text("6글자 이상 입력하세요", color = Color.Gray) },
                visualTransformation = PasswordVisualTransformation(),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(fieldHeight)
            )
            Spacer(Modifier.height(fieldSpacer))

            // 동의 문구
            Text(
                "본 앱에서는 다음 정보를 수집·이용합니다.\n동의하지 않을 시 회원가입이 불가능 합니다",
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(Modifier.height(fieldSpacer / 2))

            // 체크박스 왼쪽, 오른쪽에 '보기' 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreePhoto,
                    onCheckedChange = { agreePhoto = it }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "사진 및 메모리 정보",
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showPhotoTerms = true }) {
                    Text("보기")
                }
            }

            Spacer(Modifier.height(fieldSpacer / 2))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = agreeProfile,
                    onCheckedChange = { agreeProfile = it }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "환자 프로필 정보",
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { showProfileTerms = true }) {
                    Text("보기")
                }
            }

            Spacer(Modifier.height(fieldSpacer))

            // 버튼들
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = fieldSpacer)
                    .padding(bottom = 60.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        if (name.isBlank() || email.isBlank() || password.length < 6 || !agreePhoto || !agreeProfile) {
                            Toast.makeText(
                                ctx,
                                "모든 필드를 올바르게 입력하고 동의가 필요합니다.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@Button
                        }
                        scope.launch {
                            isLoading = true
                            try {
                                val signupJson = JSONObject().apply {
                                    put("email", email)
                                    put("password", password)
                                    put("name", name)
                                    put("role", "patient")
                                }
                                Log.d("PatientSignUp", "보내는 JSON: $signupJson")
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
                                val responseBody = signupRes.body?.string()
                                Log.d("PatientSignUp", "응답 코드: ${signupRes.code}, 바디: $responseBody")
                                if (!signupRes.isSuccessful) throw Exception("signup 실패: ${signupRes.code}")

                                // (현재 구조 유지) Firebase 이메일/비번 로그인
                                auth.signInWithEmailAndPassword(email, password).await()
                                auth.currentUser?.getIdToken(true)?.await()?.token
                                    ?: throw Exception("ID 토큰 획득 실패")

                                Toast.makeText(ctx, "회원가입 및 로그인 성공!", Toast.LENGTH_SHORT).show()
                                navController.navigate("code2/{patientId}") {
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
                    modifier = Modifier
                        .weight(1f)
                        .height(fieldHeight),
                    enabled = name.isNotBlank() &&
                            Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                            password.length >= 6 &&
                            agreePhoto && agreeProfile,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                ) {
                    Text("가입하기", color = Color.White, fontSize = 16.sp)
                }
                Button(
                    onClick = { navController.navigate("p_login") },
                    modifier = Modifier
                        .weight(1f)
                        .height(fieldHeight),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                ) {
                    Text("로그인", color = Color.White, fontSize = 16.sp)
                }
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

    // 약관 다이얼로그들
    if (showPhotoTerms) {
        TermsDialog(
            title = "사진 및 메모리 정보 수집·이용 동의",
            text = "• 사진, 동영상, 메모리 설명 텍스트를 수집합니다.\n" +
                    "• 서비스 기능 제공(회상 콘텐츠 생성, 퀴즈 생성 등)에 사용됩니다.\n" +
                    "• 보관 기간 및 파기 등은 개인정보 처리방침을 따릅니다.",
            onDismiss = { showPhotoTerms = false }
        )
    }
    if (showProfileTerms) {
        TermsDialog(
            title = "환자 프로필 정보 수집·이용 동의",
            text = "• 이름, 관계, 기본 프로필 정보 등을 수집합니다.\n" +
                    "• 사용자 식별, 콘텐츠 개인화, 보호자 연동에 사용됩니다.\n" +
                    "• 자세한 내용은 개인정보 처리방침을 확인하세요.",
            onDismiss = { showProfileTerms = false }
        )
    }
}

@Composable
private fun TermsDialog(
    title: String,
    text: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = { Text(text, fontSize = 14.sp) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("닫기") }
        }
    )
}





















