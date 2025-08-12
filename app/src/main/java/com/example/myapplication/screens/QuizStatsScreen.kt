// app/src/main/java/com/example/myapplication/screens/QuizStatsScreen.kt
package com.example.myapplication.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun QuizStatsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val patientId = prefs.getString("patient_id", null)

    // 통계: 서버 스펙에 맞춰 'categories' 배열을 그대로 들고 있음
    var statsList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    // 상단 요약(전체 정확도/평균 응답시간) 추가
    var totalAccuracy by remember { mutableStateOf<Double?>(null) }
    var totalAvgTime by remember { mutableStateOf<Double?>(null) }

    // 추천 목록 (그대로 JSONObject 리스트 사용)
    var recList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    var errorMsg by remember { mutableStateOf<String?>(null) }
    val client = remember { OkHttpClient() }

    LaunchedEffect(patientId) {
        if (patientId.isNullOrBlank()) {
            errorMsg = "환자 ID가 없습니다."
            return@LaunchedEffect
        }

        // 1) Firebase ID 토큰
        val idToken = try {
            Firebase.auth.currentUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            null
        }
        if (idToken.isNullOrBlank()) {
            errorMsg = "인증 토큰이 없습니다."
            return@LaunchedEffect
        }

        // 2) 카테고리별 통계 조회 (GET /quizStats?patient_id=...)
        launch {
            try {
                val url = "${BuildConfig.BASE_URL}/quizStats?patient_id=$patientId"
                val res = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder()
                            .url(url)
                            .addHeader("Authorization", "Bearer $idToken")
                            .get()
                            .build()
                    ).execute()
                }
                if (!res.isSuccessful) throw Exception("통계 API 오류: ${res.code}")
                val root = JSONObject(res.body?.string().orEmpty())

                // 상단 요약값
                totalAccuracy = if (root.has("total_accuracy")) root.optDouble("total_accuracy") else null
                totalAvgTime = if (root.has("total_avg_time")) root.optDouble("total_avg_time") else null

                // categories 배열 파싱
                val arr: JSONArray = root.optJSONArray("categories")
                    ?: throw Exception("categories 배열이 없습니다.")
                statsList = List(arr.length()) { i -> arr.getJSONObject(i) }

                errorMsg = null
            } catch (e: Exception) {
                errorMsg = "통계 로드 실패: ${e.message}"
            }
        }

        // 3) 추천 문제 조회
        launch {
            try {
                // 먼저 POST /quizzes/recommend 시도 (서버 문서 기준)
                val postUrl = "${BuildConfig.BASE_URL}/quizzes/recommend"
                val jsonBody = """{"patient_id":"$patientId"}"""
                    .toRequestBody("application/json; charset=utf-8".toMediaType())

                val postRes = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder()
                            .url(postUrl)
                            .addHeader("Authorization", "Bearer $idToken")
                            .post(jsonBody)
                            .build()
                    ).execute()
                }

                val recRoot: JSONObject = if (postRes.isSuccessful) {
                    JSONObject(postRes.body?.string().orEmpty())
                } else {
                    // 폴백: GET /quiz/recommend?patient_id=...
                    val getUrl = "${BuildConfig.BASE_URL}/quiz/recommend?patient_id=$patientId"
                    val getRes = withContext(Dispatchers.IO) {
                        client.newCall(
                            Request.Builder()
                                .url(getUrl)
                                .addHeader("Authorization", "Bearer $idToken")
                                .get()
                                .build()
                        ).execute()
                    }
                    if (!getRes.isSuccessful) throw Exception("추천 API 오류: ${postRes.code} / ${getRes.code}")
                    JSONObject(getRes.body?.string().orEmpty())
                }

                // 서버 구현별 키가 다를 수 있어 널 안전 처리
                val arr = recRoot.optJSONArray("recommended_questions")
                    ?: recRoot.optJSONArray("items")
                    ?: recRoot.optJSONArray("data")
                    ?: JSONArray()

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

        // 상단 요약 표시(있을 때만)
        totalAccuracy?.let { acc ->
            Text("전체 정확도: ${"%.1f".format(acc)}%")
        }
        totalAvgTime?.let { t ->
            Text("평균 응답 시간: ${"%.1f".format(t)}초")
        }
        if (totalAccuracy != null || totalAvgTime != null) {
            Spacer(Modifier.height(8.dp))
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // 카테고리별
            items(statsList) { stat ->
                val category = stat.optString("category", "-")
                val total    = stat.optInt("total", 0)
                val correct  = stat.optInt("correct", 0)
                val accPct   = stat.optDouble("accuracy", 0.0)     // 서버가 %로 내려줌
                val avgTime  = stat.optDouble("avg_time", 0.0)     // 초

                Text(
                    "$category: 정답 $correct / $total, " +
                            "정확도 ${"%.1f".format(accPct)}%, 평균응답 ${"%.1f".format(avgTime)}초"
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text("추천 문제", fontSize = 20.sp)
                Spacer(Modifier.height(8.dp))
            }

            // 추천 문제 표시 (서버 키에 맞춰 안전하게)
            items(recList) { rec ->
                val category = rec.optString("category", "알수없음")
                val question = rec.optString("question_text",
                    rec.optString("question", "제목 없음")
                )
                Text("- [$category] $question")
            }
        }
    }
}