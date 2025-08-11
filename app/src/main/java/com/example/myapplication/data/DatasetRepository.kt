// app/src/main/java/com/example/myapplication/data/DatasetRepository.kt
package com.example.myapplication.data

import android.util.Log
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
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import okhttp3.Protocol

class NoPhotoException(message: String) : Exception(message)

class DatasetRepository {

    companion object {
        private const val TAG_FETCH = "DatasetRepository"
        private const val TAG_POST  = "QuizPOST"
        private const val TAG_FALL  = "QuizFallback"
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)   // TCP 연결
        .writeTimeout(120, TimeUnit.SECONDS)    // 이미지/본문 업로드 여유
        .readTimeout(90, TimeUnit.SECONDS)      // 서버 처리 대기 여유 ↑
        .callTimeout(120, TimeUnit.SECONDS)     // 전체 콜 상한
        .retryOnConnectionFailure(true)
        .protocols(listOf(Protocol.HTTP_1_1))   // ★ HTTP/2 → 1.1 강제 (ngrok 헤더 스톨 회피)
        .build()

    suspend fun fetchAll(
        patientId: String,
        photoId: String? = null,
        imageUrl: String? = null,
        imagePath: String? = null,
        description: String? = null
    ): List<DatasetItem> = withContext(Dispatchers.IO) {
        val user = Firebase.auth.currentUser ?: throw Exception("인증된 사용자가 없습니다.")
        val token = user.getIdToken(false).await().token ?: throw Exception("ID 토큰을 가져오지 못했습니다.")
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val pidEnc = URLEncoder.encode(patientId, Charsets.UTF_8.name())

        Log.d(
            TAG_FETCH,
            "fetchAll start | pid=$patientId, photoId=$photoId, imageUrl=${imageUrl?.take(60) ?: "null"}…, imagePath=$imagePath, desc=${description?.replace('\n','/')}"
        )

        fun parseQuizzes(txt: String): List<DatasetItem> {
            val arr = when {
                txt.trim().startsWith("[") -> JSONArray(txt)
                else -> {
                    val obj = runCatching { JSONObject(txt) }.getOrNull() ?: JSONObject()
                    obj.optJSONArray("quizzes")
                        ?: obj.optJSONArray("items")
                        ?: obj.optJSONArray("data")
                        ?: JSONArray()
                }
            }
            val out = mutableListOf<DatasetItem>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val id: Int = when (val raw = when {
                    o.has("quiz_id") -> o.opt("quiz_id")
                    o.has("id") -> o.opt("id")
                    o.has("quizId") -> o.opt("quizId")
                    else -> -1
                }) {
                    is Number -> raw.toInt()
                    is String -> raw.toIntOrNull() ?: -1
                    else -> -1
                }
                val questionText = o.optString("question_text", o.optString("question", ""))
                val tts = o.optString("tts_audio_url", o.optString("quiz_tts_url", ""))
                val optionsJson = o.optJSONArray("options") ?: JSONArray()
                val options = List(optionsJson.length()) { j -> optionsJson.getString(j) }
                out += DatasetItem(id = id, questionText = questionText, options = options, ttsAudioUrl = tts)
            }
            return out
        }

        fun httpSnippet(s: String, max: Int = 160): String {
            val t = s.replace(Regex("\\s+"), " ").trim()
            return if (t.length <= max) t else t.substring(0, max) + "…"
        }

        fun hasVoiceIdError(msg: String?): Boolean {
            if (msg.isNullOrBlank()) return false
            val s = msg.lowercase()
            // 서버 표준 문구 포함 (+ 영문 케이스도 포괄)
            return s.contains("voice_id") || s.contains("보호자 음성")
        }

        fun shouldFallbackMissingPhotoId(msg: String?): Boolean {
            if (msg.isNullOrBlank()) return false
            val s = msg.lowercase()
            val hasPhotoKey = listOf("photo_id","photoid","사진").any { s.contains(it) }
            val missingWords = listOf("없", "not found", "존재", "invalid").any { s.contains(it) }
            return hasPhotoKey && missingWords
        }

        fun getExistingOne(): List<DatasetItem> {
            val url = "$base/quizzes/recommend?patient_id=$pidEnc"
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()

            client.newCall(req).execute().use { resp ->
                val txt = resp.body?.string().orEmpty()
                if (!resp.isSuccessful || txt.isBlank()) {
                    Log.d(TAG_FETCH, "GET existing resp=${resp.code} body=${httpSnippet(txt)}")
                    return emptyList()
                }
                Log.d(TAG_FETCH, "GET existing OK | body=${httpSnippet(txt)}")
                return parseQuizzes(txt)
            }
        }

