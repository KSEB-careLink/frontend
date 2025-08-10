// app/src/main/java/com/example/myapplication/screens/PatientQuiz.kt
package com.example.myapplication.screens

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.example.myapplication.data.DatasetItem
import com.example.myapplication.viewmodel.QuizViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ────────────────────────────────────────────────
// 인증 보장 유틸
// ────────────────────────────────────────────────
private suspend fun ensureFirebaseLogin(): Boolean = withContext(Dispatchers.IO) {
    val user = Firebase.auth.currentUser ?: return@withContext false
    return@withContext try {
        // 자주 갱신이 부담이면 false로 두고 만료 시에만 새로고침 전략 고려
        user.getIdToken(true).await()
        true
    } catch (_: Exception) { false }
}

// ────────────────────────────────────────────────
// 메인 컴포저블
// ────────────────────────────────────────────────
@Composable
fun Patient_Quiz(
    navController: NavController,
    patientId: String,
    quizViewModel: QuizViewModel = viewModel()
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()

    // 네비게이션 param이 "{patientId}" 같은 플레이스홀더일 수 있어 복구
    val activePatientId by remember(patientId) {
        mutableStateOf(resolvePatientId(context, patientId))
    }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // 퀴즈 데이터 로드: 메모리/업로드 힌트를 우선 전달 + voiceId 사전 체크
    LaunchedEffect(activePatientId) {
        if (activePatientId.isBlank()) {
            Toast.makeText(context, "환자 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }

        // 0) Firestore 접근 전에 FirebaseAuth 로그인 보장(커스텀 토큰 로그인 끝났는지)
        val authed = ensureFirebaseLogin()
        if (!authed) {
            Toast.makeText(context, "인증 상태를 확인해 주세요.", Toast.LENGTH_LONG).show()
            return@LaunchedEffect
        }

        // 1) 환자 문서에서 voiceId 조회 (미러링 전략 전제)
        val voiceId = fetchVoiceIdFromPatient(activePatientId)
        if (voiceId.isNullOrBlank()) {
            // 규칙/데이터 정비 전까진 보호자 문서 조회를 시도하지 않고 UX로 유도
            Toast.makeText(
                context,
                "보호자 음성이 환자 문서에 등록되어 있지 않습니다.\n보호자 앱에서 음성을 등록해 주세요.",
                Toast.LENGTH_LONG
            ).show()
            // 필요 시: 다이얼로그/네비게이션 유도 (예: navController.navigate("guardianVoiceEnroll"))
            return@LaunchedEffect
        }

        // ▶ 여기서만 퀴즈 생성 API 호출
        val memImageUrl = prefs.getString("last_memory_image_url", null)
        val memDesc     = prefs.getString("last_memory_sentence", null)
        val photoId     = prefs.getString("last_photo_id", null)
        val imageUrl    = prefs.getString("last_image_url", null)
        val desc        = prefs.getString("last_description", null)

        Log.d(
            "PatientQuiz",
            "hints | memImageUrl=$memImageUrl, memDesc=$memDesc, photoId=$photoId, imageUrl=$imageUrl, desc=$desc"
        )

        if (memImageUrl == null && memDesc == null && photoId == null && imageUrl == null && desc == null) {
            Toast.makeText(context, "힌트 없음 → 서버 자동 보완 시도", Toast.LENGTH_SHORT).show()
        }

        quizViewModel.loadQuizzes(
            patientId = activePatientId,
            photoId = photoId,
            imageUrl = memImageUrl ?: imageUrl,
            description = memDesc ?: desc
        )
    }

    val items by quizViewModel.items.collectAsState()
    val error by quizViewModel.error.collectAsState()

    var currentIndex by remember { mutableStateOf(0) }

    // 아이템 로드 성공 시: 힌트 키 정리(중복 재사용 방지) + ✅ 인덱스 리셋
    LaunchedEffect(items) {
        if (items.isNotEmpty()) {
            prefs.edit()
                .remove("last_photo_id")
                .remove("last_image_url")
                .remove("last_description")
                .remove("last_memory_image_url")
                .remove("last_memory_sentence")
                .apply()
            currentIndex = 0
        }
    }

    LaunchedEffect(error) {
        error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    // 서버에서 받은 ttsAudioUrl을 사용해 바로 재생
    LaunchedEffect(items, currentIndex) {
        if (items.isNotEmpty()) {
            scope.launch {
                playTTS(items[currentIndex].ttsAudioUrl, context)
            }
        }
    }

    // 화면 떠날 때 플레이어 정리
    DisposableEffect(Unit) {
        onDispose { stopTTS() }
    }

    Scaffold(bottomBar = { QuizBottomBar(navController, activePatientId) }) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            if (items.isEmpty()) {
                // ✅ 에러가 있으면 로딩 대신 에러 UI
                if (error != null) {
                    Text(
                        "문제를 불러오지 못했습니다.\n$error",
                        fontSize = 16.sp,
                        color = Color(0xFFE2101A)
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = {
                        // 필요 시 재시도 로직 (마지막 힌트로 다시 호출)
                        val memImageUrl = prefs.getString("last_memory_image_url", null)
                        val memDesc     = prefs.getString("last_memory_sentence", null)
                        val photoId     = prefs.getString("last_photo_id", null)
                        val imageUrl    = prefs.getString("last_image_url", null)
                        val desc        = prefs.getString("last_description", null)

                        scope.launch {
                            quizViewModel.loadQuizzes(
                                patientId = activePatientId,
                                photoId = photoId,
                                imageUrl = memImageUrl ?: imageUrl,
                                description = memDesc ?: desc
                            )
                        }
                    }) { Text("다시 시도") }
                } else {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(16.dp))
                    Text("로딩 중…", fontSize = 16.sp)
                }
            } else {
                QuizContent(
                    patientId = activePatientId,
                    item = items[currentIndex],
                    client = client,
                    onNext = {
                        stopTTS()
                        if (currentIndex < items.size - 1) {
                            currentIndex++
                        } else {
                            Toast.makeText(context, "문제를 모두 풀었습니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

// ────────────────────────────────────────────────
// 하단 탭
// ────────────────────────────────────────────────
@Composable
private fun QuizBottomBar(navController: NavController, patientId: String) {
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    data class Tab(val pattern: String, val actual: String, val label: String)

    val tabs = listOf(
        Tab(pattern = "sentence/{patientId}", actual = "sentence/$patientId", label = "회상문장"),
        Tab(pattern = "quiz/{patientId}",     actual = "quiz/$patientId",     label = "회상퀴즈"),
        Tab(pattern = "alert",                 actual = "alert",               label = "긴급알림")
    )

    val navColors = NavigationBarItemDefaults.colors(
        indicatorColor = Color.Transparent,
        selectedIconColor = Color(0xFF00C4B4),
        unselectedIconColor = Color(0xFF888888),
        selectedTextColor = Color(0xFF00C4B4),
        unselectedTextColor = Color(0xFF888888)
    )

    NavigationBar {
        tabs.forEach { tab ->
            val selected = currentRoute == tab.pattern || currentRoute == tab.actual
            NavigationBarItem(
                icon = { Icon(Icons.Default.Timer, contentDescription = tab.label) },
                label = { Text(tab.label) },
                selected = selected,
                onClick = {
                    if (!selected) {
                        navController.navigate(tab.actual) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                colors = navColors
            )
        }
    }
}

// ────────────────────────────────────────────────
@Composable
private fun QuizContent(
    patientId: String,
    item: DatasetItem,
    client: OkHttpClient,
    onNext: () -> Unit
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var isCorrect by remember { mutableStateOf<Boolean?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val startTime = remember(item.id) { System.currentTimeMillis() }
    LaunchedEffect(item.id) {
        selected = null
        showResult = false
        isCorrect = null
        elapsedTime = 0L
    }

    LaunchedEffect(showResult) {
        if (!showResult) {
            val begin = startTime
            while (!showResult) {
                delay(1000)
                elapsedTime = (System.currentTimeMillis() - begin) / 1000
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!showResult) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(String.format("%02d:%02d", elapsedTime / 60, elapsedTime % 60), fontSize = 20.sp)
            }
            Spacer(Modifier.height(16.dp))

            Text(item.questionText, fontSize = 28.sp, color = Color(0xFF00C4B4))
            Spacer(Modifier.height(16.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                item.options.chunked(2).forEachIndexed { row, opts ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                        opts.forEachIndexed { col, opt ->
                            val idx = row * 2 + col
                            OptionButton(text = opt, modifier = Modifier.weight(1f)) {
                                selected = idx
                                scope.launch {
                                    try {
                                        val token = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
                                        if (token.isNullOrBlank()) {
                                            Toast.makeText(context, "인증 토큰 없음", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }
                                        val body = JSONObject().apply {
                                            put("patient_id", patientId)
                                            put("quiz_id", item.id)           // Int 그대로 전송
                                            put("selected_index", idx)        // 0-based
                                            put("response_time_sec", (System.currentTimeMillis() - startTime) / 1000)
                                        }.toString()
                                        val req = Request.Builder()
                                            .url("${BuildConfig.BASE_URL.trimEnd('/')}/quiz-responses")
                                            .addHeader("Authorization", "Bearer $token")
                                            .post(body.toRequestBody("application/json".toMediaType()))
                                            .build()
                                        val resp = withContext(Dispatchers.IO) { client.newCall(req).execute() }
                                        val ok = resp.isSuccessful
                                        val resBody = resp.body?.string().orEmpty()
                                        resp.close()
                                        if (!ok) throw Exception("서버 오류: HTTP ${resp.code}")
                                        val obj = JSONObject(resBody)
                                        isCorrect = obj.optBoolean("is_correct", false)
                                        showResult = true
                                        playAck()
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "제출 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.height(100.dp))
            val correct = isCorrect == true

            Text(
                if (correct) "정답이에요!" else "오답이에요!",
                fontSize = 32.sp,
                color = if (correct) Color(0xFF00A651) else Color(0xFFE2101A)
            )
            Spacer(Modifier.height(16.dp))
            Text("풀이 시간: ${elapsedTime}초", fontSize = 18.sp)
            Spacer(Modifier.height(16.dp))

            Image(
                painter = painterResource(if (correct) R.drawable.ch else R.drawable.wr),
                contentDescription = null,
                modifier = Modifier.size(300.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (correct) {
                        stopTTS()
                        onNext()
                    } else {
                        selected = null
                        isCorrect = null
                        showResult = false
                        stopTTS()
                        // 다시 같은 문제 읽어주기
                        scope.launch { playTTS(item.ttsAudioUrl, context) }
                    }
                },
                modifier = Modifier
                    .width(130.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text(if (correct) "다음 문제로" else "다시 풀기", color = Color.White)
            }
        }
    }
}

@Composable
private fun OptionButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = modifier.height(56.dp), shape = RoundedCornerShape(8.dp)) {
        Text(text, color = Color.White)
    }
}

// ────────────────────────────────────────────────
// 공통 TTS 유틸 (안정화 버전)
// ────────────────────────────────────────────────
private var currentTTSPlayer: MediaPlayer? = null

private fun stopTTS() {
    try {
        currentTTSPlayer?.apply {
            setOnPreparedListener(null)
            setOnCompletionListener(null)
            stop()
            reset()
            release()
        }
    } catch (_: Exception) { }
    currentTTSPlayer = null
}

private suspend fun playTTS(
    ttsUrl: String,
    context: Context
) {
    if (ttsUrl.isBlank()) return
    try {
        playAck()
        delay(200)
        stopTTS()
        currentTTSPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            setDataSource(context, Uri.parse(ttsUrl))
            setOnPreparedListener { it.start() }
            setOnErrorListener { _, what, extra ->
                Toast.makeText(context, "TTS 오류($what/$extra)", Toast.LENGTH_SHORT).show()
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

private fun playAck() {
    try {
        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        toneGen.release()
    } catch (_: Exception) { }
}

// ────────────────────────────────────────────────
// patientId 복구/조회 유틸
// ────────────────────────────────────────────────
private fun getPatientIdFromPrefs(context: Context): String? {
    val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
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

/**
 * Firestore: patients/{patientId}.voiceId 읽기
 */
private suspend fun fetchVoiceIdFromPatient(patientId: String): String? = withContext(Dispatchers.IO) {
    try {
        val snap = Firebase.firestore.collection("patients").document(patientId).get().await()
        snap.getString("voiceId")?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        Log.e("PatientQuiz", "patients voiceId 조회 실패: ${e.message}", e)
        null
    }
}





























