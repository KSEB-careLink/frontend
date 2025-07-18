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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaTypeOrNull

@Composable
fun Code2(navController: NavController) {
    var code by remember { mutableStateOf("") }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

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

        Text("보호자 코드 입력", fontSize = 22.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(24.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in 0 until 6) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFF333333), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = code.getOrNull(i)?.toString() ?: "",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = code,
            onValueChange = { if (it.length <= 6) code = it },
            label = { Text("6자리 코드") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(46.dp))

        Button(
            onClick = {
                if (code.length == 6) {
                    coroutineScope.launch {
                        try {
                            val user = Firebase.auth.currentUser
                            user?.getIdToken(true)?.addOnSuccessListener { result ->
                                val idToken = result.token ?: return@addOnSuccessListener

                                val json = JSONObject().apply {
                                    put("joinCode", code)
                                }

                                val request = Request.Builder()
                                    .url("${BuildConfig.BASE_URL}/link/patient")
                                    .post(RequestBody.create("application/json".toMediaTypeOrNull(), json.toString()))
                                    .addHeader("Authorization", "Bearer $idToken")
                                    .build()

                                client.newCall(request).enqueue(object : Callback {
                                    override fun onFailure(call: Call, e: IOException) {
                                        coroutineScope.launch {
                                            withContext(Dispatchers.Main) {
                                                Toast.makeText(context, "연결 실패: 서버 오류", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }

                                    override fun onResponse(call: Call, response: Response) {
                                        coroutineScope.launch {
                                            withContext(Dispatchers.Main) {
                                                if (response.isSuccessful) {
                                                    Toast.makeText(context, "보호자와 연결 완료!", Toast.LENGTH_SHORT).show()
                                                    navController.navigate("sentence")
                                                } else {
                                                    val errorBody = response.body?.string()
                                                    val message = JSONObject(errorBody ?: "{}").optString("error", "연결 실패")
                                                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                })
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(context, "6자리를 모두 입력해주세요", Toast.LENGTH_SHORT).show()
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("보호자와 연결", color = Color.White, fontSize = 16.sp)
        }
    }
}

