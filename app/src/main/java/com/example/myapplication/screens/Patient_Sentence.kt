package com.example.myapplication.screens

import android.content.Context
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.*               // ← 여기에 LaunchedEffect, rememberCoroutineScope, etc 포함
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil.compose.AsyncImage
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@Composable
fun Patient_Sentence(
    navController: NavController,
    patientId: String,
    voiceId: String
) {
    // layout constants
    val padding        = 24.dp
    val speakerTopGap  = 16.dp
    val imageGap       = 16.dp
    val imageHeight    = 200.dp
    val imageCorner    = 12.dp
    val btnGap         = 12.dp
    val btnHeightRatio = 4

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()

    // HTTP client
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // model
    data class MemoryItem(
        val id: Int,
        val photoUrl: String,
        val sentence: String,
        val ttsUrl: String,
        val createdAt: String
    )

    // state
    val memories = remember { mutableStateListOf<MemoryItem>() }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // load once
    LaunchedEffect(patientId) {
        try {
            val token = Firebase.auth.currentUser
                ?.getIdToken(false)?.await()?.token
                ?: throw Exception("인증 토큰 없음")
            val url = "${BuildConfig.BASE_URL.trimEnd('/')}/memories?patient_id=$patientId"
            val resp = withContext(Dispatchers.IO) {
                client.newCall(
                    Request.Builder()
                        .url(url)
                        .addHeader("Authorization", "Bearer $token")
                        .get()
                        .build()
                ).execute()
            }
            if (!resp.isSuccessful) throw Exception("API 오류 ${resp.code}")
            val arr = JSONObject(resp.body?.string().orEmpty())
                .optJSONArray("memories")
                ?: throw Exception("memories 배열이 없습니다.")
            memories.clear()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                memories += MemoryItem(
                    id        = o.getInt("id"),
                    photoUrl  = o.getString("image_url"),
                    sentence  = o.getString("sentence"),
                    ttsUrl    = o.getString("tts_audio_url"),
                    createdAt = o.getString("created_at")
                )
            }
            errorMsg = null
        } catch (e: Exception) {
            errorMsg = "로드 실패: ${e.message}"
        }
    }

    Scaffold(
        bottomBar = {
            val navBackStack by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStack?.destination?.route
            val navColors = NavigationBarItemDefaults.colors(
                indicatorColor    = Color.Transparent,
                selectedIconColor = Color(0xFF00C4B4),
                unselectedIconColor = Color(0xFF888888),
                selectedTextColor   = Color(0xFF00C4B4),
                unselectedTextColor = Color(0xFF888888)
            )
            NavigationBar {
                listOf(
                    "sentence/$patientId" to "회상문장",
                    "quiz/$patientId"     to "회상퀴즈",
                    "alert"               to "긴급알림"
                ).forEach { (route, label) ->
                    NavigationBarItem(
                        icon    = { Icon(Icons.Default.VolumeUp, contentDescription = label) },
                        label   = { Text(label) },
                        selected= currentRoute == route,
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
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(padding)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 헤더
            Image(
                painter = painterResource(R.drawable.rogo),
                contentDescription = "Logo",
                modifier = Modifier.size(200.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(speakerTopGap))
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
            Spacer(Modifier.height(imageGap))

            // 에러
            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                Spacer(Modifier.height(btnGap))
                return@Column
            }

            // 리스트
            memories.forEach { mem ->
                AsyncImage(
                    model = mem.photoUrl,
                    contentDescription = "Memory Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(imageHeight)
                        .clip(RoundedCornerShape(imageCorner)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(btnGap))

                Button(
                    onClick = { scope.launch { playTTS(mem.ttsUrl, context) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(btnGap * btnHeightRatio),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                ) {
                    Text(mem.sentence, color = Color.White, fontSize = 16.sp)
                }
                Spacer(Modifier.height(btnGap * 2))
            }
        }
    }
}

// ────────────────────────────────────────────────
// TTS block (공통)
// ────────────────────────────────────────────────

private val ttsClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

private var currentTTSPlayer: MediaPlayer? = null

private suspend fun playTTS(
    ttsUrl: String,
    context: Context
) {
    try {
        // 1) 효과음
        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        delay(200)
        toneGen.release()

        // 2) 기존 플레이어 해제
        currentTTSPlayer?.apply {
            reset()
            release()
        }

        // 3) 새로운 MediaPlayer 생성·재생
        currentTTSPlayer = MediaPlayer().apply {
            setDataSource(ttsUrl)
            setOnPreparedListener { it.start() }
            prepareAsync()
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "TTS 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}












