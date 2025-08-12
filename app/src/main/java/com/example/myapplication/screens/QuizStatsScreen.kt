// app/src/main/java/com/example/myapplication/screens/QuizStatsScreen.kt
package com.example.myapplication.screens

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun QuizStatsScreen() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val patientId = prefs.getString("patient_id", null)

    // --- 카테고리별 누적 통계 ---
    var statsList by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var totalAccuracy by remember { mutableStateOf<Double?>(null) }
    var totalAvgTime by remember { mutableStateOf<Double?>(null) }

    // --- 월간 통계 + 드롭다운 ---
    val seoul = remember { ZoneId.of("Asia/Seoul") }
    val ymFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM") }
    val currentMonth = remember { YearMonth.now(seoul).format(ymFormatter) }
    var month by remember { mutableStateOf(currentMonth) }
    var monthlyObj by remember { mutableStateOf<JSONObject?>(null) }
    var monthlyDaily by remember { mutableStateOf<List<JSONObject>>(emptyList()) }

    // 최근 18개월 옵션
    val monthOptions = remember(currentMonth) {
        val start = YearMonth.parse(currentMonth, ymFormatter)
        (0 until 18).map { i -> start.minusMonths(i.toLong()).format(ymFormatter) }
    }

    var errorMsg by remember { mutableStateOf<String?>(null) }
    val client = remember { OkHttpClient() }

    // 서버 경로 상수 (서버 마운트에 맞춰 필요 시 수정)
    val PATH_STATS = "/quizStats"          // 예) app.use('/quizStats', ...)
    val PATH_MONTHLY_BASE = "/quiz-stats"  // 예) app.use('/quiz-stats', ...)  (카멜케이스면 "/quizStats"로 변경)

    LaunchedEffect(patientId, month) {
        if (patientId.isNullOrBlank()) {
            errorMsg = "환자 ID가 없습니다."
            return@LaunchedEffect
        }

        // Firebase ID 토큰
        val idToken = try { Firebase.auth.currentUser?.getIdToken(true)?.await()?.token } catch (_: Exception) { null }
        if (idToken.isNullOrBlank()) {
            errorMsg = "인증 토큰이 없습니다."
            return@LaunchedEffect
        }

        // 1) 카테고리별 누적 통계
        launch {
            try {
                val url = "${BuildConfig.BASE_URL}$PATH_STATS?patient_id=${enc(patientId)}"
                val res = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder().url(url)
                            .addHeader("Authorization", "Bearer $idToken")
                            .get()
                            .build()
                    ).execute()
                }
                if (!res.isSuccessful) throw Exception("통계 API 오류: ${res.code}")
                val root = JSONObject(res.body?.string().orEmpty())

                totalAccuracy = root.optDoubleOrNull("total_accuracy")
                totalAvgTime  = root.optDoubleOrNull("total_avg_time")

                val arr: JSONArray = root.optJSONArray("categories")
                    ?: throw Exception("categories 배열이 없습니다.")
                statsList = List(arr.length()) { i -> arr.getJSONObject(i) }

                if (errorMsg?.startsWith("통계") == true) errorMsg = null
            } catch (e: Exception) {
                errorMsg = "통계 로드 실패: ${e.message}"
                statsList = emptyList()
                totalAccuracy = null
                totalAvgTime = null
            }
        }

        // 2) 월간 통계
        launch {
            try {
                val url = "${BuildConfig.BASE_URL}$PATH_MONTHLY_BASE/monthly" +
                        "?patient_id=${enc(patientId)}&month=${enc(month)}"
                val res = withContext(Dispatchers.IO) {
                    client.newCall(
                        Request.Builder().url(url)
                            .addHeader("Authorization", "Bearer $idToken")
                            .get()
                            .build()
                    ).execute()
                }
                if (!res.isSuccessful) throw Exception("월간 API 오류: ${res.code}")
                val root = JSONObject(res.body?.string().orEmpty())

                monthlyObj = root
                val dailyArr = root.optJSONArray("daily")
                    ?: root.optJSONArray("by_day")
                    ?: root.optJSONArray("days")
                monthlyDaily = if (dailyArr != null) {
                    List(dailyArr.length()) { i -> dailyArr.getJSONObject(i) }
                } else emptyList()

                if (errorMsg?.startsWith("월간") == true) errorMsg = null
            } catch (e: Exception) {
                errorMsg = "월간 통계 로드 실패: ${e.message}"
                monthlyObj = null
                monthlyDaily = emptyList()
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        errorMsg?.let {
            Text("오류: $it", color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        // ───────── 월간 통계 + 드롭다운 ─────────
        Text("월간 통계", fontSize = 20.sp)
        Spacer(Modifier.height(6.dp))

        MonthDropdown(
            month = month,
            options = monthOptions,
            onSelect = { month = it },
        )

        Spacer(Modifier.height(6.dp))

        monthlyObj?.let { m ->
            val mAcc = m.optDoubleOrNull("accuracy") ?: m.optDoubleOrNull("total_accuracy")
            val mAvg = m.optDoubleOrNull("avg_time")
                ?: m.optDoubleOrNull("average_time")
                ?: m.optDoubleOrNull("total_avg_time")
            val mTotal = m.optIntOrNull("total")
                ?: m.optIntOrNull("total_attempts")
                ?: m.optIntOrNull("total_questions")
            val mCorrect = m.optIntOrNull("correct")
                ?: m.optIntOrNull("correct_count")

            val parts = mutableListOf<String>()
            mAcc?.let { parts.add("정확도 ${"%.1f".format(it)}%") }
            mAvg?.let { parts.add("평균 ${"%.1f".format(it)}초") }
            if (mCorrect != null && mTotal != null) parts.add("정답 $mCorrect / $mTotal")

            if (parts.isEmpty()) Text("월간 데이터가 없습니다.") else Text(parts.joinToString(", "))
        } ?: Text("월간 데이터 없음")

        if (monthlyDaily.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            val preview = monthlyDaily.take(5) // 처음 5일만 미리보기
            preview.forEach { d ->
                val day = d.optString("date", d.optString("day", d.optString("dt", "-")))
                val acc = d.optDoubleOrNull("accuracy")
                val avg = d.optDoubleOrNull("avg_time") ?: d.optDoubleOrNull("average_time")
                val line = buildString {
                    append(" - $day")
                    if (acc != null) append(" · ${"%.1f".format(acc)}%")
                    if (avg != null) append(" · ${"%.1f".format(avg)}초")
                }
                Text(line)
            }
        }

        Spacer(Modifier.height(16.dp))

        // ───────── 카테고리별 누적 통계 ─────────
        Text("카테고리별 누적 통계", fontSize = 20.sp)
        // 요청: 작은 글씨 설명 추가
        Text(
            "풀이 기록이 많아질수록 예상 정답률이 실제 정답률에 가까워집니다.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
        Spacer(Modifier.height(6.dp))

        totalAccuracy?.let { Text("전체 정확도: ${"%.1f".format(it)}%") }
        totalAvgTime?.let { Text("평균 응답 시간: ${"%.1f".format(it)}초") }
        if (totalAccuracy != null || totalAvgTime != null) Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(statsList) { stat ->
                val category = stat.optString("category", "-")
                val total = stat.optInt("total", 0)
                val correct = stat.optInt("correct", 0)
                val accPct = stat.optDouble("accuracy", 0.0).coerceIn(0.0, 100.0)
                val avgTime = stat.optDouble("avg_time", 0.0)

                Column {
                    Text(
                        "$category: 정답 $correct / $total, 정확도 ${"%.1f".format(accPct)}%, 평균응답 ${"%.1f".format(avgTime)}초"
                    )
                    Spacer(Modifier.height(6.dp))
                    // 정확도 프로그레스바
                    Column {
                        Text(
                            "정확도 ${"%.1f".format(accPct)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(4.dp))
                        LinearProgressIndicator(
                            progress = (accPct / 100.0).toFloat(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                        )
                    }
                }
            }
        }
    }
}

/** 월 선택 드롭다운 (Exposed 없이 구현) */
@Composable
private fun MonthDropdown(
    month: String,
    options: List<String>,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.TopStart
    ) {
        OutlinedTextField(
            value = month,
            onValueChange = {},
            readOnly = true,
            label = { Text("월 선택 (YYYY-MM)") },
            trailingIcon = { Text(if (expanded) "▲" else "▼") },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun enc(s: String): String =
    URLEncoder.encode(s, StandardCharsets.UTF_8.toString())

// JSONObject 확장: 안전 파싱
private fun JSONObject.optDoubleOrNull(key: String): Double? =
    if (has(key) && !isNull(key)) {
        val v = optDouble(key, Double.NaN)
        if (v.isNaN()) null else v
    } else null

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null



