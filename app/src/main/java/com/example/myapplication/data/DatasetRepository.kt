package com.example.myapplication.data

import android.util.Log
import com.example.myapplication.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

class DatasetRepository {
    companion object {
        private const val TAG = "DatasetRepository"
    }

    private val client = OkHttpClient()
    private val json   = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class QuizzesResponse(
        val quizzes: List<DatasetItem>
    )

    /**
     * MySQL 기반 REST API 로부터 patientId 에 해당하는
     * 전체 퀴즈 목록을 가져옵니다.
     */
    suspend fun fetchAll(patientId: String): List<DatasetItem> = withContext(Dispatchers.IO) {
        // 1) Firebase ID 토큰 획득
        val idToken = Firebase.auth.currentUser
            ?.getIdToken(true)?.await()?.token
            ?: throw Exception("인증 토큰을 가져올 수 없습니다.")

        // 2) API 요청
        val url = "${BuildConfig.BASE_URL}/quizzes?patient_id=$patientId"
        Log.d(TAG, "fetchAll() 호출 — URL: $url")
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $idToken")
            .get()
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            throw Exception("API 오류: HTTP ${response.code}")
        }

        // 3) JSON 파싱
        val body = response.body?.string().orEmpty()
        val wrapper = json.decodeFromString<QuizzesResponse>(body)
        Log.d(TAG, "파싱된 퀴즈 개수: ${wrapper.quizzes.size}")

        return@withContext wrapper.quizzes
    }
}


