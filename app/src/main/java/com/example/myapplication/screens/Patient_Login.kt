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
                    try {
                        val json = JSONObject()
                            .put("email", email.trim())
                            .put("password", password.trim())
                        val requestBody = json.toString()
                            .toRequestBody("application/json".toMediaTypeOrNull())

                        val loginRequest = Request.Builder()
                            .url("${BuildConfig.BASE_URL}/auth/login")
                            .post(requestBody)
                            .build()

                        client.newCall(loginRequest).enqueue(object : Callback {
                            override fun onFailure(call: Call, e: IOException) {
                                coroutineScope.launch {
                                    withContext(Dispatchers.Main) {
                                        isLoading = false
                                        Toast.makeText(context, "서버 연결 실패", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }

                            override fun onResponse(call: Call, response: Response) {
                                coroutineScope.launch {
                                    withContext(Dispatchers.Main) {
                                        if (!response.isSuccessful) {
                                            isLoading = false
                                            Toast.makeText(context, "로그인 실패", Toast.LENGTH_SHORT).show()
                                            return@withContext
                                        }

                                        val body = response.body?.string() ?: ""
                                        val jsonRes = JSONObject(body)
                                        val token = jsonRes.optString("token")
                                        val uid = jsonRes.optString("uid")

                                        if (token.isNotEmpty() && uid.isNotEmpty()) {
                                            saveTokenToPrefs(context, token)

                                            // 🔐 Firebase 커스텀 토큰 발급 요청
                                            val tokenRequestJson = JSONObject().put("uid", uid)
                                            val tokenReqBody = tokenRequestJson.toString()
                                                .toRequestBody("application/json".toMediaTypeOrNull())

                                            val firebaseTokenReq = Request.Builder()
                                                .url("${BuildConfig.BASE_URL}/auth/generateFirebaseToken")
                                                .post(tokenReqBody)
                                                .build()

                                            client.newCall(firebaseTokenReq).enqueue(object : Callback {
                                                override fun onFailure(call: Call, e: IOException) {
                                                    coroutineScope.launch {
                                                        withContext(Dispatchers.Main) {
                                                            isLoading = false
                                                            Toast.makeText(context, "커스텀 토큰 발급 실패", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                }

                                                override fun onResponse(call: Call, response: Response) {
                                                    coroutineScope.launch {
                                                        withContext(Dispatchers.Main) {
                                                            val tokenBody = response.body?.string() ?: ""
                                                            val firebaseToken = JSONObject(tokenBody).optString("token")
                                                            if (firebaseToken.isNotEmpty()) {
                                                                // ✅ Firebase 로그인
                                                                auth.signInWithCustomToken(firebaseToken)
                                                                    .addOnSuccessListener {
                                                                        // 🔎 사용자 정보 확인
                                                                        val infoRequest = Request.Builder()
                                                                            .url("${BuildConfig.BASE_URL}/auth/me")
                                                                            .addHeader("Authorization", "Bearer $token")
                                                                            .get()
                                                                            .build()

                                                                        client.newCall(infoRequest).enqueue(object : Callback {
                                                                            override fun onFailure(call: Call, e: IOException) {
                                                                                coroutineScope.launch {
                                                                                    withContext(Dispatchers.Main) {
                                                                                        isLoading = false
                                                                                        Toast.makeText(context, "정보 조회 실패", Toast.LENGTH_SHORT).show()
                                                                                    }
                                                                                }
                                                                            }

                                                                            override fun onResponse(call: Call, response: Response) {
                                                                                coroutineScope.launch {
                                                                                    withContext(Dispatchers.Main) {
                                                                                        isLoading = false
                                                                                        val infoBody = response.body?.string()
                                                                                        val role = JSONObject(infoBody ?: "{}").optString("role")
                                                                                        if (role == "patient") {
                                                                                            Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()
                                                                                            navController.navigate("code2")
                                                                                        } else {
                                                                                            Toast.makeText(context, "환자 계정이 아닙니다", Toast.LENGTH_SHORT).show()
                                                                                        }
                                                                                    }
                                                                                }
                                                                            }
                                                                        })
                                                                    }
                                                                    .addOnFailureListener {
                                                                        isLoading = false
                                                                        Toast.makeText(context, "Firebase 로그인 실패", Toast.LENGTH_SHORT).show()
                                                                    }
                                                            } else {
                                                                isLoading = false
                                                                Toast.makeText(context, "커스텀 토큰 없음", Toast.LENGTH_SHORT).show()
                                                            }
                                                        }
                                                    }
                                                }
                                            })
                                        } else {
                                            isLoading = false
                                            Toast.makeText(context, "서버 응답 오류", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                        })
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










