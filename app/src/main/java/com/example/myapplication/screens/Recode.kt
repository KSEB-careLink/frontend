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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
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
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavController
import com.example.myapplication.R
import com.example.myapplication.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@Composable
fun RecodeUI(
    navController: NavController,
    patientId: String,
    samples: List<String>,
    error: String?,
    onPlaySample: (String) -> Unit,
    onRecordClick: () -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val (logo, title, selectBtn, sampleBox, errText, bottomBtn) = createRefs()

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
                .constrainAs(selectBtn) {
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
                .constrainAs(sampleBox) {
                    top.linkTo(selectBtn.bottom, margin = 24.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                    bottom.linkTo(errText.top, margin = 8.dp)
                    height = Dimension.preferredWrapContent
                }
        ) {
            if (samples.isEmpty()) {
                Text("등록된 목소리가 없습니다", color = Color.Gray, modifier = Modifier.padding(16.dp))
            } else {
                samples.forEachIndexed { idx, url ->
                    Text(
                        text = "${idx + 1}. 샘플 듣기",
                        fontSize = 16.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPlaySample(url) }
                            .padding(vertical = 8.dp)
                    )
                    if (idx < samples.lastIndex) Divider()
                }
            }
        }

        // 에러가 있을 때만 보여줌
        error?.let {
            Text(
                text = "목소리 목록 오류: $it",
                color = Color.Red,
                modifier = Modifier
                    .constrainAs(errText) {
                        bottom.linkTo(bottomBtn.top, margin = 16.dp)
                        start.linkTo(parent.start)
                    }
                    .padding(4.dp)
            )
        }

        Button(
            onClick = onRecordClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(bottomBtn) {
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

    // 샘플 URL 리스트와 에러 메시지 상태
    var samples by remember { mutableStateOf<List<String>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    // OkHttp 클라이언트 설정
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    suspend fun loadSamples() {
        error = null
        try {
            // 1) Firebase ID 토큰
            val idToken = Firebase.auth.currentUser
                ?.getIdToken(true)
                ?.await()
                ?.token
                .orEmpty()

            val url = "${BuildConfig.BASE_URL}/voice-sample/list/$patientId"
            Log.d("RecodeScreen", "샘플 호출 URL: $url")

            withContext(Dispatchers.IO) {
                val req = Request.Builder()
                    .url(url)
                    .get()
                    .apply { if (idToken.isNotBlank()) addHeader("Authorization", "Bearer $idToken") }
                    .build()

                client.newCall(req).execute().use { resp ->
                    if (!resp.isSuccessful) {
                        error = "HTTP ${resp.code}"
                        Log.e("RecodeScreen", "비정상 HTTP 코드: ${resp.code}")
                        return@use
                    }
                    val body = resp.body?.string().orEmpty()
                    Log.d("RecodeScreen", "샘플 응답 JSON: $body")
                    val arr = JSONObject(body).optJSONArray("voiceSamples")
                    if (arr != null) {
                        withContext(Dispatchers.Main) {
                            samples = List(arr.length()) { i -> arr.getString(i) }
                        }
                    } else {
                        error = "파싱 오류: voiceSamples 배열 없음"
                        Log.e("RecodeScreen", "파싱 오류: voiceSamples 배열 없음")
                    }
                }
            }
        } catch (e: Exception) {
            error = e.message
            Log.e("RecodeScreen", "loadSamples 예외", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "샘플 로드 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 1) 최초 진입 시 한 번
    LaunchedEffect(patientId) {
        scope.launch { loadSamples() }
    }

    // 2) 뒤로 돌아오면(onResume) 다시 부르기
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = LifecycleEventObserver { _, ev ->
            if (ev == Lifecycle.Event.ON_RESUME) {
                scope.launch { loadSamples() }
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    RecodeUI(
        navController  = navController,
        patientId      = patientId,
        samples        = samples,
        error          = error,
        onPlaySample   = { url ->
            onSelectVoice(url)
            MediaPlayer().apply {
                setDataSource(url)
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        },
        onRecordClick  = {
            navController.navigate("recode2/$patientId")
        }
    )
}













