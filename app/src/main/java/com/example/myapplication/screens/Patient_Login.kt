package com.example.myapplication.screens

import android.content.Context
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
import com.example.myapplication.components.refreshAndSaveToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Composable
fun PatientLoginScreen(navController: NavController) {
    val auth = Firebase.auth
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val client = remember { OkHttpClient() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        val (logo, textLogo, title, emailLabel, emailField, passwordLabel, passwordField,
            loginButton, registerButton, loadingBox) = createRefs()

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

        Text(
            text = "어르신 로그인",
            fontSize = 28.sp,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(textLogo.bottom, margin = -20.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        Text(
            text = "이메일 주소",
            fontSize = 16.sp,
            modifier = Modifier.constrainAs(emailLabel) {
                top.linkTo(title.bottom, margin = 24.dp)
                start.linkTo(parent.start)
            }
        )

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

        Text(
            text = "비밀번호",
            fontSize = 16.sp,
            modifier = Modifier.constrainAs(passwordLabel) {
                top.linkTo(emailField.bottom, margin = 16.dp)
                start.linkTo(parent.start)
            }
        )

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

        Button(
            onClick = {
                coroutineScope.launch {
                    isLoading = true
                    try {
                        // 1) Firebase 로그인
                        auth.signInWithEmailAndPassword(email, password).await()
                        val user = auth.currentUser
                            ?: throw Exception("Firebase user is null")

                        // 2) ID 토큰 강제 갱신 + 저장
                        val idToken = user.refreshAndSaveToken(context)

                        // 3) 서버에서 /auth/me 호출
                        val meRequest = Request.Builder()
                            .url("${BuildConfig.BASE_URL}/auth/me")
                            .addHeader("Authorization", "Bearer $idToken")
                            .get()
                            .build()

                        val meResponse = withContext(Dispatchers.IO) {
                            client.newCall(meRequest).execute()
                        }
                        if (!meResponse.isSuccessful) throw Exception("me 실패: ${meResponse.code}")

                        val role = JSONObject(meResponse.body?.string() ?: "{}").optString("role")

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











