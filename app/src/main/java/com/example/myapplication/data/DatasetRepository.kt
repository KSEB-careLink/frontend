package com.example.myapplication.data

import android.util.Log
import com.example.myapplication.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class DatasetRepository {
    companion object {
        private const val TAG = "DatasetRepository"
    }
    private val client = OkHttpClient()

    /**
     * patientId 에 해당하는 전체 퀴즈 목록을 가져옵니다.
     * 토큰 만료 시 한 번만 재발급 후 재시도합니다.
     */
    suspend fun fetchAll(patientId: String): List<DatasetItem> = withContext(Dispatchers.IO) {
        // 1) 캐시 우선 토큰 얻기
        val user = Firebase.auth.currentUser
            ?: throw Exception("인증된 사용자가 없습니다.")
        var token = user.getIdToken(false).await().token
            ?: throw Exception("토큰을 가져올 수 없습니다.")

        // 2) 요청 빌더 함수
        val base = BuildConfig.BASE_URL.trimEnd('/')
        fun buildRequest(tok: String): Request {
            val url = "$base/quizzes?patient_id=$patientId"
            return Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $tok")
                .get()
                .build()
        }

        // 3) 최초 호출
        var resp = client.newCall(buildRequest(token)).execute()

        // 4) 401 이면 한 번만 토큰 강제 갱신 후 재호출
        if (resp.code == 401) {
            resp.close()
            token = user.getIdToken(true).await().token
                ?: throw Exception("토큰 갱신에 실패했습니다.")
            resp = client.newCall(buildRequest(token)).execute()
        }

        if (!resp.isSuccessful) {
            throw Exception("API 오류: HTTP ${resp.code}")
        }

        // 5) JSON 파싱 (옵션 4개는 개별 필드로 매핑)
        val body = resp.body?.string().orEmpty()
        val root = JSONObject(body)
        val arr  = root.optJSONArray("quizzes")
            ?: throw Exception("응답에 'quizzes' 필드가 없습니다.")

        val list = mutableListOf<DatasetItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                DatasetItem(
                    id           = obj.getInt("id"),
                    patientId    = obj.getString("patient_id"),
                    memoryId     = obj.getInt("memory_id"),
                    questionText = obj.getString("question_text"),
                    option1      = obj.optString("option_1", ""),
                    option2      = obj.optString("option_2", ""),
                    option3      = obj.optString("option_3", ""),
                    option4      = obj.optString("option_4", ""),
                    answerIndex  = obj.getInt("answer_index"),
                    ttsAudioUrl  = obj.optString("tts_audio_url", ""),
                    category     = obj.getString("category"),
                    createdAt    = obj.getString("created_at")
                )
            )
        }
        resp.close()
        return@withContext list
    }
}

