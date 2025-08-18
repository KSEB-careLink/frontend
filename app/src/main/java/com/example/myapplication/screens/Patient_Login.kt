// PatientLoginScreen.kt
package com.example.myapplication.screens

import android.content.Context
import android.util.Log
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.google.firebase.messaging.FirebaseMessaging
import com.example.myapplication.service.NotificationService
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

@Composable
fun PatientLoginScreen(navController: NavController) {
    val auth = Firebase.auth
    val client = remember { OkHttpClient() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var email     by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // ✅ 자동 로그인: 이미 로그인된 세션이 있으면 /auth/me 확인 후 바로 이동 (+ FCM 토큰 송신 추가)
    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val user = auth.currentUser
            if (user != null) {
                val idToken = user.getIdToken(true).await().token
                if (idToken != null) {
                    val meReq = Request.Builder()
                        .url("${BuildConfig.BASE_URL}/auth/me")
                        .addHeader("Authorization", "Bearer $idToken")
                        .get()
                        .build()
                    val meResp = withContext(Dispatchers.IO) { client.newCall(meReq).execute() }
                    if (meResp.isSuccessful) {
                        val meJson = JSONObject(meResp.body?.string() ?: "{}")
                        val role = meJson.optString("role")
                        if (role == "patient") {
                            val patientId = meJson.optString("uid").takeIf { it.isNotBlank() }
                                ?: throw Exception("응답에 uid가 없습니다")

                            // 🔔 자동 로그인 시에도 FCM 토큰 전송
                            runCatching {
                                FirebaseMessaging.getInstance().token.await()
                            }.onSuccess { token ->
                                Log.d("FCM", "자동 로그인 FCM 토큰: ${token.take(12)}...")
                                NotificationService.sendFcmTokenToServer(context, token)
                            }.onFailure { e ->
                                Log.e("FCM", "자동 로그인 FCM 토큰 획득 실패", e)
                            }

                            // 저장
                            context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("patientId", patientId)
                                .apply()

                            // 바로 sentence/{patientId}로
                            navController.navigate("sentence/$patientId") {
                                popUpTo("p_login") { inclusive = true }
                            }
                            return@LaunchedEffect
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // 조용히 무시하고 로그인 화면 표시
        } finally {
            isLoading = false
        }
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        val (
            logo, textLogo, title,
            emailLabel, emailField,
            passwordLabel, passwordField,
            loginButton, registerButton, loadingBox
        ) = createRefs()

        // 로고
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(160.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 154.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                }
        )
        // 텍스트 로고
        Image(
            painter = painterResource(id = R.drawable.ai_text),
            contentDescription = "텍스트 로고",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(120.dp)
                .constrainAs(textLogo) {
                    top.linkTo(logo.bottom, margin = -54.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                }
        )
        // 제목
        Text(
            text = "어르신 로그인",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(textLogo.bottom, margin = -20.dp)
                start.linkTo(parent.start); end.linkTo(parent.end)
            }
        )

        // 이메일 라벨
        Text(
            text = "이메일 주소",
            fontSize = 16.sp,
            modifier = Modifier.constrainAs(emailLabel) {
                top.linkTo(title.bottom, margin = 24.dp)
                start.linkTo(parent.start)
            }
        )
        // 이메일 입력
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            textStyle = TextStyle(color = Color.Black),
            placeholder = { Text("example@mail.com 형식으로 입력하세요", color = Color.Gray) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(emailField) {
                    top.linkTo(emailLabel.bottom, margin = 8.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
        )

        // 비밀번호 라벨
        Text(
            text = "비밀번호",
            fontSize = 16.sp,
            modifier = Modifier.constrainAs(passwordLabel) {
                top.linkTo(emailField.bottom, margin = 16.dp)
                start.linkTo(parent.start)
            }
        )
        // 비밀번호 입력
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            textStyle = TextStyle(color = Color.Black),
            placeholder = { Text("6글자 이상 입력하세요", color = Color.Gray) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(passwordField) {
                    top.linkTo(passwordLabel.bottom, margin = 8.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
        )

        // 로그인 버튼
        Button(
            onClick = {
                coroutineScope.launch {
                    if (email.isBlank() || password.isBlank()) {
                        Toast.makeText(context, "이메일과 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    isLoading = true

                    try {
                        // 1) Firebase Authentication
                        auth.signInWithEmailAndPassword(email.trim(), password.trim()).await()
                        val user = auth.currentUser
                            ?: throw Exception("Firebase user is null")

                        // ✅ 로그인 직후: 최신 FCM 토큰 받아서 서버로 전달
                        runCatching {
                            FirebaseMessaging.getInstance().token.await()
                        }.onSuccess { token ->
                            Log.d("FCM", "환자 로그인 직후 토큰: ${token.take(12)}...")
                            NotificationService.sendFcmTokenToServer(context, token)
                        }.onFailure { e ->
                            Log.e("FCM", "환자 로그인 직후 토큰 획득 실패", e)
                        }

                        // 2) ID 토큰 갱신 후 획득
                        val idToken = user.getIdToken(true).await().token
                            ?: throw Exception("ID 토큰이 null입니다")

                        // 3) /auth/me 호출
                        val meReq = Request.Builder()
                            .url("${BuildConfig.BASE_URL}/auth/me")
                            .addHeader("Authorization", "Bearer $idToken")
                            .get()
                            .build()
                        val meResp = withContext(Dispatchers.IO) { client.newCall(meReq).execute() }
                        if (!meResp.isSuccessful) throw Exception("me 실패: ${meResp.code}")

                        val meJson = JSONObject(meResp.body?.string() ?: "{}")
                        val role = meJson.optString("role")
                        if (role != "patient") {
                            Toast.makeText(context, "환자 계정이 아닙니다", Toast.LENGTH_SHORT).show()
                            isLoading = false
                            return@launch
                        }

                        // 4) patientId 로 사용할 uid 추출
                        val patientId = meJson.optString("uid").takeIf { it.isNotBlank() }
                            ?: throw Exception("응답에 uid가 없습니다")

                        // SharedPreferences에 patientId 저장
                        context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                            .edit()
                            .putString("patientId", patientId)
                            .apply()

                        Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()

                        // 5) code2/{patientId} 화면으로 이동
                        navController.navigate("code2/$patientId") {
                            popUpTo("p_login") { inclusive = true }
                        }

                    } catch (e: Exception) {
                        Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(loginButton) {
                    top.linkTo(passwordField.bottom, margin = 32.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("로그인", color = Color.White, fontSize = 16.sp)
        }

        // 회원가입 버튼
        Button(
            onClick = { navController.navigate("patient") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(registerButton) {
                    top.linkTo(loginButton.bottom, margin = 16.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("회원가입", color = Color.White, fontSize = 16.sp)
        }

        // 로딩 오버레이
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000))
                    .constrainAs(loadingBox) {
                        top.linkTo(parent.top); bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start); end.linkTo(parent.end)
                    },
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPatientLogin() {
    PatientLoginScreen(navController = rememberNavController())
}










