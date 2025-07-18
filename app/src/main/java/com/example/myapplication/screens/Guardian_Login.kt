package com.example.myapplication.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import okhttp3.*
import org.json.JSONObject
import java.io.IOException

@Composable
fun Guardian_Login(navController: NavController) {
    val auth = Firebase.auth
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val client = remember { OkHttpClient() }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        val (logo, textLogo, title, emailLabel, emailField, passwordLabel, passwordField,
            loginButton, registerButton) = createRefs()

        // 로고 이미지
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

        // 타이틀
        Text(
            text = "보호자 로그인",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
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

        // 이메일 입력창
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("example@mail.com") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(emailField) {
                    top.linkTo(emailLabel.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            singleLine = true
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

        // 비밀번호 입력창
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("••••••••") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(passwordField) {
                    top.linkTo(passwordLabel.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            singleLine = true
        )

        // 로그인 버튼
        Button(
            onClick = {
                coroutineScope.launch {
                    val emailTrimmed = email.trim()
                    val passwordTrimmed = password.trim()

                    try {
                        auth.signInWithEmailAndPassword(emailTrimmed, passwordTrimmed)
                            .addOnSuccessListener { result ->
                                result.user?.getIdToken(true)?.addOnSuccessListener { tokenResult ->
                                    val idToken = tokenResult.token
                                    val request = Request.Builder()
                                        .url("${BuildConfig.BASE_URL}/auth/me")
                                        .addHeader("Authorization", "Bearer $idToken")
                                        .get()
                                        .build()

                                    client.newCall(request).enqueue(object : Callback {
                                        override fun onFailure(call: Call, e: IOException) {
                                            coroutineScope.launch {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "서버 연결 실패", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }

                                        override fun onResponse(call: Call, response: Response) {
                                            coroutineScope.launch {
                                                withContext(Dispatchers.Main) {
                                                    if (response.isSuccessful) {
                                                        val body = response.body?.string()
                                                        val json = JSONObject(body ?: "{}")
                                                        val role = json.optString("role")
                                                        val joinCode = json.optString("joinCode") // 🔹 joinCode 추출

                                                        if (role == "guardian" && joinCode.isNotBlank()) {
                                                            Toast.makeText(context, "로그인 성공!", Toast.LENGTH_SHORT).show()
                                                            navController.navigate("code/$joinCode") // 🔹 화면 이동
                                                        } else {
                                                            Toast.makeText(context, "잘못된 사용자 역할 또는 joinCode 없음", Toast.LENGTH_SHORT).show()
                                                        }
                                                    } else {
                                                        Toast.makeText(context, "사용자 정보 조회 실패", Toast.LENGTH_SHORT).show()
                                                    }
                                                }
                                            }
                                        }

                                    })
                                }
                            }
                            .addOnFailureListener {
                                Toast.makeText(context, "로그인 실패", Toast.LENGTH_SHORT).show()
                            }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
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
            onClick = { navController.navigate("guardianSignup") },
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
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLogin2() {
    Guardian_Login(navController = rememberNavController())
}


