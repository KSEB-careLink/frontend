// app/src/main/java/com/example/myapplication/data/DatasetRepository.kt
package com.example.myapplication.data

import android.util.Log
import com.example.myapplication.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

class NoPhotoException(message: String) : Exception(message)

class DatasetRepository {

    companion object {
        private const val TAG_FETCH = "DatasetRepository"
        private const val TAG_POST  = "QuizPOST"
        private const val TAG_FALL  = "QuizFallback"
    }

    val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(120, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .protocols(listOf(Protocol.HTTP_1_1)) // ngrok 헤더 스톨 회피
        .build()

    suspend fun fetchAll(
        patientId: String,
        photoId: String? = null,
        imageUrl: String? = null,
        imagePath: String? = null,
        description: String? = null,
        category: String? = null
    ): List<DatasetItem> = withContext(Dispatchers.IO) {
        val user = Firebase.auth.currentUser ?: throw Exception("인증된 사용자가 없습니다.")
        val token = user.getIdToken(false).await().token ?: throw Exception("ID 토큰을 가져오지 못했습니다.")
        val base = BuildConfig.BASE_URL.trimEnd('/')
        val pidEnc = URLEncoder.encode(patientId, Charsets.UTF_8.name())

        fun sanitizeDesc(src: String?): String? =
            src?.replace(Regex("\\s+"), " ")?.trim()?.takeIf { it.isNotBlank() }?.take(400)

        val cleanDesc = sanitizeDesc(description)
        val categoryClean = category?.trim()?.takeIf { it.isNotBlank() }

        Log.d(
            TAG_FETCH,
            "fetchAll start | pid=$patientId, photoId=$photoId, imageUrl=${imageUrl?.take(60) ?: "null"}…, imagePath=$imagePath, desc=${cleanDesc ?: "null"}, category=${categoryClean ?: "null"}"
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

                // ★ 이미지 URL 다키 지원
                val img = o.optString(
                    "image_url",
                    o.optString(
                        "imageUrl",
                        o.optString(
                            "mediaUrl",
                            o.optString(
                                "photo_url",
                                o.optString("photoUrl", "")
                            )
                        )
                    )
                ).ifBlank { null }

                out += DatasetItem(
                    id = id,
                    questionText = questionText,
                    options = options,
                    ttsAudioUrl = tts,
                    imageUrl = img // ★ 추가
                )
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

        // JSON → Form (Map 기반)
        fun postRecommend(params: Map<String, String>): Pair<List<DatasetItem>, String> {
            val url = "$base/quizzes/recommend"
            val form = FormBody.Builder(Charsets.UTF_8).apply {
                params.forEach { (k, v) -> add(k, v) }
            }.build()

            val logJson = JSONObject().apply { params.forEach { (k, v) -> put(k, v) } }.toString()
            Log.d(TAG_POST, "POST $url | body=$logJson")

            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $token")
                .post(form)
                .build()

            client.newCall(req).execute().use { resp ->
                val txt = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val elapsed = resp.receivedResponseAtMillis - resp.sentRequestAtMillis
                    val snippet = httpSnippet(txt)
                    Log.d(TAG_POST, "RESP ${resp.code} (${elapsed}ms) | snippet=$snippet")

                    val (msg, detail) = runCatching {
                        val o = JSONObject(txt)
                        Pair(
                            o.optString("error").ifBlank { o.optString("message") },
                            o.optString("detail")
                        )
                    }.getOrDefault("" to "")

                    if (resp.code == 422) throw NoPhotoException("HTTP_422: ${if (msg.isNotBlank()) msg else snippet}")
                    val looksLike422 = listOf(msg, detail, snippet)
                        .any { it.contains("422", true) || it.contains("status code 422", true) }
                    if (looksLike422) throw NoPhotoException("HTTP_422_PROXY: ${if (msg.isNotBlank()) msg else detail.ifBlank { snippet }}")

                    if (msg.isNotBlank()) throw NoPhotoException(msg)
                    throw Exception("퀴즈 추천(POST) 실패: HTTP ${resp.code} $snippet")
                }

                Log.d(TAG_POST, "RESP 200 (${resp.receivedResponseAtMillis - resp.sentRequestAtMillis}ms)")
                return parseQuizzes(txt) to txt
            }
        }

        // ─────────────────────────────────────────────────────────────────
        // 1) 1차 본문: photo_id > image_url > image_path > description (+ category 포함)
        // ─────────────────────────────────────────────────────────────────
        val primaryParams = mutableMapOf<String, String>().apply {
            put("patient_id", patientId)
            categoryClean?.let { put("category", it) }

            when {
                !photoId.isNullOrBlank() -> {
                    put("photo_id", (photoId.toLongOrNull() ?: photoId).toString())
                    cleanDesc?.let {
                        put("description", it)
                        put("photo_description", it) // 서버 별칭 대응
                    }
                }
                !imageUrl.isNullOrBlank() -> {
                    put("image_url", imageUrl)
                    cleanDesc?.let {
                        put("description", it)       // image_url엔 desc 동반
                        put("photo_description", it)
                    }
                }
                !imagePath.isNullOrBlank() -> {
                    put("image_path", imagePath)
                    cleanDesc?.let { put("description", it) }
                }
                else -> {
                    cleanDesc?.let { put("description", it) }
                }
            }
        }

        fun hasAnyParams(p: Map<String, String>) =
            p.containsKey("photo_id") || p.containsKey("image_url") || p.containsKey("image_path")
                    || p.containsKey("description") || p.containsKey("photo_description")

        if (!hasAnyParams(primaryParams)) throw NoPhotoException("이미지 지정 또는 설명이 필요합니다.")

        // ─────────────────────────────────────────────────────────────────
        // 2) POST 시도 → 실패 시 보강 폴백
        // ─────────────────────────────────────────────────────────────────
        try {
            val (list0, _) = postRecommend(primaryParams)
            if (list0.isNotEmpty()) return@withContext list0

            // 200인데 빈 배열 → 저장만 되고 비동기 생성 케이스 가능: GET 회수
            val fromGet = getExistingOne()
            if (fromGet.isNotEmpty()) return@withContext fromGet

            return@withContext emptyList()
        } catch (e: NoPhotoException) {
            val emsg = e.message.orEmpty()

            // 2-a) 422 계열(직접/프록시) → desc-only → image_url(+desc) → GET
            val is422 = emsg.startsWith("HTTP_422") || emsg.contains("422")
            if (is422) {
                Log.w(TAG_FALL, "Primary POST returned 422-like error, fallback start")

                // desc-only
                if (cleanDesc != null) {
                    val fb = mutableMapOf<String, String>().apply {
                        put("patient_id", patientId)
                        categoryClean?.let { put("category", it) }
                        put("description", cleanDesc)
                        put("photo_description", cleanDesc)
                    }
                    runCatching { postRecommend(fb) }.onSuccess { (l, _) ->
                        if (l.isNotEmpty()) return@withContext l
                    }
                }

                // image_url(+desc)
                if (!imageUrl.isNullOrBlank()) {
                    val fb2 = mutableMapOf<String, String>().apply {
                        put("patient_id", patientId)
                        categoryClean?.let { put("category", it) }
                        put("image_url", imageUrl)
                        cleanDesc?.let {
                            put("description", it)
                            put("photo_description", it)
                        }
                    }
                    runCatching { postRecommend(fb2) }.onSuccess { (l2, _) ->
                        if (l2.isNotEmpty()) return@withContext l2
                    }
                }

                val fromGet = getExistingOne()
                if (fromGet.isNotEmpty()) return@withContext fromGet
                // 계속 다음 분기
            }

            // 2-b) voice_id 미등록 → 기존 저장 퀴즈로 폴백
            if (hasVoiceIdError(emsg)) {
                Log.w(TAG_FALL, "Primary POST failed (voice_id): $emsg")
                val fromGet = getExistingOne()
                if (fromGet.isNotEmpty()) return@withContext fromGet
                throw NoPhotoException("보호자 음성이 등록되어 있지 않습니다. 보호자 앱에서 음성(voice_id)을 등록한 뒤 다시 시도해주세요.")
            }

            // 2-c) photo_id 무효 추정 → 최신 photo_id로 재시도
            if (!photoId.isNullOrBlank() && shouldFallbackMissingPhotoId(emsg)) {
                Log.w(TAG_FALL, "Primary POST failed (photo_id invalid): $emsg")
                val latest = fetchLatestPhotoId()
                if (latest != null) {
                    val fb3 = mutableMapOf<String, String>().apply {
                        put("patient_id", patientId)
                        categoryClean?.let { put("category", it) }
                        put("photo_id", latest.toString())
                        cleanDesc?.let {
                            put("description", it)
                            put("photo_description", it)
                        }
                    }
                    val (list2, _) = postRecommend(fb3)
                    if (list2.isNotEmpty()) return@withContext list2
                    val fromGet2 = getExistingOne()
                    if (fromGet2.isNotEmpty()) return@withContext fromGet2
                }
            }

            // 2-d) “설명과 일치하는 사진 없음” → desc-only / image_url(+desc)
            val s = emsg.lowercase()
            val descMatchFail = s.contains("설명") || s.contains("description")
            val notFoundish  = s.contains("없") || s.contains("not found") || s.contains("존재")
            if (descMatchFail && notFoundish) {
                if (cleanDesc != null) {
                    Log.d(TAG_FALL, "Fallback: description only (rule match)")
                    val fbDescOnly = mutableMapOf<String, String>().apply {
                        put("patient_id", patientId)
                        categoryClean?.let { put("category", it) }
                        put("description", cleanDesc)
                        put("photo_description", cleanDesc)
                    }
                    val (l3, _) = postRecommend(fbDescOnly)
                    if (l3.isNotEmpty()) return@withContext l3
                }
                if (!imageUrl.isNullOrBlank()) {
                    Log.d(TAG_FALL, "Fallback: image_url only (rule match)")
                    val fbUrlOnly = mutableMapOf<String, String>().apply {
                        put("patient_id", patientId)
                        categoryClean?.let { put("category", it) }
                        put("image_url", imageUrl)
                        cleanDesc?.let {
                            put("description", it)
                            put("photo_description", it)
                        }
                    }
                    val (l4, _) = postRecommend(fbUrlOnly)
                    if (l4.isNotEmpty()) return@withContext l4
                }
                val fromGet3 = getExistingOne()
                if (fromGet3.isNotEmpty()) return@withContext fromGet3
            }

            // 그 외에는 원본 메시지 그대로 전달
            throw e
        }
    }
}


















