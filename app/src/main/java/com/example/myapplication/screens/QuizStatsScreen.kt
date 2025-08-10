package com.example.myapplication.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

@Composable
fun QuizStatsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val patientId = prefs.getString("patient_id", null)

    var statsList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var recList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    val client = remember { OkHttpClient() }

    LaunchedEffect(patientId) {
        if (patientId.isNullOrBlank()) {
            errorMsg = "환자 ID가 없습니다."
            return@LaunchedEffect
        }
        // 1) Firebase ID 토큰 획득
        val idToken = try {
            Firebase.auth.currentUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            null
        }
        if (idToken.isNullOrBlank()) {
            errorMsg = "인증 토큰이 없습니다."
            return@LaunchedEffect
        }

        // 2) 카테고리별 통계 조회
        launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder()
                            .url("${BuildConfig.BASE_URL}/quiz_stats?patient_id=$patientId")
                            .addHeader("Authorization", "Bearer $idToken")
                            .get()
                            .build()
                    ).execute()
                }
                if (!res.isSuccessful) throw Exception("통계 API 오류: ${res.code}")
                val root = JSONObject(res.body?.string().orEmpty())
                val arr = root.optJSONArray("quizStats")
                    ?: throw Exception("quizStats 배열이 없습니다.")
                statsList = List(arr.length()) { i -> arr.getJSONObject(i) }
                errorMsg = null
            } catch (e: Exception) {
                errorMsg = "통계 로드 실패: ${e.message}"
            }
        }

        // 3) 추천 문제 조회
        launch {
            try {
                val res = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder()
                            .url("${BuildConfig.BASE_URL}/quiz/recommend?patient_id=$patientId")
                            .addHeader("Authorization", "Bearer $idToken")
                            .get()
                            .build()
                    ).execute()
                }
                if (!res.isSuccessful) throw Exception("추천 API 오류: ${res.code}")
                val root = JSONObject(res.body?.string().orEmpty())
                val arr = root.optJSONArray("recommended_questions")
                    ?: throw Exception("recommended_questions 배열이 없습니다.")
                recList = List(arr.length()) { i -> arr.getJSONObject(i) }
                if (errorMsg == null) errorMsg = null
            } catch (e: Exception) {
                errorMsg = "추천 로드 실패: ${e.message}"
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        if (errorMsg != null) {
            Text("오류: $errorMsg", color = MaterialTheme.colorScheme.error)
            return@Column
        }

        Text("퀴즈 통계", fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(statsList) { stat ->
                val category      = stat.optString("category", "-")
                val totalAttempts = stat.optInt("total_attempts", 0)
                val correctCount  = stat.optInt("correct_count", 0)
                val avgTime       = stat.optDouble("avg_solve_time", 0.0)
                val accuracy = if (totalAttempts > 0) {
                    "${(correctCount * 100 / totalAttempts)}%"
                } else "-"
                Text(
                    "$category: 시도 ${totalAttempts}회, 정답 ${correctCount}회, " +
                            "정답률 $accuracy, 평균풀이시간 ${"%.1f".format(avgTime)}초"
                )
            }
            item {
                Spacer(Modifier.height(16.dp))
                Text("추천 문제", fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
            }
            items(recList) { rec ->
                val category   = rec.optString("category", "알수없음")
                val question   = rec.optString("question_text", "제목 없음")
                Text("- [$category] $question")
            }
        }
    }
}





