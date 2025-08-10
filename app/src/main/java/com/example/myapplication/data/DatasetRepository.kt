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
import org.json.JSONException
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class NoPhotoException(message: String) : Exception(message)

class DatasetRepository {

    companion object {
        private const val TAG = "DatasetRepository"
        private const val REQ_TAG = "QuizPOST"
        private const val GET_TAG = "QuizGET"
        private const val PHOTO_TAG = "PhotosAPI"
        private const val FB_TAG = "QuizFallback"
        private const val PARSE_TAG = "QuizParse"
        private const val NGROK_TAG = "Ngrok"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // ────────────── helpers (클래스 메서드) ──────────────
    private fun shorten(s: String?, max: Int = 80): String? =
        s?.let { if (it.length <= max) it else it.take(max) + "…" }

    private fun previewDesc(s: String?, max: Int = 60): String =
        s?.lines()?.joinToString(" / ")?.let { if (it.length <= max) it else it.take(max) + "…" } ?: "null"

    private fun isNgrokUpstreamError(html: String): Boolean {
        val h = html.lowercase()
        return h.contains("ngrok") && (h.contains("err_ngrok_8012") || h.contains("upstream web service"))
    }

    private fun shouldFallbackMissingPhotoId(msg: String?): Boolean {
        if (msg.isNullOrBlank()) return false
        val s = msg.lowercase()
        val hasPhotoKey = listOf("photo_id","photoid","사진").any { s.contains(it) }
        val missingWords = listOf("없", "not found", "존재", "invalid").any { s.contains(it) }
        return hasPhotoKey && missingWords
    }

    private fun isDescMatchFail(msg: String?): Boolean {
        if (msg.isNullOrBlank()) return false
        val s = msg.lowercase()
        val hasDesc = s.contains("설명") || s.contains("description")
        val notFoundish  = s.contains("없") || s.contains("not found") || s.contains("존재")
        return hasDesc && notFoundish
    }

    private fun parseQuizzes(txt: String): List<DatasetItem> {
        try {
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
            if (out.isEmpty()) Log.w(PARSE_TAG, "parseQuizzes: empty array. raw=${shorten(txt, 300)}")
            return out
        } catch (je: JSONException) {
            Log.e(PARSE_TAG, "JSON parse error: ${je.message} | raw=${shorten(txt, 300)}")
            throw je
        }
    }

    private fun postRecommend(base: String, token: String, body: JSONObject): Pair<List<DatasetItem>, String> {
        val url = "$base/quizzes/recommend"
        val bodyStr = body.toString()
        Log.d(REQ_TAG, "POST $url | body=${shorten(bodyStr, 500)}")
        val start = System.nanoTime()
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .post(bodyStr.toRequestBody("application/json".toMediaType()))
            .build()
        client.newCall(req).execute().use { resp ->
            val tMs = (System.nanoTime() - start) / 1_000_000
            val txt = resp.body?.string().orEmpty()
            Log.d(REQ_TAG, "RESP ${resp.code} (${tMs}ms) | snippet=${shorten(txt, 500)}")

            if (!resp.isSuccessful) {
                if (txt.startsWith("<!DOCTYPE html", ignoreCase = true) && isNgrokUpstreamError(txt)) {
                    val msg = "서버 터널 문제(NGROK): upstream 연결 실패. ngrok 대상 포트를 실제 서버 포트로 설정 필요."
                    Log.e(NGROK_TAG, msg)
                    throw Exception(msg)
                }
                val msg = runCatching {
                    val o = JSONObject(txt)
                    o.optString("error").ifBlank { o.optString("message") }
                }.getOrDefault("")
                if (msg.isNotBlank()) {
                    Log.e(REQ_TAG, "Server error JSON: $msg")
                    throw NoPhotoException(msg)
                }
                throw Exception("퀴즈 추천(POST) 실패: HTTP ${resp.code} ${shorten(txt, 300)}")
            }
            return parseQuizzes(txt) to txt
        }
    }

    private fun getExistingOne(base: String, token: String, pidEnc: String): List<DatasetItem> {
        val url = "$base/quizzes/recommend?patient_id=$pidEnc"
        Log.d(GET_TAG, "GET $url")
        val start = System.nanoTime()
        val req = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            val tMs = (System.nanoTime() - start) / 1_000_000
            val txt = resp.body?.string().orEmpty()
            Log.d(GET_TAG, "RESP ${resp.code} (${tMs}ms) | snippet=${shorten(txt, 400)}")
            if (!resp.isSuccessful || txt.isBlank()) return emptyList()
            return parseQuizzes(txt)
        }
    }

