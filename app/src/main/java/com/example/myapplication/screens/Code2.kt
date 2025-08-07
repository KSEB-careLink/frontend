package com.example.myapplication.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import androidx.compose.foundation.background

@Composable
fun Code2(
    navController: NavController,
    patientId: String
) {
    var code by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }
    val auth = Firebase.auth

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Spacer(modifier = Modifier.height(164.dp))

        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            modifier = Modifier.size(160.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "보호자 코드 입력",
            fontSize = 22.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(6) { i ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF333333), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = code.getOrNull(i)?.toString() ?: "",
                        color = Color.White,
                        fontSize = 22.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = { Text("6자리 코드") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(46.dp))

        Button(
            onClick = {
                scope.launch {
                    if (code.length != 6) {
                        Toast.makeText(context, "6자리를 모두 입력해주세요", Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    isLoading = true
                    try {
                        // Firebase ID 토큰 획득
                        val user = auth.currentUser
                            ?: throw Exception("로그인된 유저가 없습니다")
                        val idToken = user.getIdToken(true).await().token
                            ?: throw Exception("토큰이 비어있습니다")

                        // /link/patient 요청
                        val linkJson = JSONObject().put("joinCode", code).toString()
                        val linkBody = linkJson.toRequestBody("application/json".toMediaType())
                        val linkReq = Request.Builder()
                            .url("${BuildConfig.BASE_URL}/link/patient")
                            .addHeader("Authorization", "Bearer $idToken")
                            .post(linkBody)
                            .build()
                        val linkResp = withContext(Dispatchers.IO) {
                            client.newCall(linkReq).execute()
                        }

                        if (linkResp.isSuccessful) {
                            Toast.makeText(context, "보호자와 연결 완료!", Toast.LENGTH_SHORT).show()

                            // JWT 토큰 저장
                            context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                                .edit()
                                .putString("jwt_token", idToken)
                                .apply()

                            // 다음 화면으로 이동
                            navController.navigate("sentence/$patientId") {
                                popUpTo("code2/$patientId") { inclusive = true }
                            }
                        } else {
                            val err = JSONObject(linkResp.body?.string().orEmpty())
                                .optString("error", "연결 실패")
                            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = code.length == 6 && !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("보호자와 연결", color = Color.White, fontSize = 16.sp)
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        }
    }
}





