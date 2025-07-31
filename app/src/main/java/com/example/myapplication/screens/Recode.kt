package com.example.myapplication.screens

import android.media.MediaPlayer
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.util.concurrent.TimeUnit

/**
 * 원본 Recode UI를 분리하여 재사용 가능하도록 리팩토링
 */
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
        val (
            logo, title, selectButton, voiceListBox, bottomButton
        ) = createRefs()

        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            modifier = Modifier
                .size(200.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 80.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        Text(
            text = "보호자 음성 등록",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(logo.bottom, margin = 24.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        Button(
            onClick = { /* TODO: 파일 선택 로직 */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(selectButton) {
                    top.linkTo(title.bottom, margin = 32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
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
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(bottomButton.top, margin = 32.dp)
                    height = Dimension.preferredWrapContent
                }
        ) {
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

        Button(
            onClick = { navController.navigate("recode2/$patientId") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(bottomButton) {
                    bottom.linkTo(parent.bottom, margin = 40.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("녹음하러 가기", color = Color.White, fontSize = 16.sp)
        }
    }
}

/**
 * 실제 화면: 백엔드에서 목소리 리스트를 불러와 RecodeUI에 전달
 */
@Composable
fun RecodeScreen(
    navController: NavController,
    patientId: String,
    onSelectVoice: (String) -> Unit
) {
    val context = LocalContext.current
    var voices by remember { mutableStateOf<List<String>>(emptyList()) }

    // OkHttp 클라이언트 설정…
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    LaunchedEffect(patientId) {
        try {
            val url = "https://backend-f61l.onrender.com/voice/list/$patientId"
            val request = Request.Builder().url(url).get().build()
            val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
            if (response.isSuccessful) {
                response.body?.string()?.let { body ->
                    val arr = JSONArray(body)
                    voices = List(arr.length()) { i -> arr.getString(i) }
                }
            } else {
                Toast.makeText(context, "목소리 목록 불러오기 실패: ${response.code}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "목소리 목록 오류: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    RecodeUI(
        navController = navController,
        patientId = patientId,
        voices = voices,
        onSelectVoice = { voiceId ->
            // 1) MainActivity에 선택값 전달
            onSelectVoice(voiceId)
            // 2) 즉시 미리 듣기용 재생 (선택 사항)
            val streamUrl = "https://backend-f61l.onrender.com/voice/download/$patientId/$voiceId"
            MediaPlayer().apply {
                setDataSource(streamUrl)
                setOnPreparedListener { it.start() }
                prepareAsync()
            }
        }
    )
}



