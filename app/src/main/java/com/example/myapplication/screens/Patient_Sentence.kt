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
import androidx.compose.material3.*
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeUp
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
    voiceId: String
) {
    // 1) 런타임 위치 권한 관리
    val context = LocalContext.current
    val perms = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    // 2) 권한 요청 및 서비스 시작
    LaunchedEffect(perms.allPermissionsGranted) {
        if (!perms.allPermissionsGranted) {
            perms.launchMultiplePermissionRequest()
        } else {
            Intent(context, LocationUpdatesService::class.java).also { intent ->
                ContextCompat.startForegroundService(context, intent)
            }
        }
    }

    // 3) 컴포저블 상태 및 HTTP 클라이언트
    val scope = rememberCoroutineScope()
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // 4) 메모리 모델 정의
    data class MemoryItem(
        val id: Int,
        val photoUrl: String,
        val sentence: String,
        val ttsUrl: String,
        val createdAt: String
    )

    // 5) 화면 상태
    val memories = remember { mutableStateListOf<MemoryItem>() }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // 6) 데이터 로드
    LaunchedEffect(patientId) {
        try {
            val token = Firebase.auth.currentUser
                ?.getIdToken(false)?.await()?.token
                ?: throw Exception("인증 토큰 없음")
            val url = "${BuildConfig.BASE_URL.trimEnd('/')}/memories/list/$patientId"
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

    // 7) UI 구성
    Scaffold(
        bottomBar = {
            val navBackStack by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStack?.destination?.route
            val navColors = NavigationBarItemDefaults.colors(
                indicatorColor      = Color.Transparent,
                selectedIconColor   = Color(0xFF00C4B4),
                unselectedIconColor = Color(0xFF888888),
                selectedTextColor   = Color(0xFF00C4B4),
                unselectedTextColor = Color(0xFF888888)
            )
            NavigationBar {
                listOf(
                    "sentence/{patientId}" to "회상문장",
                    "quiz/{patientId}"     to "회상퀴즈",
                    "alert"                to "긴급알림"
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
                Icon(Icons.Default.VolumeUp, contentDescription = null, Modifier.size(24.dp))
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

            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                Spacer(Modifier.height(12.dp))
                return@Column
            }

            memories.forEach { mem ->
                AsyncImage(
                    model = mem.photoUrl,
                    contentDescription = "Memory Photo",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { scope.launch { playTTS(mem.ttsUrl, context) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp * 4 / 4),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                ) {
                    Text(mem.sentence, color = Color.White, fontSize = 16.sp)
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ────────────────────────────────────────────────
// TTS helper
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
        // 알림음
        val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        toneGen.startTone(ToneGenerator.TONE_PROP_ACK, 200)
        delay(200)
        toneGen.release()

        // 기존 해제
        currentTTSPlayer?.apply {
            reset()
            release()
        }

        // 재생
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