    private fun fetchLatestPhotoId(base: String, token: String, pidEnc: String): Long? {
        val candidates = listOf(
            "$base/photos/patient/$pidEnc/latest",
            "$base/photos/patient/$pidEnc?limit=1&order=desc",
            "$base/photos?patient_id=$pidEnc&limit=1&order=desc"
        )
        for (url in candidates) {
            Log.d(PHOTO_TAG, "probe $url")
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                val txt = resp.body?.string().orEmpty()
                Log.d(PHOTO_TAG, "resp ${resp.code} | snippet=${shorten(txt, 300)}")
                if (!resp.isSuccessful || txt.isBlank()) return@use
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

    // ─────────────────────────────────────────────────────────────────────────

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

        Log.d(TAG, "fetchAll start | pid=$patientId, photoId=$photoId, imageUrl=${shorten(imageUrl)}, imagePath=${shorten(imagePath)}, desc=${previewDesc(description)}")

        // 1) 단일 소스 우선순위: image_path > image_url > photo_id > description
        val primaryBody = JSONObject().apply {
            put("patient_id", patientId)
            when {
                !imagePath.isNullOrBlank() -> {
                    put("image_path", imagePath) // 단독
                }
                !imageUrl.isNullOrBlank() -> {
                    put("image_url", imageUrl)   // 단독
                }
                !photoId.isNullOrBlank() -> {
                    val n = photoId.toLongOrNull()
                    if (n != null) put("photo_id", n) else put("photo_id", photoId) // 단독
                }
                !description.isNullOrBlank() -> {
                    put("description", description) // 최후단
                }
            }
        }

        fun hasAny(b: JSONObject) =
            b.has("photo_id") || b.has("image_url") || b.has("image_path") || b.has("description")

        if (!hasAny(primaryBody)) throw NoPhotoException("이미지 지정 또는 설명이 필요합니다.")

        // 2) POST 시도
        try {
            val (list, postTxt) = postRecommend(base, token, primaryBody)
            if (list.isNotEmpty()) return@withContext list
            val fromGet = getExistingOne(base, token, pidEnc)
            if (fromGet.isNotEmpty()) return@withContext fromGet
            Log.w(TAG, "POST ok but no quizzes, GET empty as well. Returning empty list.")
            return@withContext emptyList()
        } catch (e: NoPhotoException) {
            Log.w(FB_TAG, "Primary POST failed: ${e.message}")

            // 3-a) photo_id 무효 → 최신 photo_id 로 재시도
            if (primaryBody.has("photo_id") && shouldFallbackMissingPhotoId(e.message)) {
                Log.d(FB_TAG, "Fallback: latest photo_id")
                val latest = fetchLatestPhotoId(base, token, pidEnc)
                if (latest != null) {
                    val fb = JSONObject().apply {
                        put("patient_id", patientId)
                        put("photo_id", latest) // 단독
                    }
                    val (list2, _) = postRecommend(base, token, fb)
                    if (list2.isNotEmpty()) return@withContext list2
                    val fromGet2 = getExistingOne(base, token, pidEnc)
                    if (fromGet2.isNotEmpty()) return@withContext fromGet2
                } else {
                    Log.w(FB_TAG, "No latest photo_id found for pid=$patientId")
                }
            }

            // 3-b) 설명 매칭 실패 → image_url 단독 재시도
            if (isDescMatchFail(e.message) && !imageUrl.isNullOrBlank()) {
                Log.d(FB_TAG, "Fallback: image_url only (desc mismatch)")
                val fbUrlOnly = JSONObject().apply {
                    put("patient_id", patientId)
                    put("image_url", imageUrl) // 단독
                }
                val (listUrlOnly, _) = postRecommend(base, token, fbUrlOnly)
                if (listUrlOnly.isNotEmpty()) return@withContext listUrlOnly
                val fromGetUrlOnly = getExistingOne(base, token, pidEnc)
                if (fromGetUrlOnly.isNotEmpty()) return@withContext fromGetUrlOnly
            }

            // 3-c) 최후: description 단독 재시도
            if (!description.isNullOrBlank()) {
                Log.d(FB_TAG, "Fallback: description only")
                val fbDescOnly = JSONObject().apply {
                    put("patient_id", patientId)
                    put("description", description) // 단독
                }
                val (list3, _) = postRecommend(base, token, fbDescOnly)
                if (list3.isNotEmpty()) return@withContext list3
                val fromGet3 = getExistingOne(base, token, pidEnc)
                if (fromGet3.isNotEmpty()) return@withContext fromGet3
            }

            throw e
        } catch (ex: Exception) {
            // ngrok/네트워크/기타 예외
            Log.e(TAG, "fetchAll fatal: ${ex.message}", ex)
            throw ex
        }
    }
}













