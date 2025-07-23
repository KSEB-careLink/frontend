package com.example.myapplication.screens

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
import kotlinx.coroutines.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import kotlinx.coroutines.tasks.await
@Composable
fun PatientLoginScreen(navController: NavController) {
    val auth = Firebase.auth
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val client = remember { OkHttpClient() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun saveTokenToPrefs(context: Context, token: String) {
        val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("jwt_token", token).apply()
    }

    ConstraintLayout(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp)
    ) {
        val (logo, textLogo, title, emailLabel, emailField, passwordLabel, passwordField,
            loginButton, registerButton, loadingBox) = createRefs()

        // 로고
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(160.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 154.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
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
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        // 제목
        Text(
            text = "어르신 로그인",
            fontSize = 28.sp,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(textLogo.bottom, margin = -20.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
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
            onValueChange = { email = it.trim() },
            placeholder = { Text("example@mail.com 형식으로 입력하세요") },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(emailField) {
                    top.linkTo(emailLabel.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
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
            onValueChange = { password = it.trim() },
            placeholder = { Text("6글자 이상 입력하세요") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(passwordField) {
                    top.linkTo(passwordLabel.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
        )

        // 로그인 버튼
        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    val emailTrimmed = email.trim()
                    val passwordTrimmed = password.trim()
                    try {
                        // 1) Firebase 로그인
                        auth.signInWithEmailAndPassword(emailTrimmed, passwordTrimmed).await()
                        val user = auth.currentUser
                            ?: throw Exception("Firebase user is null")

                        // 2) ID 토큰 강제 갱신 후 획득 (커스텀 클레임 포함)
                        val idToken = user.getIdToken(true).await().token
                            ?: throw Exception("ID 토큰이 null입니다")

                        // 3) 서버에서 /auth/me 호출
                        val meRequest = Request.Builder()
                            .url("${BuildConfig.BASE_URL}/auth/me")
                            .addHeader("Authorization", "Bearer $idToken")
                            .get()
                            .build()

                        val meResponse = withContext(Dispatchers.IO) {
                            client.newCall(meRequest).execute()
                        }
                        if (!meResponse.isSuccessful) {
                            throw Exception("me 실패: ${meResponse.code}")
                        }

                        val meJson = JSONObject(meResponse.body?.string() ?: "{}")
                        val role = meJson.optString("role")

                        isLoading = false
                        if (role == "patient") {
                            Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()
                            navController.navigate("code2") {
                                popUpTo("PatientLogin") { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "환자 계정이 아닙니다", Toast.LENGTH_SHORT).show()
                        }

                    } catch (e: Exception) {
                        isLoading = false
                        Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(loginButton) {
                    top.linkTo(passwordField.bottom, margin = 32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
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
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("회원가입", color = Color.White, fontSize = 16.sp)
        }

        // 로딩 인디케이터
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000))
                    .constrainAs(loadingBox) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
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










