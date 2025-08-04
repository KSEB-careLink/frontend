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
import android.content.Context
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.tasks.await
import android.util.Log

@Composable
fun Guardian_Login(navController: NavController) {
    val auth = Firebase.auth
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val client = remember { OkHttpClient() }

    fun saveUserInfoToPrefs(context: Context, patientId: String, guardianId: String) {
        val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("patient_id", patientId)
            .putString("guardian_id", guardianId)
            .apply()
    }

    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        val (logo, textLogo, title, emailLabel, emailField, passwordLabel, passwordField,
            loginButton, registerButton) = createRefs()

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
            text = "보호자 로그인",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
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
            onValueChange = { email = it },
            placeholder = { Text("example@mail.com 형식으로 입력하세요") },
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
            onValueChange = { password = it },
            placeholder = { Text("6글자 이상 입력하세요") },
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

        Button(
            onClick = {
                coroutineScope.launch {
                    val emailTrimmed = email.trim()
                    val passwordTrimmed = password.trim()

                    try {
                        // 1) Firebase 로그인
                        auth.signInWithEmailAndPassword(emailTrimmed, passwordTrimmed).await()
                        val user = auth.currentUser
                            ?: throw Exception("Firebase user is null")
                        val guardianId = user.uid

                        // 2) ID 토큰 획득
                        val idToken = user.getIdToken(true).await().token
                            ?: throw Exception("ID 토큰이 null입니다")

                        // 3) 서버 /auth/me 요청
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
                        val joinCode = meJson.optString("joinCode")
                        val linkedPatients = meJson.optJSONArray("linkedPatients")

                        if (role == "guardian" && joinCode.isNotBlank()) {
                            if (linkedPatients != null && linkedPatients.length() > 0) {
                                val patientId = linkedPatients.getString(0)
                                saveUserInfoToPrefs(context, patientId, guardianId)

                                showToast("로그인 성공!")
                                navController.navigate("code/$joinCode") {
                                    popUpTo("G_login") { inclusive = true }
                                }
                            } else {
                                showToast("연결된 환자가 없습니다. 먼저 연결을 진행해주세요.")
                            }
                        } else {
                            showToast("역할 또는 코드 오류")
                        }

                    } catch (e: Exception) {
                        Log.e("GuardianLogin", "error", e)
                        Toast.makeText(context, "예외 발생: ${e.message}", Toast.LENGTH_LONG).show()
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



