// app/src/main/java/com/example/myapplication/screens/Guardian_Login.kt
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
import com.google.firebase.messaging.FirebaseMessaging
import com.example.myapplication.service.NotificationService

// 🔽 추가 import
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.ktx.Firebase as FirebaseAlias // 이름 충돌 방지용 별칭(선택)
import com.google.firebase.firestore.DocumentSnapshot

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

                        // 로그인 직후: 최신 FCM 토큰 받아서 서버에 전달 (선택)
                        runCatching {
                            FirebaseMessaging.getInstance().token.await()
                        }.onSuccess { token ->
                            Log.d("FCM", "로그인 직후 토큰: ${token.take(12)}...")
                            NotificationService.sendFcmTokenToServer(context, token)
                        }.onFailure { e ->
                            Log.e("FCM", "로그인 직후 토큰 획득 실패", e)
                        }

                        // 2) ID 토큰 획득
                        val idToken = user.getIdToken(true).await().token
                            ?: throw Exception("ID 토큰이 null입니다")
                        Log.d("GuardianLogin", "발급받은 ID토큰: ${idToken.take(12)}...")

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

                                // ✅ 4) 앱 진입 시 1회 동기화: guardians → patients voiceId 백필
                                val mirrored = ensureVoiceIdMirroredOnce(
                                    patientId = patientId,
                                    guardianUid = guardianId
                                )

                                if (mirrored) {
                                    Log.d("GuardianLogin", "voiceId 미러링 완료(patients/$patientId)")
                                    showToast("로그인 성공! (음성 동기화 완료)")
                                } else {
                                    Log.w("GuardianLogin", "voiceId 미러링 스킵/실패")
                                    showToast("로그인 성공! (음성 동기화 확인 필요)")
                                }

                                // 기존 네비 흐름 유지
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

/**
 * ✅ 앱 진입 시 1회 동기화:
 * - patients/{patientId}.voiceId 가 비어 있으면
 * - guardians/{guardianUid}.voiceId 를 읽어와 patients 쪽에 SetOptions.merge()로 백필
 * - 이미 값이 있으면 건드리지 않음
 * @return true = 이미 있었거나 백필 성공, false = guardian에 voiceId가 없거나 오류
 */
private suspend fun ensureVoiceIdMirroredOnce(
    patientId: String,
    guardianUid: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val db = Firebase.firestore

        // 1) 환자 문서에 이미 있으면 스킵
        val pSnap = db.collection("patients").document(patientId).get().await()
        val patientHas = pSnap.getString("voiceId")?.isNotBlank() == true
        if (patientHas) return@withContext true

        // 2) 보호자 문서에서 가져오기
        val gSnap = db.collection("guardians").document(guardianUid).get().await()
        val v = gSnap.getString("voiceId")?.takeIf { it.isNotBlank() } ?: return@withContext false

        // 3) 백필
        db.collection("patients")
            .document(patientId)
            .set(mapOf("voiceId" to v), SetOptions.merge())
            .await()

        true
    } catch (e: Exception) {
        Log.e("GuardianLogin", "voiceId 미러링 실패: ${e.message}", e)
        false
    }
}




