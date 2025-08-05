// app/src/main/java/com/example/myapplication/data/DatasetRepository.kt
package com.example.myapplication.data

import com.example.myapplication.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray

class DatasetRepository {
    companion object {
        private const val TAG = "DatasetRepository"
    }

    private val client = OkHttpClient()

    /**
     * patientId 에 해당하는 추천 퀴즈 목록을 가져옵니다.
     * - GET /quizzes/recommend?patient_id=...
     * - 토큰 만료 시 한 번만 강제 갱신 후 재시도
     */
    suspend fun fetchAll(patientId: String): List<DatasetItem> = withContext(Dispatchers.IO) {
        // 1) 현재 사용자 토큰 (캐시) 획득
        val user = Firebase.auth.currentUser
            ?: throw Exception("인증된 사용자가 없습니다.")
        var token = user.getIdToken(false).await().token
            ?: throw Exception("토큰을 가져올 수 없습니다.")

        // 2) 요청 빌더
        val base = BuildConfig.BASE_URL.trimEnd('/')
        fun buildRequest(tok: String): Request {
            val url = "$base/quizzes/recommend?patient_id=$patientId"
            return Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $tok")
                .get()
                .build()
        }

        // 3) 최초 요청
        var resp = client.newCall(buildRequest(token)).execute()

        // 4) 401 Unauthorized → 한 번만 토큰 강제 갱신 후 재요청
        if (resp.code == 401) {
            resp.close()
            token = user.getIdToken(true).await().token
                ?: throw Exception("토큰 갱신에 실패했습니다.")
            resp = client.newCall(buildRequest(token)).execute()
        }

        if (!resp.isSuccessful) {
            throw Exception("API 오류: HTTP ${resp.code}")
        }

        // 5) 응답 바디가 바로 JSON 배열이므로 JSONArray 로 파싱
        val body = resp.body?.string().orEmpty()
        resp.close()
        val arr = JSONArray(body)

        // 6) JSONArray → List<DatasetItem> 변환
        val list = mutableListOf<DatasetItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list += DatasetItem(
                id           = obj.getInt("quiz_id"),           // recommend API 에서는 quiz_id 로 제공
                patientId    = obj.getString("patient_id"),
                memoryId     = obj.getInt("memory_id"),
                questionText = obj.getString("question_text"),
                option1      = obj.getString("option_1"),
                option2      = obj.getString("option_2"),
                option3      = obj.getString("option_3"),
                option4      = obj.getString("option_4"),
                answerIndex  = obj.getInt("answer_index"),
                ttsAudioUrl  = obj.optString("tts_audio_url", ""),
                category     = obj.optString("category", ""),
                createdAt    = obj.optString("created_at", "")
            )
        }
        return@withContext list
    }
}

