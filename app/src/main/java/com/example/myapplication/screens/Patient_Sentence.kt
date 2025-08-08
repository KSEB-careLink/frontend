// app/src/main/java/com/example/myapplication/screens/Patient_Sentence.kt
package com.example.myapplication.screens

import android.Manifest
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.example.myapplication.service.LocationUpdatesService
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONArray
import org.json.JSONObject
import android.util.Log
import androidx.compose.material.icons.filled.Timer
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Patient_Sentence(
    navController: NavController,
    patientId: String,
) {
    // ─── 0) patientId 복구 ─────────────────────────────────────────────────────
    val context = LocalContext.current
    val activePatientId by remember(patientId) {
        mutableStateOf(resolvePatientId(context, patientId))
    }

    // ─── 1) 위치 권한 + 서비스 시작 ────────────────────────────────────────────
    val perms = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    LaunchedEffect(perms.allPermissionsGranted) {
        if (!perms.allPermissionsGranted) {
            perms.launchMultiplePermissionRequest()
        } else {
            Intent(context, LocationUpdatesService::class.java).also { intent ->
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }

    // ─── 2) OkHttp 클라이언트 (로깅 포함) ──────────────────────────────────────
    val client = remember {
        val logging = HttpLoggingInterceptor { msg -> Log.d("HTTP", msg) }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    val scope = rememberCoroutineScope()

    // ─── 3) 데이터 모델 ────────────────────────────────────────────────────────
    data class MemoryOne(
        val sentence: String,
        val imageUrl: String?, // 옵션 (없을 수 있음)
        val mp3Url: String
    )

    // 히스토리 + 현재 인덱스
    val history = remember { mutableStateListOf<MemoryOne>() }
    var currentIndex by remember { mutableStateOf(-1) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // ★ TTS 엔드포인트 1회만 해두고 재사용
    var ttsEndpoint by remember { mutableStateOf<String?>(null) }

    // ─── 4) 유틸 ──────────────────────────────────────────────────────────────
    fun normalizeImageUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        if (url.startsWith("gs://")) {
            val noScheme = url.removePrefix("gs://")
            val slash = noScheme.indexOf('/')
            if (slash > 0) {
                val bucket = noScheme.substring(0, slash)
                val path = noScheme.substring(slash + 1)
                return "https://storage.googleapis.com/$bucket/$path"
            }
        }
        return url
    }

    fun withCacheBuster(url: String): String {
        val sep = if (url.contains("?")) "&" else "?"
        return "$url${sep}_=${System.currentTimeMillis()}"
    }

    // ─── 5) 환자 최신 사진 Fallback 조회 ──────────────────────────────────────
    suspend fun fetchFallbackImageUrl(
        baseUrl: String,
        patientId: String,
        idToken: String,
        client: OkHttpClient
    ): String? {
        val base = baseUrl.trimEnd('/')
        val pidEnc = Uri.encode(patientId)
        val candidates = listOf(
            "$base/photos/patient/$pidEnc/latest",                   // { image_url: "..." }
            "$base/photos/patient/$pidEnc?limit=1&order=desc",       // [ { image_url: "..." } ]
            "$base/photos?patient_id=$pidEnc&limit=1&order=desc"     // [ { image_url: "..." } ]
        )

        for (raw in candidates) {
            val url = withCacheBuster(raw)
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $idToken")
                .addHeader("Cache-Control", "no-cache, no-store")
                .addHeader("Pragma", "no-cache")
                .get()
                .build()
            val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
            val code = resp.code
            Log.d("PhotosAPI", "probe $url -> $code")
            if (!resp.isSuccessful) continue
            val body = resp.body?.string().orEmpty()
            if (body.isBlank()) continue

            // object 형
            runCatching {
                val o = JSONObject(body)
                val img = normalizeImageUrl(o.optString("image_url"))
                if (!img.isNullOrBlank()) return img
            }

            // array 형
            runCatching {
                val arr = JSONArray(body)
                if (arr.length() > 0) {
                    val img = normalizeImageUrl(arr.getJSONObject(0).optString("image_url"))
                    if (!img.isNullOrBlank()) return img
                }
            }
        }
        return null
    }

    // ─── 6) /memories tts 경로 자동 탐색(1회) ────────────────────────────────
    suspend fun resolveMemoriesTtsUrlOnce(
        baseUrl: String,
        patientId: String,
        idToken: String,
        client: OkHttpClient
    ): String {
        ttsEndpoint?.let { return it } // 이미 있으면 재사용

        val base = baseUrl.trimEnd('/')
        val pidEnc = Uri.encode(patientId)
        val candidates = listOf(
            "$base/memories/$pidEnc/tts",          // 문서 스펙
            "$base/memories/tts/$pidEnc",          // 반대 패턴
            "$base/memory/$pidEnc/tts",            // 단수
            "$base/memories/tts?patientId=$pidEnc" // 쿼리 파라미터
        )

        for (raw in candidates) {
            val probeUrl = withCacheBuster(raw)
            val req = Request.Builder()
                .url(probeUrl)
                .addHeader("Authorization", "Bearer $idToken")
                .addHeader("Cache-Control", "no-cache, no-store")
                .addHeader("Pragma", "no-cache")
                .get()
                .build()

            val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
            val code = resp.code
            Log.d("MemoriesAPI", "Probe URL: $probeUrl -> $code")

            if (code == 200) {
                // 원래의 raw(캐시버스터 없는 베이스)를 저장, 호출 시엔 매번 캐시버스터 붙임
                ttsEndpoint = raw
                return raw
            }
            if (code == 204) throw Exception("아직 생성된 회상 문장이 없습니다. (204)")
        }
        throw Exception("TTS 엔드포인트를 찾지 못했습니다.")
    }

    // ─── 7) 랜덤 1개 호출 (+중복 회피 재시도) ─────────────────────────────────
    suspend fun fetchOneRandom(ctx: Context, avoidDuplicateOf: MemoryOne? = null): MemoryOne {
        val idToken = Firebase.auth.currentUser
            ?.getIdToken(false)?.await()?.token
            ?: throw Exception("인증 토큰 없음")

        if (activePatientId.isBlank()) {
            throw Exception("patientId 없음(네비/SharedPreferences/Firebase 모두 비어 있음)")
        }

        val baseEndpoint = resolveMemoriesTtsUrlOnce(
            baseUrl = BuildConfig.BASE_URL,
            patientId = activePatientId,
            idToken = idToken,
            client = client
        )

        // 최대 3번까지 중복 회피 재시도
        repeat(3) { attempt ->
            val workingUrl = withCacheBuster(baseEndpoint)
            val resp = withContext(Dispatchers.IO) {
                client.newCall(
                    Request.Builder()
                        .url(workingUrl)
                        .addHeader("Authorization", "Bearer $idToken")
                        .addHeader("Cache-Control", "no-cache, no-store")
                        .addHeader("Pragma", "no-cache")
                        .get()
                        .build()
                ).execute()
            }

            if (resp.code == 204) throw Exception("아직 생성된 회상 문장이 없습니다. (204)")
            if (!resp.isSuccessful) throw Exception("API 오류 ${resp.code} @ $workingUrl")

            val bodyStr = resp.body?.string().orEmpty()
            Log.d("MemoriesAPI", "OK body: $bodyStr")
            val obj = JSONObject(bodyStr)

            val sentence = obj.optString("sentence", obj.optString("reminder_text"))
            var imageOpt = normalizeImageUrl(
                obj.optString("image_url", obj.optString("image", ""))
                    .takeIf { it.isNotBlank() }
            )
            val mp3 = obj.optString("mp3_url", obj.optString("tts_url"))

            if (sentence.isBlank() || mp3.isBlank()) {
                Log.e("MemoriesAPI", "응답 필드 부족: sentence='${sentence}', image='${imageOpt ?: ""}', mp3='${mp3}'")
                throw Exception("응답 필드 부족 (sentence/mp3_url)")
            }

            if (imageOpt == null) {
                imageOpt = fetchFallbackImageUrl(
                    baseUrl = BuildConfig.BASE_URL,
                    patientId = activePatientId,
                    idToken = idToken,
                    client = client
                )
            }

            val item = MemoryOne(sentence, imageOpt, mp3)

            // 직전 항목과 동일하면 다시 시도
            val dupBase = avoidDuplicateOf ?: history.lastOrNull()
            val isDup =
                dupBase != null &&
                        dupBase.sentence == item.sentence &&
                        dupBase.mp3Url == item.mp3Url &&
                        (dupBase.imageUrl ?: "") == (item.imageUrl ?: "")

            if (!isDup) {
                Log.d("MemoriesAPI", "Using URL: $baseEndpoint  (patientId=$activePatientId)")
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        ctx,
                        "TTS 새로고침 완료",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return item
            } else {
                Log.d("MemoriesAPI", "중복 감지(${attempt + 1}/3), 재시도")
                delay(150)
            }
        }

        // 그래도 중복이면 마지막 항목 반환(백엔드에서 실제로 바뀌지 않는 경우)
        val idToken2 = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token ?: ""
        val fallbackUrl = withCacheBuster(baseEndpoint)
        val respFinal = withContext(Dispatchers.IO) {
            client.newCall(
                Request.Builder()
                    .url(fallbackUrl)
                    .addHeader("Authorization", "Bearer $idToken2")
                    .addHeader("Cache-Control", "no-cache, no-store")
                    .addHeader("Pragma", "no-cache")
                    .get()
                    .build()
            ).execute()
        }
        val obj = JSONObject(respFinal.body?.string().orEmpty())
        val sentence = obj.optString("sentence", obj.optString("reminder_text"))
        val mp3 = obj.optString("mp3_url", obj.optString("tts_url"))
        var imageOpt = normalizeImageUrl(
            obj.optString("image_url", obj.optString("image", ""))
                .takeIf { it.isNotBlank() }
        ) ?: fetchFallbackImageUrl(BuildConfig.BASE_URL, activePatientId, idToken2, client)
        return MemoryOne(sentence, imageOpt, mp3)
    }

    suspend fun loadInitial() {
        isLoading = true
        errorMsg = null
        try {
            val first = fetchOneRandom(context)
            history.clear()
            history += first
            currentIndex = 0
        } catch (e: Exception) {
            history.clear()
            currentIndex = -1
            errorMsg = "로드 실패: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(activePatientId) {
        loadInitial()
    }

    // ─── 8) 다음/이전 이동 로직 ───────────────────────────────────────────────
    fun canGoPrev() = currentIndex > 0
    fun canGoNext() = true // 다음은 항상 가능(끝이면 새 랜덤 fetch)

    suspend fun goPrev() {
        stopTTS()
        if (canGoPrev()) currentIndex -= 1
    }

    suspend fun goNext() {
        stopTTS()
        if (currentIndex + 1 < history.size) {
            currentIndex += 1
            return
        }
        isLoading = true
        errorMsg = null
        try {
            val last = history.lastOrNull()
            val one = fetchOneRandom(context, avoidDuplicateOf = last)
            history += one
            currentIndex += 1
        } catch (e: Exception) {
            errorMsg = "다음 문장 불러오기 실패: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // 인덱스가 바뀌면 자동으로 해당 항목 TTS 재생
    LaunchedEffect(currentIndex) {
        if (currentIndex in history.indices) {
            val mem = history[currentIndex]
            playTTS(mem.mp3Url, context)
        }
    }

    // ─── 9) UI ────────────────────────────────────────────────────────────────
    Scaffold(
        bottomBar = {
            val navBackStack by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStack?.destination?.route
            val navColors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.Transparent,
                selectedIconColor = Color(0xFF00C4B4),
                unselectedIconColor = Color(0xFF888888),
                selectedTextColor = Color(0xFF00C4B4),
                unselectedTextColor = Color(0xFF888888)
            )
            NavigationBar {
                listOf(
                    "sentence/{patientId}" to "회상문장",
                    "quiz/{patientId}" to "회상퀴즈",
                    "alert" to "긴급알림"
                ).forEach { (routePattern, label) ->
                    val selected = when {
                        routePattern.startsWith("sentence/") || routePattern == "sentence/{patientId}" ->
                            currentRoute?.startsWith("sentence/") == true
                        routePattern.startsWith("quiz/") || routePattern == "quiz/{patientId}" ->
                            currentRoute?.startsWith("quiz/") == true
                        else -> currentRoute == routePattern
                    }
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Timer, contentDescription = label) },
                        label = { Text(label) },
                        selected = selected,
                        onClick = {
                            val pid = activePatientId
                            val target = when (routePattern) {
                                "sentence/{patientId}" -> "sentence/$pid"
                                "quiz/{patientId}" -> "quiz/$pid"
                                else -> routePattern
                            }
                            if (currentRoute != target) {
                                navController.navigate(target) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        },
                        colors = navColors
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.rogo),
                contentDescription = "Logo",
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    buildAnnotatedString {
                        append("기억 ")
                        withStyle(SpanStyle(color = Color(0xFF00C4B4))) { append("나시나요?") }
                    },
                    fontSize = 24.sp
                )
            }
            Spacer(Modifier.height(16.dp))

            // 상단 우측: 새로고침(최초부터 다시)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                FilledTonalButton(
                    onClick = { scope.launch { stopTTS(); loadInitial() } },
                    enabled = !isLoading
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("처음부터")
                }
            }
            Spacer(Modifier.height(12.dp))

            when {
                isLoading && history.isEmpty() -> {
                    CircularProgressIndicator()
                }
                errorMsg != null && history.isEmpty() -> {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                }
                currentIndex !in history.indices -> {
                    Text("표시할 회상 문장이 없습니다.", fontSize = 16.sp)
                }
                else -> {
                    val mem = history[currentIndex]

                    // 이미지(옵션): 없으면 placeholder UI
                    if (mem.imageUrl != null) {
                        val req = ImageRequest.Builder(context)
                            .data(mem.imageUrl)
                            .crossfade(true)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .networkCachePolicy(CachePolicy.DISABLED)
                            .build()

                        AsyncImage(
                            model = req,
                            contentDescription = "Memory Photo",
                            placeholder = painterResource(R.drawable.rogo),
                            error = painterResource(R.drawable.rogo),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(220.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("이미지 없음", color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))

                    // 문장 + 수동 재생 버튼(현재 문장 다시 듣기)
                    Button(
                        onClick = { scope.launch { stopTTS(); playTTS(mem.mp3Url, context) } },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                    ) {
                        Text(mem.sentence, color = Color.White, fontSize = 16.sp)
                    }
                    Spacer(Modifier.height(16.dp))

                    // 이전/다음 네비게이션
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { scope.launch { goPrev() } },
                            enabled = canGoPrev() && !isLoading
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "이전")
                            Spacer(Modifier.width(8.dp))
                            Text("이전 문장")
                        }

                        Text("${currentIndex + 1} / ${history.size}", fontSize = 14.sp)

                        Button(
                            onClick = { scope.launch { goNext() } },
                            enabled = canGoNext() && !isLoading
                        ) {
                            Text("다음 문장")
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = "다음")
                        }
                    }

                    if (isLoading && history.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    errorMsg?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // ─── 10) MediaPlayer 정리 ────────────────────────────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            stopTTS()
        }
    }
}

// ────────────────────────────────────────────────
// Pref & patientId 복구 유틸
// ────────────────────────────────────────────────
private fun getPatientIdFromPrefs(context: Context): String? {
    val prefs = context.getSharedPreferences("MyPrefs", MODE_PRIVATE)
    return prefs.getString("patient_id", null)
}

private fun resolvePatientId(context: Context, param: String): String {
    val cleaned = param.trim()
    if (cleaned.isNotEmpty() && cleaned != "{patientId}" && cleaned.lowercase() != "null") {
        return cleaned
    }
    // 네비 값이 placeholder면 Pref → Firebase uid 순으로 복구
    getPatientIdFromPrefs(context)?.let { return it }
    Firebase.auth.currentUser?.uid?.let { return it }
    return "" // 비어있으면 fetch 단계에서 에러로 처리
}

// ────────────────────────────────────────────────
// TTS helpers
// ────────────────────────────────────────────────
private var currentTTSPlayer: MediaPlayer? = null

private fun stopTTS() {
    currentTTSPlayer?.run {
        try { stop() } catch (_: Exception) {}
        try { reset() } catch (_: Exception) {}
        release()
    }
    currentTTSPlayer = null
}

private suspend fun playTTS(
    ttsUrl: String,
    context: Context
) {
    try {
        // 알림음
        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        delay(200)
        toneGen.release()

        // 기존 플레이어 정리
        stopTTS()

        // 재생
        currentTTSPlayer = MediaPlayer().apply {
            setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            // http/https는 문자열 오버로드로 직접 지정
            setDataSource(ttsUrl)
            setOnPreparedListener { it.start() }
            setOnErrorListener { _, what, extra ->
                Toast.makeText(context, "TTS 오류: what=$what, extra=$extra", Toast.LENGTH_SHORT).show()
                true
            }
            prepareAsync()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "TTS 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}




















