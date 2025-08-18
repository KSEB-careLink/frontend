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
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
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
import java.util.concurrent.TimeUnit
import androidx.compose.material.icons.filled.CheckCircle

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

    // 초기 첫 Compose에서 빈 리스트 접근 방지 위해 true로 시작
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // ✔ 이미 본 조합 기록(중복 방지)
    val seenKeys = remember { mutableSetOf<String>() }

    // ─── 4) gs:// → https 변환 ────────────────────────────────────────────────
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

    // 문장 분해(불릿/따옴표/공백 제거)
    fun splitVariants(raw: String): List<String> =
        raw.lines()
            .map { it.trim() }
            .map { it.removePrefix("-").removePrefix("•").trim() }
            .map { it.trim('"') }
            .filter { it.isNotBlank() }

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

        for (url in candidates) {
            val req = Request.Builder()
                .url(url)
                .addHeader("Authorization", "Bearer $idToken")
                .get()
                .build()
            val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
            val code = resp.code
            Log.d("PhotosAPI", "probe $url -> $code")
            if (!resp.isSuccessful) continue
            val body = resp.body?.string().orEmpty()
            if (body.isBlank()) continue

            runCatching {
                val o = JSONObject(body)
                val img = normalizeImageUrl(o.optString("image_url"))
                if (!img.isNullOrBlank()) return img
            }
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

    // ─── 6) /memories tts 경로: 네트워크 호출 없이 '계산'만 ───────────────────────
    fun resolveMemoriesTtsUrl(
        baseUrl: String,
        patientId: String
    ): String {
        val base = baseUrl.trimEnd('/')
        val pidEnc = Uri.encode(patientId)
        // 문서 스펙 경로 고정 사용 (필요시 다른 패턴은 fetchOneRandom에서 순차 시도)
        return "$base/memories/$pidEnc/tts"
    }


    // ─── 7) 랜덤 1개 호출 (문장 분해 + 중복 방지) ─────────────────────────────
    suspend fun fetchOneRandom(ctx: Context): MemoryOne {
        val idToken = Firebase.auth.currentUser
            ?.getIdToken(false)?.await()?.token
            ?: throw Exception("인증 토큰 없음")

        if (activePatientId.isBlank()) {
            throw Exception("patientId 없음(네비/SharedPreferences/Firebase 모두 비어 있음)")
        }

        // ✨ 탐색 GET 제거: URL만 계산
        val primaryUrl = resolveMemoriesTtsUrl(
            baseUrl = BuildConfig.BASE_URL,
            patientId = activePatientId
        )

        // 필요시 대체 경로도 '실제 사용 시점'에만 시도
        val candidates = listOf(
            primaryUrl, // 문서 스펙
            BuildConfig.BASE_URL.trimEnd('/') + "/memories/tts/${Uri.encode(activePatientId)}",
            BuildConfig.BASE_URL.trimEnd('/') + "/memory/${Uri.encode(activePatientId)}/tts",
            BuildConfig.BASE_URL.trimEnd('/') + "/memories/tts?patientId=${Uri.encode(activePatientId)}"
        )

        val maxTries = 4
        var lastErr: Exception? = null

        for (endpoint in candidates) {
            repeat(maxTries) { attempt ->
                // ✅ nonce( ?_=... ) 제거: 서버 분기/오작동 방지
                val req = Request.Builder()
                    .url(endpoint)
                    .addHeader("Authorization", "Bearer $idToken")
                    .addHeader("Cache-Control", "no-cache, no-store")
                    .addHeader("Pragma", "no-cache")
                    .get()
                    .build()

                val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
                val bodyStr = resp.body?.string().orEmpty()

                if (resp.code == 204) {
                    // 서버가 진짜 "아직 없음"을 의미하는 케이스
                    lastErr = Exception("아직 생성된 회상 문장이 없습니다. (204)")
                    // 다음 후보 endpoint로 넘어감
                    return@repeat
                }

                if (!resp.isSuccessful) {
                    // 404 이면서 서버 메시지가 "아직 없습니다"면 짧게 재시도 후 같은 endpoint 재시도
                    if (resp.code == 404 && bodyStr.contains("회상 문장이 아직 없습니다")) {
                        if (attempt < maxTries - 1) {
                            // 짧은 backoff
                            withContext(Dispatchers.IO) { Thread.sleep(200) }
                            return@repeat
                        } else {
                            lastErr = Exception("서버 응답 404: 선택된 사진 회상문장 없음(재시도 소진) @ $endpoint")
                            return@repeat
                        }
                    } else {
                        lastErr = Exception("API 오류 ${resp.code} @ $endpoint: $bodyStr")
                        return@repeat
                    }
                }

                // 200 성공
                Log.d("MemoriesAPI", "OK body: $bodyStr")
                val obj = JSONObject(bodyStr)

                val full = obj.optString("sentence", obj.optString("reminder_text"))
                val variants = splitVariants(full)
                var imageOpt = normalizeImageUrl(
                    obj.optString("image_url", obj.optString("image", ""))
                        .takeIf { it.isNotBlank() }
                )
                val mp3 = obj.optString("mp3_url", obj.optString("tts_url"))

                if (variants.isEmpty() || mp3.isBlank()) {
                    Log.e(
                        "MemoriesAPI",
                        "응답 필드 부족: sentence='${full}', image='${imageOpt ?: ""}', mp3='${mp3}'"
                    )
                    lastErr = Exception("응답 필드 부족 (sentence/mp3_url)")
                    return@repeat
                }

                if (imageOpt == null) {
                    imageOpt = fetchFallbackImageUrl(
                        baseUrl = BuildConfig.BASE_URL,
                        patientId = activePatientId,
                        idToken = idToken,
                        client = client
                    )
                }

                // 아직 보지 않은 문장 변형 선택
                val shuffled = variants.shuffled()
                val picked = shuffled.firstOrNull { v ->
                    val key = "$v|$mp3|${imageOpt ?: ""}"
                    !seenKeys.contains(key)
                }

                if (picked == null) {
                    Log.d("MemoriesAPI", "중복 감지(모든 변형 사용) → 재시도 ($attempt/${maxTries - 1})")
                    if (attempt < maxTries - 1) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(ctx, "새 문장을 찾는 중…", Toast.LENGTH_SHORT).show()
                        }
                        withContext(Dispatchers.IO) { Thread.sleep(200) }
                        return@repeat
                    } else {
                        lastErr = Exception("새 문장을 더 받지 못했습니다(서버가 동일 응답만 반환).")
                        return@repeat
                    }
                }

                // 마지막 성공 상태 저장
                withContext(Dispatchers.Main) {
                    // 필요하면 안내 토스트 유지
                    Toast.makeText(
                        ctx,
                        "TTS 경로: ${endpoint.removePrefix(BuildConfig.BASE_URL)}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                // 최근 값 Pref 저장
                val prefs = ctx.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                prefs.edit()
                    .putString("last_memory_sentence", picked)
                    .putString("last_memory_image_url", imageOpt ?: "")
                    .apply()

                return MemoryOne(picked, imageOpt, mp3)
            }
        }

        // 모든 후보/재시도 실패
        throw (lastErr ?: Exception("여러 번 시도했지만 새로운 문장을 받지 못했습니다."))
    }

    suspend fun loadInitial() {
        isLoading = true
        errorMsg = null
        try {
            val first = fetchOneRandom(context)
            history.clear()
            seenKeys.clear()
            history += first
            seenKeys += "${first.sentence}|${first.mp3Url}|${first.imageUrl ?: ""}"
            currentIndex = 0
        } catch (e: Exception) {
            history.clear()
            seenKeys.clear()
            currentIndex = -1
            errorMsg = "로드 실패: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    LaunchedEffect(activePatientId) {
        loadInitial()
    }

    // history 크기가 변하면 인덱스를 안전 구간으로 보정
    LaunchedEffect(history.size) {
        if (history.isNotEmpty() && currentIndex !in history.indices) {
            currentIndex = 0
        }
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
            val one = fetchOneRandom(context)
            val key = "${one.sentence}|${one.mp3Url}|${one.imageUrl ?: ""}"
            if (seenKeys.contains(key)) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "새 문장을 아직 받지 못했어요. 잠시 후 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
                }
                return
            }
            history += one
            seenKeys += key
            currentIndex += 1
        } catch (e: Exception) {
            errorMsg = "다음 문장 불러오기 실패: ${e.message}"
            withContext(Dispatchers.Main) {
                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
            }
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
            // ← 기존 bottomBar 그대로 유지
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
                        icon = { Icon(Icons.Default.Star, contentDescription = label) },
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
        // history 빈 처리 (기존 그대로)
        if (history.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (isLoading) {
                    CircularProgressIndicator()
                } else {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF00C4B4),
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(text = errorMsg ?: "오늘의 회상정보를 모두 보셨습니다.", fontSize = 18.sp)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { scope.launch { stopTTS(); loadInitial() } }) {
                        Text("다시 시도")
                    }
                }
            }
            return@Scaffold
        }

        val safeIndex = currentIndex.coerceIn(0, history.lastIndex)
        val mem = history[safeIndex]

        Box(
            Modifier
                .fillMaxSize()
                .padding(innerPadding) // 바텀바 인셋 반영
        ) {
            // ⬆️ 스크롤 본문(안전판: Column + verticalScroll)
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp)
                    // 하단 고정 컨트롤에 가리지 않도록 아래쪽 여백
                    .padding(bottom = 160.dp)
                    .verticalScroll(scrollState)
            ) {
                // 화면을 아래로 내리기 위한 스페이서 (원하면 80~160dp로 조절)
                Spacer(Modifier.height(120.dp))

                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        buildAnnotatedString {
                            append("기억 "); withStyle(SpanStyle(color = Color(0xFF00C4B4))) { append("나시나요?") }
                        },
                        fontSize = 24.sp
                    )
                }
                Spacer(Modifier.height(16.dp))

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

                if (mem.imageUrl != null) {
                    AsyncImage(
                        model = mem.imageUrl,
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
                    ) { Text("이미지 없음", color = Color.Gray, fontSize = 14.sp) }
                }
                Spacer(Modifier.height(12.dp))

                // ✅ 긴 문장: 카드 안에서 스크롤 → 절대 잘리지 않음
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 72.dp, max = 260.dp)
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp)
                    ) {
                        Text(
                            text = mem.sentence,
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            softWrap = true
                            // maxLines/overflow 미지정 → 끊김 없음
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { scope.launch { stopTTS(); playTTS(mem.mp3Url, context) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("다시 듣기")
                }
                Spacer(Modifier.height(12.dp))
            }

            // ⬇️ 하단 고정 컨트롤 (바텀바 위)
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .navigationBarsPadding()
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedButton(
                        onClick = { scope.launch { stopTTS(); if (safeIndex > 0) currentIndex -= 1 } },
                        enabled = safeIndex > 0 && !isLoading
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "이전")
                        Spacer(Modifier.width(8.dp))
                        Text("이전 문장")
                    }

                    Text("${safeIndex + 1} / ${history.size}", fontSize = 14.sp)

                    Button(onClick = { scope.launch { goNext() } }, enabled = !isLoading) {
                        Text("다음 문장")
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = "다음")
                    }
                }

                if (isLoading && history.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }

                errorMsg?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("오늘의 회상정보를 모두 보셨습니다.", color = Color.Gray, fontSize = 12.sp)
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
    getPatientIdFromPrefs(context)?.let { return it }
    Firebase.auth.currentUser?.uid?.let { return it }
    return ""
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
        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        delay(200)
        toneGen.release()

        stopTTS()

        currentTTSPlayer = MediaPlayer().apply {
            setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
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

