        fun fetchLatestPhotoId(): Long? {
            val candidates = listOf(
                "$base/photos/patient/$pidEnc/latest",
                "$base/photos/patient/$pidEnc?limit=1&order=desc",
                "$base/photos?patient_id=$pidEnc&limit=1&order=desc"
            )
            for (url in candidates) {
                val req = Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $token")
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) return@use
                    val txt = resp.body?.string().orEmpty()
                    if (txt.isBlank()) return@use

                    runCatching {
                        val o = JSONObject(txt)
                        val id = o.optLong("photo_id", o.optLong("id", -1L))
                        if (id > 0) return id
                    }
                    runCatching {
                        val arr = JSONArray(txt)
                        if (arr.length() > 0) {
                            val first = arr.getJSONObject(0)
                            val id = first.optLong("photo_id", first.optLong("id", -1L))
                            if (id > 0) return id
                        }
                    }
                }
            }
            return null
        }

        fun postRecommend(body: JSONObject): Pair<List<DatasetItem>, String> {
            val url = "$base/quizzes/recommend"
            Log.d(TAG_POST, "POST $url | body=${body}")
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .post(body.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(req).execute().use { resp ->
                val txt = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val snippet = httpSnippet(txt)
                    Log.d(TAG_POST, "RESP ${resp.code} (${resp.receivedResponseAtMillis - resp.sentRequestAtMillis}ms) | snippet=$snippet")

                    val msg = runCatching {
                        val o = JSONObject(txt)
                        o.optString("error").ifBlank { o.optString("message") }
                    }.getOrDefault("")

                    if (msg.isNotBlank()) {
                        Log.e(TAG_POST, "Server error JSON: $msg")
                        throw NoPhotoException(msg)
                    }
                    throw Exception("퀴즈 추천(POST) 실패: HTTP ${resp.code} $snippet")
                }

                Log.d(TAG_POST, "RESP 200 (${resp.receivedResponseAtMillis - resp.sentRequestAtMillis}ms)")
                return parseQuizzes(txt) to txt
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // 1) 1차 본문: photo_id > image_url > image_path > description
        // ─────────────────────────────────────────────────────────────────
        val primaryBody = JSONObject().apply {
            put("patient_id", patientId)
            when {
                !photoId.isNullOrBlank() -> {
                    val n = photoId.toLongOrNull()
                    if (n != null) put("photo_id", n) else put("photo_id", photoId)
                    if (!description.isNullOrBlank()) put("description", description)
                }
                !imageUrl.isNullOrBlank() -> {
                    // NOTE: .firebasestorage.app 그대로 사용 (치환 금지)
                    put("image_url", imageUrl)
                }
                !imagePath.isNullOrBlank() -> {
                    put("image_path", imagePath)
                    if (!description.isNullOrBlank()) put("description", description)
                }
                !description.isNullOrBlank() -> {
                    put("description", description)
                }
            }
        }

        fun hasAny(b: JSONObject) =
            b.has("photo_id") || b.has("image_url") || b.has("image_path") || b.has("description")

        if (!hasAny(primaryBody)) throw NoPhotoException("이미지 지정 또는 설명이 필요합니다.")

        // ─────────────────────────────────────────────────────────────────
        // 2) POST 시도 → 실패 시 보강 폴백
        // ─────────────────────────────────────────────────────────────────
        try {
            val (list, _) = postRecommend(primaryBody)
            if (list.isNotEmpty()) return@withContext list

            // POST 성공 + 빈배열 → DB 저장만 된 케이스일 수 있으니 GET으로 회수
            val fromGet = getExistingOne()
            if (fromGet.isNotEmpty()) return@withContext fromGet

            return@withContext emptyList()
        } catch (e: NoPhotoException) {

            // 2-a) voice_id 미등록 → 기존 저장 퀴즈로 폴백
            if (hasVoiceIdError(e.message)) {
                Log.w(TAG_FALL, "Primary POST failed (voice_id): ${e.message}")

                val fromGet = getExistingOne()
                if (fromGet.isNotEmpty()) return@withContext fromGet

                // 그래도 없으면 명확한 안내로 종료
                throw NoPhotoException("보호자 음성이 등록되어 있지 않습니다. 보호자 앱에서 음성(voice_id)을 등록한 뒤 다시 시도해주세요.")
            }

            // 2-b) photo_id 무효 → 최신 photo_id로 재시도
            if (!photoId.isNullOrBlank() && shouldFallbackMissingPhotoId(e.message)) {
                Log.w(TAG_FALL, "Primary POST failed: ${e.message}")
                val latest = fetchLatestPhotoId()
                if (latest != null) {
                    val fb = JSONObject().apply {
                        put("patient_id", patientId)
                        put("photo_id", latest)
                    }
                    val (list2, _) = postRecommend(fb)
                    if (list2.isNotEmpty()) return@withContext list2
                    val fromGet2 = getExistingOne()
                    if (fromGet2.isNotEmpty()) return@withContext fromGet2
                }
            }

            // 2-c) “설명과 일치하는 사진 없음” 등 → description-only / image_url-only 등 추가 폴백
            val msg = e.message?.lowercase().orEmpty()
            val descMatchFail = msg.contains("설명") || msg.contains("description")
            val notFoundish  = msg.contains("없") || msg.contains("not found") || msg.contains("존재")

            if (descMatchFail && notFoundish) {
                // 우선 description-only
                if (!description.isNullOrBlank()) {
                    Log.d(TAG_FALL, "Fallback: description only")
                    val fbDescOnly = JSONObject().apply {
                        put("patient_id", patientId)
                        put("description", description)
                    }
                    val (list3, _) = postRecommend(fbDescOnly)
                    if (list3.isNotEmpty()) return@withContext list3
                }

                // 그다음 image_url-only
                if (!imageUrl.isNullOrBlank()) {
                    Log.d(TAG_FALL, "Fallback: image_url only")
                    val fbUrlOnly = JSONObject().apply {
                        put("patient_id", patientId)
                        put("image_url", imageUrl)
                    }
                    val (list4, _) = postRecommend(fbUrlOnly)
                    if (list4.isNotEmpty()) return@withContext list4
                }

                val fromGet3 = getExistingOne()
                if (fromGet3.isNotEmpty()) return@withContext fromGet3
            }

            // 다른 케이스는 원본 메시지 그대로 전달
            throw e
        }
    }
}














