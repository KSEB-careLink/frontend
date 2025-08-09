// app/src/main/java/com/example/myapplication/data/DatasetRepository.kt
package com.example.myapplication.data

import com.example.myapplication.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class DatasetRepository {
    companion object {
        private const val TAG = "DatasetRepository"
    }

    private val client = OkHttpClient()

    /**
     * 추천 퀴즈 불러오기
     * - 1순위: POST /quizzes/recommend { patient_id }
     * - 2순위: (서버에서 추가 필수값 요구로 400 발생 시) GET /quizzes/recommend?patient_id=...
     * - 401은 1회 토큰 강제갱신 후 재시도
     */
    suspend fun fetchAll(patientId: String): List<DatasetItem> = withContext(Dispatchers.IO) {
        val user = Firebase.auth.currentUser ?: throw Exception("인증된 사용자가 없습니다.")
        var token = user.getIdToken(false).await().token ?: throw Exception("토큰을 가져올 수 없습니다.")

        val base = BuildConfig.BASE_URL.trimEnd('/')

        fun postRequest(tok: String): Request {
            val url = "$base/quizzes/recommend"
            val body = JSONObject().apply { put("patient_id", patientId) }
                .toString()
                .toRequestBody("application/json".toMediaType())
            return Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $tok")
                .post(body)
                .build()
        }

        fun getRequest(tok: String): Request {
            val url = "$base/quizzes/recommend?patient_id=$patientId"
            return Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $tok")
                .get()
                .build()
        }

        // 1) POST 시도
        var resp = client.newCall(postRequest(token)).execute()

        // 401 → 강제 갱신 후 다시 POST
        if (resp.code == 401) {
            resp.close()
            token = user.getIdToken(true).await().token ?: throw Exception("토큰 갱신에 실패했습니다.")
            resp = client.newCall(postRequest(token)).execute()
        }

        // 400(필수값 누락) or 404/405 → GET 폴백
        if (resp.code == 400 || resp.code == 404 || resp.code == 405) {
            val msg = resp.body?.string().orEmpty()
            resp.close()
            // 서버가 {"error":"필수값 누락"}을 주는 케이스를 GET으로 자동 폴백
            resp = client.newCall(getRequest(token)).execute()
            if (!resp.isSuccessful) {
                val code = resp.code
                val err = resp.body?.string().orEmpty()
                resp.close()
                throw Exception("API 오류: HTTP $code ${if (err.isNotBlank()) " - $err" else ""}")
            }
            val bodyStr = resp.body?.string().orEmpty()
            resp.close()
            return@withContext parseList(bodyStr)
        }

        if (!resp.isSuccessful) {
            val code = resp.code
            val msg = resp.body?.string().orEmpty()
            resp.close()
            throw Exception("API 오류: HTTP $code ${if (msg.isNotBlank()) " - $msg" else ""}")
        }

        val bodyStr = resp.body?.string().orEmpty()
        resp.close()
        return@withContext parseList(bodyStr)
    }

    private fun parseList(bodyStr: String): List<DatasetItem> {
        val arr: JSONArray = runCatching { JSONArray(bodyStr) }.getOrElse {
            val obj = JSONObject(bodyStr)
            when {
                obj.has("quizzes") -> obj.getJSONArray("quizzes")
                obj.has("data") -> obj.getJSONArray("data")
                else -> throw Exception("Unexpected response format")
            }
        }

        val list = ArrayList<DatasetItem>(arr.length())
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)

            val id = when {
                obj.has("quiz_id") -> obj.getInt("quiz_id")
                obj.has("id") -> obj.getInt("id")
                else -> throw Exception("퀴즈 ID 누락")
            }

            val questionText = obj.getString("question_text")

            val options: List<String> =
                if (obj.has("options")) {
                    val o = obj.getJSONArray("options")
                    listOf(o.getString(0), o.getString(1), o.getString(2), o.getString(3))
                } else {
                    listOf(
                        obj.getString("option_1"),
                        obj.getString("option_2"),
                        obj.getString("option_3"),
                        obj.getString("option_4")
                    )
                }

            val tts = obj.optString("tts_audio_url", "")
            val pid = obj.optString("patient_id", null)
            val mid = if (obj.has("memory_id")) obj.optInt("memory_id") else null
            val cat = obj.optString("category", null)
            val createdAt = obj.optString("created_at", null)

            list += DatasetItem(
                id = id,
                questionText = questionText,
                options = options,
                ttsAudioUrl = tts,
                patientId = pid,
                memoryId = mid,
                category = cat,
                createdAt = createdAt
            )
        }
        return list
    }
}




