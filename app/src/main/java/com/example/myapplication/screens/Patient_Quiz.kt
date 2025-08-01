package com.example.myapplication.screens

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.example.myapplication.data.DatasetItem
import com.example.myapplication.viewmodel.QuizViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import org.json.JSONObject

@Composable
fun Patient_Quiz(
    navController: NavController,
    patientId: String,
    voiceId: String,
    quizViewModel: QuizViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(patientId) {
        quizViewModel.loadQuizzes(patientId)
    }

    val items by quizViewModel.items.collectAsState()
    val error by quizViewModel.error.collectAsState()

    LaunchedEffect(error) {
        error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    var currentIndex by remember { mutableStateOf(0) }

    LaunchedEffect(items, currentIndex, voiceId) {
        if (items.isNotEmpty()) {
            scope.launch { playTTS(context, voiceId, items[currentIndex].questionText) }
        }
    }

    Scaffold(bottomBar = { QuizBottomBar(navController) }) { innerPadding ->
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
                CircularProgressIndicator()
                Spacer(Modifier.height(16.dp))
                Text("로딩 중…", fontSize = 16.sp)
            } else {
                QuizContent(item = items[currentIndex]) {
                    if (currentIndex < items.size - 1) currentIndex++
                }
            }
        }
    }
}

@Composable
private fun QuizBottomBar(navController: NavController) {
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
        listOf("sentence/{patientId}" to "회상문장", "quiz/{patientId}" to "회상퀴즈", "alert" to "긴급알림").forEach { (route, label) ->
            NavigationBarItem(
                icon = { Icon(Icons.Default.Timer, contentDescription = label) },
                label = { Text(label) },
                selected = currentRoute == route,
                onClick = {
                    if (currentRoute != route) {
                        navController.navigate(route) {
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

@Composable
private fun QuizContent(item: DatasetItem, onNext: () -> Unit) {
    var selected by remember { mutableStateOf<Int?>(null) }
    var showResult by remember { mutableStateOf(false) }
    var elapsedTime by remember { mutableStateOf(0L) }
    var questionTime by remember { mutableStateOf<Long?>(null) }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }
    val url = "${BuildConfig.BASE_URL}/quiz_logs"

    LaunchedEffect(item.id) {
        selected = null
        showResult = false
    }

    LaunchedEffect(showResult) {
        if (!showResult) {
            elapsedTime = 0L
            val start = System.currentTimeMillis()
            while (!showResult) {
                delay(1000)
                elapsedTime = (System.currentTimeMillis() - start) / 1000
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
                                questionTime = elapsedTime
                                showResult = true
                            }
                        }
                    }
                }
            }
        } else {
            Spacer(Modifier.height(100.dp))
            val correctIdx = item.answerIndex - 1
            val isCorrect = selected == correctIdx

            Text(
                if (isCorrect) "정답이에요!" else "오답이에요!",
                fontSize = 32.sp,
                color = if (isCorrect) Color(0xFF00A651) else Color(0xFFE2101A)
            )
            Spacer(Modifier.height(16.dp))

            questionTime?.let {
                Text("풀이 시간: ${it}초", fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
            }

            Image(
                painter = painterResource(if (isCorrect) R.drawable.ch else R.drawable.wr),
                contentDescription = null,
                modifier = Modifier.size(300.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (isCorrect) "정말 잘 기억하셨어요😊" else "다시 기억해볼까요?",
                fontSize = 20.sp
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isCorrect) {
                        scope.launch {
                            val token = Firebase.auth.currentUser?.getIdToken(false)?.await()?.token
                            if (token.isNullOrBlank()) {
                                Toast.makeText(context, "인증 토큰 없음", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val body = JSONObject().apply {
                                put("quiz_id", item.id)
                                put("selected_index", selected)
                                put("response_time_sec", questionTime)
                            }.toString()
                            val req = Request.Builder()
                                .url(url)
                                .addHeader("Authorization", "Bearer $token")
                                .post(body.toRequestBody("application/json".toMediaType()))
                                .build()
                            withContext(Dispatchers.IO) { client.newCall(req).execute().close() }
                            onNext()
                        }
                    } else {
                        selected = null
                        showResult = false
                    }
                },
                modifier = Modifier
                    .width(130.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text(if (isCorrect) "다음 문제로" else "다시 풀기", color = Color.White)
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

// ─────────────────────────────────────────────
// TTS 관련 전역 변수 및 함수 sentence랑 공통
// ─────────────────────────────────────────────

private val ttsClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

private var currentTTSPlayer: MediaPlayer? = null

private suspend fun playTTS(context: Context, voiceId: String, text: String) {
    try {
        val token = Firebase.auth.currentUser?.getIdToken(true)?.await()?.token
        val encoded = URLEncoder.encode(text, "UTF-8")
        val url = "${BuildConfig.BASE_URL.trimEnd('/')}/tts?voice_id=$voiceId&text=$encoded"
        val rb = Request.Builder().url(url)
        if (!token.isNullOrBlank()) rb.addHeader("Authorization", "Bearer $token")
        val resp = withContext(Dispatchers.IO) { ttsClient.newCall(rb.get().build()).execute() }
        if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
        val mp3Url = resp.body?.string().orEmpty()

        withContext(Dispatchers.Main) {
            // 알림 소리 삽입 (약 200ms)
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK)
            delay(200)
            toneGen.release()

            // 기존 MediaPlayer 해제
            currentTTSPlayer?.apply {
                reset()
                release()
            }

            // 새로운 MediaPlayer 생성
            currentTTSPlayer = MediaPlayer().apply {
                setDataSource(mp3Url)
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "TTS 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

















