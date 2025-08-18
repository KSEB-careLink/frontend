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
import androidx.compose.material.icons.filled.Star
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
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
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
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// ────────────────────────────────────────────────
// 인증 보장 유틸
// ────────────────────────────────────────────────
private suspend fun ensureFirebaseLogin(): Boolean = withContext(Dispatchers.IO) {
    val user = Firebase.auth.currentUser ?: return@withContext false
    return@withContext try {
        user.getIdToken(false).await()
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

    // OkHttpClient는 재구성(recomposition)마다 만들지 말고 고정
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .protocols(listOf(Protocol.HTTP_1_1))
            .build()
    }

    // 퀴즈 데이터 로드
    LaunchedEffect(activePatientId) {
        if (activePatientId.isBlank()) {
            Toast.makeText(context, "환자 정보가 없습니다.", Toast.LENGTH_SHORT).show()
            return@LaunchedEffect
        }

        val authed = ensureFirebaseLogin()
        if (!authed) {
            Toast.makeText(context, "인증 상태를 확인해 주세요.", Toast.LENGTH_LONG).show()
            return@LaunchedEffect
        }

        // 1) 환자 문서에서 voiceId 조회
        var voiceId = fetchVoiceIdFromPatient(activePatientId)

        // 2) 없으면 guardians/{uid}.voiceId 폴백
        if (voiceId.isNullOrBlank()) {
            val guardianUid = fetchGuardianUidForPatient(activePatientId)
                ?: prefs.getString("guardian_id", null)
            if (!guardianUid.isNullOrBlank()) {
                voiceId = fetchVoiceIdFromGuardian(guardianUid)
            }
        }

        if (voiceId.isNullOrBlank()) {
            Toast.makeText(
                context,
                "보호자 음성이 등록되어 있지 않습니다.\n보호자 앱에서 음성을 등록해 주세요.",
                Toast.LENGTH_LONG
            ).show()
            return@LaunchedEffect
        }

        // 힌트 수집
        var seedImageUrl = prefs.getString("last_memory_image_url", null)
        var seedDesc = prefs.getString("last_memory_sentence", null)
        var seedPhotoId = prefs.getString("last_photo_id", null)
        val imageUrl = prefs.getString("last_image_url", null)
        val desc = prefs.getString("last_description", null)
        val seedCategory = prefs.getString("last_category", null)

        if (seedImageUrl == null) seedImageUrl = imageUrl
        if (seedDesc == null) seedDesc = desc

        Log.d(
            "PatientQuiz",
            "hints | memImageUrl=$seedImageUrl, memDesc=$seedDesc, photoId=$seedPhotoId, imageUrl=$imageUrl, desc=$desc"
        )

        // 폴백: 최근 업로드 자동 시드
        if (seedImageUrl == null && seedDesc == null && seedPhotoId == null) {
            try {
                val token = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
                if (!token.isNullOrBlank()) {
                    val base = BuildConfig.BASE_URL.trimEnd('/')

                    val urls = listOf(
                        "$base/photos/patient/${Uri.encode(activePatientId)}/latest",
                        "$base/photos/patient/${Uri.encode(activePatientId)}?limit=1&order=desc",
                        "$base/photos?patient_id=${java.net.URLEncoder.encode(activePatientId, "UTF-8")}&limit=1&order=desc"
                    )

                    fun extract(o: JSONObject) {
                        val img = o.optString(
                            "image_url",
                            o.optString(
                                "imageUrl",
                                o.optString(
                                    "mediaUrl",
                                    o.optString(
                                        "photo_url",
                                        o.optString("photoUrl", o.optString("url", ""))
                                    )
                                )
                            )
                        ).ifBlank { null }

                        val desc0 = o.optString("description", null)
                        val pid = o.optLong("photo_id", o.optLong("id", -1L))
                        if (!img.isNullOrBlank()) seedImageUrl = img
                        if (!desc0.isNullOrBlank()) seedDesc = desc0
                        if (pid > 0) seedPhotoId = pid.toString()
                    }

                    loop@ for (url in urls) {
                        val (ok, bodyStr) = withContext(Dispatchers.IO) {
                            val req = Request.Builder()
                                .url(url)
                                .addHeader("Authorization", "Bearer $token")
                                .get()
                                .build()
                            client.newCall(req).execute().use { r ->
                                (r.isSuccessful) to (r.body?.string().orEmpty())
                            }
                        }
                        if (!ok || bodyStr.isBlank()) continue

                        var seeded = false
                        runCatching {
                            val o = JSONObject(bodyStr)
                            extract(o)
                            seeded = (seedImageUrl != null || seedDesc != null || seedPhotoId != null)
                        }
                        if (!seeded) runCatching {
                            val arr = JSONArray(bodyStr)
                            if (arr.length() > 0) {
                                extract(arr.getJSONObject(0))
                                seeded = (seedImageUrl != null || seedDesc != null || seedPhotoId != null)
                            }
                        }

                        if (seeded) {
                            prefs.edit().apply {
                                seedPhotoId?.let { putString("last_photo_id", it) }
                                seedImageUrl?.let {
                                    putString("last_image_url", it)
                                    putString("last_memory_image_url", it)
                                }
                                seedDesc?.let {
                                    putString("last_description", it)
                                    putString("last_memory_sentence", it)
                                }
                                apply()
                            }
                            Toast.makeText(context, "최근 업로드로 시작합니다.", Toast.LENGTH_SHORT).show()
                            break@loop
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PatientQuiz", "fallback 시드 조회 에러: ${e.message}", e)
            }
        }

        if (seedImageUrl == null && seedDesc == null && seedPhotoId == null) {
            Toast.makeText(context, "회상 항목이 없습니다. 먼저 사진/설명을 등록해 주세요.", Toast.LENGTH_LONG).show()
            return@LaunchedEffect
        }

        // ▶ 퀴즈 생성 API 호출
        quizViewModel.loadQuizzes(
            patientId = activePatientId,
            photoId = seedPhotoId,
            imageUrl = seedImageUrl,
            description = seedDesc,
            category = seedCategory
        )
    }

    val items by quizViewModel.items.collectAsState()
    val error by quizViewModel.error.collectAsState()

    var currentIndex by remember { mutableStateOf(0) }

    // 아이템 로드 성공 시: 힌트 키 정리 + 인덱스 리셋
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
    val scopeForTts = rememberCoroutineScope()
    LaunchedEffect(items, currentIndex) {
        if (items.isNotEmpty()) {
            scopeForTts.launch { playTTS(items[currentIndex].ttsAudioUrl, context) }
        }
    }

    // 화면 떠날 때 플레이어 정리
    DisposableEffect(Unit) {
        onDispose { stopTTS() }
    }

    Scaffold(bottomBar = { QuizBottomBar(navController, activePatientId) }) { innerPadding ->
        // 중앙 정렬 + '조금 아래'로 이동
        val scroll = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.weight(1.2f))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scroll),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(8.dp))

                if (items.isEmpty()) {
                    if (error != null) {
                        Text(
                            "문제를 불러오지 못했습니다.\n$error",
                            fontSize = 16.sp,
                            color = Color(0xFFE2101A)
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            stopTTS()
                            val memImageUrl = prefs.getString("last_memory_image_url", null)
                            val memDesc = prefs.getString("last_memory_sentence", null)
                            val photoId = prefs.getString("last_photo_id", null)
                            val imageUrl = prefs.getString("last_image_url", null)
                            val desc = prefs.getString("last_description", null)

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
                    val fallbackPhotoUrl = prefs.getString("last_memory_image_url", null)
                        ?: prefs.getString("last_image_url", null)
                    val uiPhotoUrl =
                        items[currentIndex].imageUrl?.takeIf { it.isNotBlank() } ?: fallbackPhotoUrl

                    QuizContent(
                        patientId = activePatientId,
                        item = items[currentIndex],
                        client = client,
                        hasPrev = currentIndex > 0,
                        hasNext = currentIndex < items.size - 1,
                        onPrev = {
                            stopTTS()
                            if (currentIndex > 0) currentIndex--
                            else Toast.makeText(context, "첫 문제입니다.", Toast.LENGTH_SHORT).show()
                        },
                        onNext = {
                            stopTTS()
                            if (currentIndex < items.size - 1) currentIndex++
                            else Toast.makeText(context, "문제를 모두 풀었습니다.", Toast.LENGTH_SHORT).show()
                        },
                        photoUrl = uiPhotoUrl
                    )
                }

                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.weight(0.8f))
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
                icon = { Icon(Icons.Default.Star, contentDescription = tab.label) },
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
    hasPrev: Boolean,
    hasNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    photoUrl: String? = null
) {
    var selected by remember { mutableStateOf<Int?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var isCorrect by remember { mutableStateOf<Boolean?>(null) }
    var submitting by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // ⏱️ 재도전 시 타이머도 리셋되도록 attempt 키 도입
    var attempt by remember(item.id) { mutableStateOf(0) }
    val startTime = remember(item.id, attempt) { System.currentTimeMillis() }

    LaunchedEffect(item.id) {
        selected = null
        showResult = false
        isCorrect = null
        elapsedTime = 0L
        submitting = false
        attempt = 0
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
        // 결과 화면에서는 사진 숨김
        if (!showResult && !photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = "회상 사진",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(16.dp))
        }

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
                            OptionButton(
                                text = opt,
                                modifier = Modifier.weight(1f),
                                enabled = !submitting && !showResult
                            ) {
                                if (submitting) return@OptionButton
                                submitting = true
                                selected = idx
                                val start = startTime
                                scope.launch {
                                    try {
                                        val token = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
                                        if (token.isNullOrBlank()) {
                                            Toast.makeText(context, "인증 토큰 없음", Toast.LENGTH_SHORT).show()
                                            return@launch
                                        }

                                        val elapsedSec = ((System.currentTimeMillis() - start) / 1000).coerceAtLeast(0)
                                        val selectedText = item.options.getOrNull(idx) ?: ""

                                        val form = FormBody.Builder(Charsets.UTF_8)
                                            .add("patient_id", patientId)
                                            .add("quiz_id", item.id.toString())
                                            .add("question_id", item.id.toString())
                                            .add("selected_index", idx.toString())
                                            .add("selectedIndex", idx.toString())
                                            .add("selected_index_1based", (idx + 1).toString())
                                            .add("answer_index", idx.toString())
                                            .add("choice_index", idx.toString())
                                            .add("selected_option", selectedText)
                                            .add("options_json", JSONArray(item.options).toString())
                                            .add("response_time_sec", elapsedSec.toString())
                                            .build()

                                        suspend fun postOnce(url: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
                                            val req = Request.Builder()
                                                .url(url)
                                                .addHeader("Authorization", "Bearer $token")
                                                .post(form)
                                                .build()
                                            client.newCall(req).execute().use { r ->
                                                (r.isSuccessful) to (r.body?.string().orEmpty())
                                            }
                                        }

                                        val base = BuildConfig.BASE_URL.trimEnd('/')
                                        var (ok, resBody) = postOnce("$base/quiz-responses")
                                        if (!ok) {
                                            val second = postOnce("$base/quizResponses")
                                            ok = second.first
                                            if (ok) resBody = second.second
                                        }
                                        if (!ok) throw Exception("서버 오류: $resBody")

                                        fun pickBool(o: JSONObject?, vararg keys: String): Boolean? {
                                            if (o == null) return null
                                            for (k in keys) if (o.has(k)) {
                                                val v = o.opt(k)
                                                return when (v) {
                                                    is Boolean -> v
                                                    is Number  -> v.toInt() != 0
                                                    is String  -> v.equals("true", true) || v == "1"
                                                    else -> null
                                                }
                                            }
                                            return null
                                        }
                                        fun pickInt(o: JSONObject?, vararg keys: String): Int? {
                                            if (o == null) return null
                                            for (k in keys) if (o.has(k)) {
                                                val v = o.opt(k)
                                                when (v) {
                                                    is Number -> return v.toInt()
                                                    is String -> v.toIntOrNull()?.let { return it }
                                                }
                                            }
                                            return null
                                        }
                                        fun pickString(o: JSONObject?, vararg keys: String): String? {
                                            if (o == null) return null
                                            for (k in keys) if (o.has(k)) {
                                                val s = o.optString(k, "").trim()
                                                if (s.isNotBlank()) return s
                                            }
                                            return null
                                        }
                                        fun asJsonOrNull(s: String): JSONObject? =
                                            runCatching { JSONObject(s) }.getOrNull()

                                        val trimmed = resBody.trim()
                                        if (trimmed.equals("true", true)) {
                                            isCorrect = true
                                        } else if (trimmed.equals("false", true)) {
                                            isCorrect = false
                                        } else {
                                            val root = asJsonOrNull(resBody)
                                            val data = root?.optJSONObject("data")
                                            val result = root?.optJSONObject("result")

                                            var correct: Boolean? = pickBool(root, "is_correct", "correct")
                                                ?: pickBool(data, "is_correct", "correct")
                                                ?: pickBool(result, "is_correct", "correct")

                                            if (correct == null) {
                                                val corrIdx = pickInt(root, "correct_index", "answer_index", "correctIndex", "answerIndex")
                                                    ?: pickInt(data, "correct_index", "answer_index", "correctIndex", "answerIndex")
                                                    ?: pickInt(result, "correct_index", "answer_index", "correctIndex", "answerIndex")

                                                val corrOpt = pickString(root, "correct_option", "answer_option", "correctOption", "answerOption")
                                                    ?: pickString(data, "correct_option", "answer_option", "correctOption", "answerOption")
                                                    ?: pickString(result, "correct_option", "answer_option", "correctOption", "answerOption")

                                                correct = when {
                                                    corrIdx != null -> (corrIdx == idx || corrIdx == idx + 1)
                                                    corrOpt != null -> (corrOpt == selectedText)
                                                    else -> null
                                                }
                                            }

                                            isCorrect = correct ?: false
                                        }

                                        showResult = true
                                        playAck()

                                        Log.d(
                                            "QuizSubmit",
                                            "quizId=${item.id}, selected=$idx($selectedText) -> isCorrect=$isCorrect | body=$resBody"
                                        )
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "제출 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        submitting = false
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
        }

        Spacer(Modifier.height(20.dp))

        // ⏮️ ⏭️ 이전/다음/다시풀기 네비게이션
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = {
                    stopTTS()
                    onPrev()
                },
                enabled = hasPrev
            ) { Text("이전 문제") }

            // ✅ 두 번째 버튼: 정답이면 '다음 문제', 오답이면 '다시 풀기'
            val isAnswerShown = showResult && isCorrect != null
            val isRight = isCorrect == true

            Button(
                onClick = {
                    stopTTS()
                    if (isAnswerShown && isRight) {
                        onNext()
                    } else if (isAnswerShown && !isRight) {
                        // 다시 풀기: 상태 리셋 + 타이머/음성 재시작
                        selected = null
                        showResult = false
                        isCorrect = null
                        elapsedTime = 0L
                        submitting = false
                        attempt += 1           // ⏱️ 타이머 기준점 갱신
                        // 문제 읽기 다시 재생
                        scope.launch { playTTS(item.ttsAudioUrl, context) }
                    }
                },
                enabled = when {
                    // 정답 → 다음 문제 (마지막이면 비활성화)
                    isAnswerShown && isRight -> hasNext
                    // 오답 → 언제나 재도전 가능
                    isAnswerShown && !isRight -> true
                    else -> false
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) { Text(if (isAnswerShown && !isRight) "다시 풀기" else "다음 문제", color = Color.White) }
        }
    }
}

@Composable
private fun OptionButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
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
// patientId & voiceId/guardianUid 조회 유틸
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

/** Firestore: patients/{patientId}.voiceId 읽기 */
private suspend fun fetchVoiceIdFromPatient(patientId: String): String? = withContext(Dispatchers.IO) {
    try {
        val snap = Firebase.firestore.collection("patients").document(patientId).get().await()
        snap.getString("voiceId")?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        Log.e("PatientQuiz", "patients voiceId 조회 실패: ${e.message}", e)
        null
    }
}

/** Firestore: patients/{patientId}.linkedGuardian 읽기 */
private suspend fun fetchGuardianUidForPatient(patientId: String): String? = withContext(Dispatchers.IO) {
    try {
        val snap = Firebase.firestore.collection("patients").document(patientId).get().await()
        snap.getString("linkedGuardian")?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        Log.e("PatientQuiz", "linkedGuardian 조회 실패: ${e.message}", e)
        null
    }
}

/** Firestore: guardians/{guardianUid}.voiceId 읽기 */
private suspend fun fetchVoiceIdFromGuardian(guardianUid: String): String? = withContext(Dispatchers.IO) {
    try {
        val snap = Firebase.firestore.collection("guardians").document(guardianUid).get().await()
        snap.getString("voiceId")?.takeIf { it.isNotBlank() }
    } catch (e: Exception) {
        Log.e("PatientQuiz", "guardians voiceId 조회 실패: ${e.message}", e)
        null
    }
}



































