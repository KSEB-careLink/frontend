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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.delay

@Composable
fun Patient_Sentence(
    navController: NavController,
    patientId: String,
    voiceId: String
) {
    val speakerTopGap = 16.dp
    val imageBoxTopGap = 16.dp
    val imageBoxHeight = 330.dp
    val imageBoxCorner = 12.dp
    val recallBtnGap = 12.dp
    val recallBtnHeight = 56.dp

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    data class MemoryItem(val id: String, val description: String, val mediaUrl: String)

    val memoryList = remember { mutableStateListOf<MemoryItem>() }
    var selectedIndex by remember { mutableStateOf(0) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(patientId) {
        try {
            val token = Firebase.auth.currentUser?.getIdToken(true)?.await()?.token
                ?: throw Exception("인증 토큰 없음")
            val url = "${BuildConfig.BASE_URL}/memory/list/$patientId"
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
                .optJSONArray("memoryItems") ?: throw Exception("memoryItems 없음")
            memoryList.clear()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                memoryList += MemoryItem(
                    id = obj.getString("id"),
                    description = obj.getString("description"),
                    mediaUrl = obj.getString("mediaUrl")
                )
            }
            errorMsg = null
        } catch (e: Exception) {
            errorMsg = "불러오기 실패: ${e.message}"
        }
    }

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
                ).forEach { (route, label) ->
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Star, contentDescription = label) },
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            errorMsg?.let {
                Text(it, color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                return@Column
            }

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
                        withStyle(SpanStyle(color = Color(0xFF00C4B4))) {
                            append("나시나요?")
                        }
                    },
                    fontSize = 24.sp
                )
            }

            Spacer(Modifier.height(imageBoxTopGap))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageBoxHeight)
                    .background(Color(0xFFEDE9F5), RoundedCornerShape(imageBoxCorner)),
                contentAlignment = Alignment.Center
            ) {
                if (memoryList.isNotEmpty()) {
                    AsyncImage(
                        model = memoryList[selectedIndex].mediaUrl,
                        contentDescription = "Memory Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(recallBtnGap))
            memoryList.forEachIndexed { idx, item ->
                Button(
                    onClick = {
                        selectedIndex = idx
                        scope.launch { playTTS(context, voiceId, item.description) }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(recallBtnHeight),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                ) {
                    Text(item.description, color = Color.White, fontSize = 14.sp)
                }
                Spacer(Modifier.height(recallBtnGap))
            }
        }
    }
}

// ────────────────────────────────
//  TTS 관련 전역 변수 및 함수 quiz랑 공통
// ────────────────────────────────

private val ttsClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .build()

private var currentTTSPlayer: MediaPlayer? = null

private suspend fun playTTS(
    context: Context,
    voiceId: String,
    text: String
) {
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
            val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGen.startTone(ToneGenerator.TONE_PROP_ACK)
            delay(200)
            toneGen.release()

            currentTTSPlayer?.apply {
                reset()
                release()
            }

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









