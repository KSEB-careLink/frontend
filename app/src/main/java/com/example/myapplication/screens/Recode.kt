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
import androidx.compose.material3.*
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
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavController
import com.example.myapplication.R
import com.example.myapplication.BuildConfig
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

@Composable
fun RecodeUI(
    navController: NavController,
    patientId: String,
    voices: List<String>,
    onSelectVoice: (String) -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val (logo, title, selectButton, voiceListBox, bottomButton) = createRefs()

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
            onClick = { /* TODO: 파일 선택 */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(selectButton) {
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
                .constrainAs(voiceListBox) {
                    top.linkTo(selectButton.bottom, margin = 24.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                    bottom.linkTo(bottomButton.top, margin = 32.dp)
                    height = Dimension.preferredWrapContent
                }
        ) {
            if (voices.isEmpty()) {
                Text("등록된 목소리가 없습니다", color = Color.Gray, modifier = Modifier.padding(16.dp))
            } else {
                voices.forEachIndexed { index, name ->
                    Text(
                        text = "${index + 1}. $name",
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectVoice(name) }
                            .padding(vertical = 8.dp)
                    )
                    if (index < voices.lastIndex) Divider()
                }
            }
        }

        Button(
            onClick = { navController.navigate("recode2/$patientId") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(bottomButton) {
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
    var voices by remember { mutableStateOf<List<String>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    // OkHttp 클라이언트
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // 실제 데이터 로드 함수
    suspend fun loadVoices() {
        try {
            val listUrl = "${BuildConfig.BASE_URL}/voice/list/$patientId"
            val request = Request.Builder().url(listUrl).get().build()
            val resp = client.newCall(request).execute()
            resp.use {
                if (it.isSuccessful) {
                    val body = it.body?.string().orEmpty()
                    Log.d("RecodeScreen", "voice/list response: $body")
                    // 만약 [{"voice_id":"..."}] 형태라면 아래처럼 변경:
                    val arr = JSONArray(body)
                    voices = List(arr.length()) { i ->
                        // arr.getString(i)  // ▶ 문자열 배열일 때
                        arr.getJSONObject(i).getString("voice_id") // ▶ 객체 배열일 때
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "목소리 목록 불러오기 실패: ${it.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "목소리 목록 오류: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // **1) 화면 최초 진입 & 뒤 돌아올 때(onResume) 재호출**
    // Lifecycle observer를 달아 onResume 때마다 loadVoices()
    val lifecycleOwner = LocalViewModelStoreOwner.current as LifecycleOwner
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch { loadVoices() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // UI 렌더링
    RecodeUI(
        navController = navController,
        patientId = patientId,
        voices = voices,
        onSelectVoice = { voiceId ->
            onSelectVoice(voiceId)
            val streamUrl = "${BuildConfig.BASE_URL}/voice/download/$patientId/$voiceId"
            MediaPlayer().apply {
                setDataSource(streamUrl)
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        }
    )
}






