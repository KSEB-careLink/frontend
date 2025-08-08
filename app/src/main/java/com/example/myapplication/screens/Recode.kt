// app/src/main/java/com/example/myapplication/screens/RecodeScreen.kt
package com.example.myapplication.screens

import android.media.MediaPlayer
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.myapplication.R
import com.example.myapplication.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@Composable
fun RecodeUI(
    navController: NavController,
    patientId: String,
    samples: List<String>,
    error: String?,
    isLoading: Boolean,
    onPlaySample: (String) -> Unit,
    onRecordClick: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val (logo, title, selectBtn, sampleBox, errText, bottomBtn) = createRefs()

        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            modifier = Modifier
                .size(200.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 80.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                }
        )

        Text(
            text = "보호자 음성 등록",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(logo.bottom, margin = 24.dp)
                start.linkTo(parent.start); end.linkTo(parent.end)
            }
        )

        Button(
            onClick = { /* TODO: 파일 선택 (녹음/업로드 화면 분리 시 그대로) */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(selectBtn) {
                    top.linkTo(title.bottom, margin = 32.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("목소리 선택", color = Color.White, fontSize = 16.sp)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                .padding(8.dp)
                .constrainAs(sampleBox) {
                    top.linkTo(selectBtn.bottom, margin = 24.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                    bottom.linkTo(bottomBtn.top, margin = if (error != null) 56.dp else 16.dp)
                    height = Dimension.preferredWrapContent
                }
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp))
                            Spacer(Modifier.width(12.dp))
                            Text("불러오는 중…", fontSize = 16.sp, color = Color.Gray)
                        }
                    }
                }
                samples.isEmpty() -> {
                    Text("등록된 목소리가 없습니다", color = Color.Gray, modifier = Modifier.padding(16.dp))
                }
                else -> {
                    samples.forEachIndexed { idx, url ->
                        Text(
                            text = "${idx + 1}. 샘플 듣기",
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPlaySample(url) }
                                .padding(vertical = 8.dp)
                        )
                        if (idx < samples.lastIndex) Divider()
                    }
                }
            }
        }

        error?.let {
            Text(
                text = "목소리 목록 오류: $it",
                color = Color.Red,
                modifier = Modifier
                    .constrainAs(errText) {
                        top.linkTo(sampleBox.bottom, margin = 8.dp)
                        bottom.linkTo(bottomBtn.top, margin = 16.dp)
                        start.linkTo(parent.start); end.linkTo(parent.end)
                    }
                    .padding(4.dp)
            )
        }

        Button(
            onClick = onRecordClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(bottomBtn) {
                    bottom.linkTo(parent.bottom, margin = 40.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("녹음하러 가기", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Composable
fun RecodeScreen(
    navController: NavController,
    patientId: String,
    onSelectVoice: (String) -> Unit
) {
    val context = LocalContext.current

    var samples by remember { mutableStateOf<List<String>>(emptyList()) } // 재생 가능한 HTTPS URL 목록
    var error by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // 단일 MediaPlayer 인스턴스 관리 (겹침 방지)
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.release()
            } catch (_: Exception) {
            }
            mediaPlayer = null
        }
    }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 서버 응답(body)을 파싱해 "URL 또는 Storage 경로" 리스트로 변환.
     * 지원 포맷:
     * - ["voiceSamples/UID/xxx.wav", "https://...signed..."] (혼합 허용)
     * - { "voiceSamples": [ ... ] }
     * - [{ "url": "...signed..." }, { "signedUrl": "..." }, { "path": "voiceSamples/..." } ]
     */
    fun parseStoragePaths(body: String): List<String> {
        fun takeFromArray(ja: JSONArray): List<String> {
            val out = mutableListOf<String>()
            for (i in 0 until ja.length()) {
                when (val el = ja.get(i)) {
                    is String -> out += el
                    is JSONObject -> {
                        // 1) 완전 URL 우선 (옵션 A)
                        val urlKeys = listOf("url", "signedUrl")
                        var hit: String? = null
                        for (k in urlKeys) {
                            if (el.has(k) && el.opt(k) is String) {
                                val v = el.optString(k)
                                if (v.startsWith("http://", true) || v.startsWith(
                                        "https://",
                                        true
                                    )
                                ) {
                                    hit = v
                                    break
                                }
                            }
                        }
                        if (hit != null) {
                            out += hit; continue
                        }

                        // 2) 경로 키 (옵션 B)
                        val pathKeys = listOf(
                            "path", "storagePath", "fullPath", "gsUrl", "gsUri", "gs",
                            "name", "object", "filePath"
                        )
                        for (k in pathKeys) {
                            if (el.has(k) && el.opt(k) is String) {
                                hit = el.optString(k)
                                break
                            }
                        }
                        if (hit != null) out += hit
                    }
                }
            }
            return out
        }

        // 배열 먼저 시도
        try {
            val ja = JSONArray(body)
            return takeFromArray(ja)
        } catch (_: Exception) { /* not an array */
        }

        // 객체 안 배열
        return try {
            val jo = JSONObject(body)
            val keys = listOf("voiceSamples", "items", "paths", "samples", "data")
            for (k in keys) {
                val arr = jo.optJSONArray(k)
                if (arr != null) return takeFromArray(arr)
            }
            emptyList<String>()
        } catch (_: Exception) {
            emptyList<String>()
        }
    }

    /**
     * Storage 경로(or gs://, or http) → 재생 가능한 HTTPS URL로 변환
     * - http/https 시작: 그대로 사용 (서명 URL 등)
     * - gs:// 시작: getReferenceFromUrl(...).downloadUrl
     * - 기타: 기본 버킷 reference.child(path).downloadUrl
     */
    suspend fun resolvePlayableUrls(paths: List<String>): List<String> =
        withContext(Dispatchers.IO) {
            val storage = Firebase.storage
            kotlinx.coroutines.coroutineScope {
                paths.map { raw ->
                    async<String> {
                        val trimmed = raw.trim()
                        try {
                            when {
                                trimmed.startsWith("http://", true) ||
                                        trimmed.startsWith("https://", true) -> {
                                    trimmed
                                }

                                trimmed.startsWith("gs://", true) -> {
                                    val ref = storage.getReferenceFromUrl(trimmed)
                                    ref.downloadUrl.await().toString()
                                }

                                else -> {
                                    val ref = storage.reference.child(trimmed.removePrefix("/"))
                                    ref.downloadUrl.await().toString()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("RecodeScreen", "resolvePlayableUrl 실패: $trimmed, ${e.message}")
                            ""
                        }
                    }
                }.awaitAll().filter { it.isNotBlank() }
            }
        }

    // 샘플 목록 로드
    suspend fun loadSamples() {
        withContext(Dispatchers.Main) {
            isLoading = true
            error = null
        }
        try {
            // 1) Firebase ID 토큰 (백엔드 인증용)
            val idToken = Firebase.auth.currentUser
                ?.getIdToken(true)
                ?.await()
                ?.token
                .orEmpty()

            val url = "${BuildConfig.BASE_URL}/voice-sample/list/$patientId"
            Log.d("RecodeScreen", "샘플 호출 URL: $url")

            val body = withContext(Dispatchers.IO) {
                val req = Request.Builder()
                    .url(url)
                    .get()
                    .apply {
                        if (idToken.isNotBlank()) addHeader(
                            "Authorization",
                            "Bearer $idToken"
                        )
                    }
                    .build()

                client.newCall(req).execute().use { resp ->
                    val code = resp.code
                    val text = resp.body?.string().orEmpty()
                    Log.d("RecodeScreen", "샘플 응답 JSON: $text")
                    if (!resp.isSuccessful) throw RuntimeException("HTTP $code")
                    text
                }
            }

            // 2) URL/경로 파싱
            val rawList = parseStoragePaths(body)
            Log.d("RecodeScreen", "파싱된 항목 수: ${rawList.size}")

            // 3) 경로 → downloadUrl 변환 + 중복 제거
            val playableUrls = resolvePlayableUrls(rawList).distinct()
            Log.d("RecodeScreen", "재생 가능한 URL 수: ${playableUrls.size}")

            withContext(Dispatchers.Main) { samples = playableUrls }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                error = e.message
                Toast.makeText(context, "샘플 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            Log.e("RecodeScreen", "loadSamples 예외", e)
        } finally {
            withContext(Dispatchers.Main) { isLoading = false }
        }
    }

    // 최초 진입 시
    LaunchedEffect(patientId) {
        scope.launch { loadSamples() }
    }

    // 화면 복귀 시 갱신
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, ev ->
            if (ev == Lifecycle.Event.ON_RESUME) {
                scope.launch { loadSamples() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    RecodeUI(
        navController = navController,
        patientId = patientId,
        samples = samples,
        error = error,
        isLoading = isLoading,
        onPlaySample = { url ->
            onSelectVoice(url)
            try {
                // 기존 재생 중이면 정리
                mediaPlayer?.let {
                    try {
                        it.stop()
                    } catch (_: Exception) {
                    }
                    try {
                        it.reset()
                    } catch (_: Exception) {
                    }
                    try {
                        it.release()
                    } catch (_: Exception) {
                    }
                }

                mediaPlayer = MediaPlayer().apply {
                    // 1) 오디오 속성 지정
                    setAudioAttributes(
                        android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )

                    // 2) URL 문자열 대신 Context+Uri 사용 (리다이렉트 대응)
                    setDataSource(
                        context,
                        android.net.Uri.parse(url)
                    )

                    setOnPreparedListener { it.start() }

                    setOnErrorListener { mp, what, extra ->
                        // what/extra 로깅
                        Log.e(
                            "RecodeScreen",
                            "MediaPlayer onError what=$what extra=$extra url=$url"
                        )
                        try {
                            mp.release()
                        } catch (_: Exception) {
                        }
                        mediaPlayer = null

                        // --- 폴백: URL에서 Storage 경로 추출 → getDownloadUrl()로 재시도 ---
                        val storagePath: String? = try {
                            // GCS 서명 URL: https://storage.googleapis.com/<bucket>/<path>?...
                            val gcs =
                                Regex("""https?://storage\.googleapis\.com/[^/]+/(.+?)(?:\?|$)""")
                                    .find(url)
                                    ?.groupValues?.getOrNull(1)

                            // Firebase 토큰 URL: https://firebasestorage.googleapis.com/v0/b/<bucket>/o/<urlencoded-path>?...
                            val fb =
                                Regex("""https?://firebasestorage\.googleapis\.com/v0/b/[^/]+/o/([^?]+)""")
                                    .find(url)
                                    ?.groupValues?.getOrNull(1)
                                    ?.let { java.net.URLDecoder.decode(it, "UTF-8") }

                            (gcs ?: fb)?.removePrefix("/")
                        } catch (_: Exception) {
                            null
                        }

                        if (!storagePath.isNullOrBlank()) {
                            Firebase.storage.reference.child(storagePath)
                                .downloadUrl
                                .addOnSuccessListener { newUri ->
                                    // 새 URL로 재생 재시도
                                    try {
                                        mediaPlayer = MediaPlayer().apply {
                                            setAudioAttributes(
                                                android.media.AudioAttributes.Builder()
                                                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                                                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
                                                    .build()
                                            )
                                            setDataSource(
                                                context,
                                                android.net.Uri.parse(newUri.toString())
                                            )
                                            setOnPreparedListener { it.start() }
                                            setOnCompletionListener { mpp ->
                                                try {
                                                    mpp.release()
                                                } catch (_: Exception) {
                                                }
                                                mediaPlayer = null
                                            }
                                            setOnErrorListener { mpp, w, e2 ->
                                                Log.e(
                                                    "RecodeScreen",
                                                    "fallback onError what=$w extra=$e2"
                                                )
                                                try {
                                                    mpp.release()
                                                } catch (_: Exception) {
                                                }
                                                mediaPlayer = null
                                                true
                                            }
                                            prepareAsync()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(
                                            context,
                                            "폴백 재생 실패: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        context,
                                        "URL 복구 실패: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            Toast.makeText(
                                context,
                                "재생 오류(네트워크/URL). 잠시 후 다시 시도하세요.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        // 우리가 처리했으니 true로 소비
                        true
                    }

                    setOnCompletionListener { mp ->
                        try {
                            mp.release()
                        } catch (_: Exception) {
                        }
                        mediaPlayer = null
                    }

                    prepareAsync()
                }
            } catch (e: Exception) {
                Toast.makeText(context, "재생 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("RecodeScreen", "재생 실패", e)
                try {
                    mediaPlayer?.release()
                } catch (_: Exception) {
                }
                mediaPlayer = null
            }
        },
        onRecordClick = {
            navController.navigate("recode2/$patientId")
        }
    )
}
















