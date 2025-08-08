package com.example.myapplication.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
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
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun Patient_Sentence(
    navController: NavController,
    patientId: String,
    voiceId: String // 현재 화면에선 미사용(백엔드에서 관리)
) {
    // ─── 1) 위치 권한 + 서비스 시작 (유지) ──────────────────────────────────────
    val context = LocalContext.current
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

    // ─── 2) 네트워킹 공용 클라이언트 ───────────────────────────────────────────
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    val scope = rememberCoroutineScope()

    // ─── 3) 데이터 모델 ────────────────────────────────────────────────────────
    data class MemoryOne(
        val sentence: String,
        val imageUrl: String,
        val mp3Url: String
    )

    // 히스토리 + 현재 인덱스
    val history = remember { mutableStateListOf<MemoryOne>() }
    var currentIndex by remember { mutableStateOf(-1) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // ─── 4) 랜덤 1개 호출 (문서 스펙 준수: /memories/{patientId}/tts) ──────────
    suspend fun fetchOneRandom(): MemoryOne {
        val idToken = Firebase.auth.currentUser
            ?.getIdToken(false)?.await()?.token
            ?: throw Exception("인증 토큰 없음")

        val url = "${BuildConfig.BASE_URL.trimEnd('/')}/memories/$patientId/tts"
        val resp = withContext(Dispatchers.IO) {
            client.newCall(
                Request.Builder()
                    .url(url)
                    .addHeader("Authorization", "Bearer $idToken")
                    .get()
                    .build()
            ).execute()
        }

        if (resp.code == 204) throw Exception("아직 생성된 회상 문장이 없습니다.")
        if (!resp.isSuccessful) throw Exception("API 오류 ${resp.code}")

        val obj = JSONObject(resp.body?.string().orEmpty())
        val sentence = obj.optString("sentence", obj.optString("reminder_text"))
        val image = obj.optString("image_url")
        val mp3 = obj.optString("mp3_url", obj.optString("tts_url"))
        if (sentence.isBlank() || image.isBlank() || mp3.isBlank()) {
            throw Exception("응답 필드 부족 (sentence/image_url/mp3_url)")
        }
        return MemoryOne(sentence, image, mp3)
    }

    suspend fun loadInitial() {
        isLoading = true
        errorMsg = null
        try {
            val first = fetchOneRandom()
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

    LaunchedEffect(patientId) {
        loadInitial()
    }

    // ─── 5) 다음/이전 이동 로직 ────────────────────────────────────────────────
    fun canGoPrev() = currentIndex > 0
    fun canGoNext() = true // 다음은 항상 가능(끝이면 새 랜덤 fetch)

    suspend fun goPrev() {
        stopTTS()
        if (canGoPrev()) {
            currentIndex -= 1
        } else {
            // 맨 앞이면 아무것도 하지 않음(버튼 자체를 disabled로 처리)
        }
    }

    suspend fun goNext() {
        stopTTS()
        // 히스토리에 다음 항목이 있으면 인덱스만 이동
        if (currentIndex + 1 < history.size) {
            currentIndex += 1
            return
        }
        // 없으면 새로 랜덤 Fetch → 히스토리에 추가 → 이동
        isLoading = true
        errorMsg = null
        try {
            val one = fetchOneRandom()
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

    // ─── 6) UI ────────────────────────────────────────────────────────────────
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
                        icon = { Icon(Icons.Default.VolumeUp, contentDescription = label) },
                        label = { Text(label) },
                        selected = selected,
                        onClick = {
                            val target = when (routePattern) {
                                "sentence/{patientId}" -> "sentence/$patientId"
                                "quiz/{patientId}" -> "quiz/$patientId"
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

                    // 이미지
                    AsyncImage(
                        model = mem.imageUrl,
                        contentDescription = "Memory Photo",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
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

    // ─── 7) MediaPlayer 정리 ───────────────────────────────────────────────────
    DisposableEffect(Unit) {
        onDispose {
            stopTTS()
        }
    }
}

// ────────────────────────────────────────────────
// TTS helpers
// ────────────────────────────────────────────────

private val ttsClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

private var currentTTSPlayer: MediaPlayer? = null

private fun stopTTS() {
    currentTTSPlayer?.run {
        try { stop() } catch (_: Exception) {}
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
            setDataSource(ttsUrl) // 문서 스펙: mp3_url or tts_url
            setOnPreparedListener { it.start() }
            prepareAsync()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "TTS 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}













