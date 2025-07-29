// QuizStatsScreen.kt
package com.example.myapplication.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.myapplication.viewmodel.QuizStatsViewModel
import com.example.myapplication.viewmodel.QuizViewModel
import org.json.JSONObject

@Composable
fun QuizStatsScreen(
    quizStatsViewModel: QuizStatsViewModel = viewModel(),
    quizViewModel: QuizViewModel = viewModel()
) {
    val patientId by quizStatsViewModel.patientId.collectAsState()

    var statsJson by remember { mutableStateOf<JSONObject?>(null) }
    var recommendJson by remember { mutableStateOf<JSONObject?>(null) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(patientId) {
        patientId?.let {
            quizViewModel.fetchStats(it) { json ->
                if (json?.has("error") == true) {
                    errorMsg = json.getString("error")
                } else {
                    statsJson = json
                    errorMsg = null
                }
            }

            quizViewModel.fetchRecommend(it) { json ->
                if (json?.has("error") == true) {
                    errorMsg = json.getString("error")
                } else {
                    recommendJson = json
                    if (errorMsg == null) errorMsg = null
                }
            }
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("환자 ID: ${patientId ?: "-"}")
        Spacer(Modifier.height(8.dp))

        if (errorMsg != null) {
            Text("오류: $errorMsg")
        } else {
            statsJson?.let { stats ->
                Text("총 시도: ${stats.optInt("total_attempted")}")
                Text("정답 수: ${stats.optInt("correct_count")}")
                Text("정답률: ${stats.optString("accuracy", "-%")}%")
                Text("문장퀴즈 정답률: ${stats.optString("sentence_choice_accuracy", "-")}%")
                Text("사진퀴즈 정답률: ${stats.optString("photo_quiz_accuracy", "-")}%")
                Text("최근 푼 시각: ${stats.optString("last_answered_at", "-")}")
            }

            Spacer(Modifier.height(16.dp))

            recommendJson?.let { rec ->
                val arr = rec.optJSONArray("recommended_questions")
                if (arr != null && arr.length() > 0) {
                    Text("추천 문제:")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        val title = obj.optString("topic", obj.optString("question", "문제 없음"))
                        Text("- $title")
                    }
                } else {
                    Text("추천된 문제가 없습니다.")
                }
            }
        }
    }
}





