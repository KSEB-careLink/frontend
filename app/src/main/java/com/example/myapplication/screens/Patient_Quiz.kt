package com.example.myapplication.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.example.myapplication.data.DatasetItem
import com.example.myapplication.data.QuizOption
import com.example.myapplication.viewmodel.QuizViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject

@Composable
fun Patient_Quiz(
    navController: NavController,
    patientId: String,
    quizViewModel: QuizViewModel = viewModel()
) {
    val context = LocalContext.current

    // 1) 진입 시 퀴즈 불러오기
    LaunchedEffect(patientId) {
        quizViewModel.loadQuizzes(patientId)
    }

    // 2) ViewModel 상태 구독
    val items by quizViewModel.items.collectAsState()
    val error by quizViewModel.error.collectAsState()

    // 에러 발생 시 토스트
    LaunchedEffect(error) {
        error?.let { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
    }

    var currentIndex by remember { mutableStateOf(0) }

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
                QuizContent(
                    item = items[currentIndex],
                    onNext = {
                        if (currentIndex < items.size - 1) {
                            currentIndex++
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun QuizBottomBar(navController: NavController) {
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
            "sentence" to "회상문장",
            "quiz"     to "회상퀴즈",
            "alert"    to "긴급알림"
        ).forEach { (route, label) ->
            NavigationBarItem(
                icon    = { Icon(Icons.Default.Timer, contentDescription = label) },
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
                colors  = navColors
            )
        }
    }
}

@Composable
private fun QuizContent(
    item: DatasetItem,
    onNext: () -> Unit
) {
    var selected     by remember { mutableStateOf<Int?>(null) }
    var showResult   by remember { mutableStateOf(false) }
    var elapsedTime  by remember { mutableStateOf(0L) }
    var questionTime by remember { mutableStateOf<Long?>(null) }

    val scope   = rememberCoroutineScope()
    val context = LocalContext.current
    val client  = remember { OkHttpClient() }

    // 예시: 응답 전송 URL
    val url = BuildConfig.BASE_URL.trimEnd('/') + "/quiz-response"

    // 새 문제 진입 시 초기화
    LaunchedEffect(item.questionId) {
        selected   = null
        showResult = false
    }

    // 타이머 로직
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
            // 타이머 표시
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = "타이머", modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text     = String.format("%02d:%02d", elapsedTime / 60, elapsedTime % 60),
                    fontSize = 20.sp
                )
            }
            Spacer(Modifier.height(16.dp))

            // 회상 문장 (DatasetItem.sentence)
            Text(text = item.sentence, fontSize = 18.sp, lineHeight = 24.sp)
            Spacer(Modifier.height(16.dp))

            // 퀴즈 질문
            Text(item.question, fontSize = 28.sp, color = Color(0xFF00C4B4))
            Spacer(Modifier.height(16.dp))

            // 보기 렌더링
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 2줄씩 묶어서
                item.options.chunked(2).forEachIndexed { rowIndex, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        rowItems.forEachIndexed { indexInRow, option ->
                            val flatIndex = rowIndex * 2 + indexInRow
                            OptionButton(text = option.optionText, modifier = Modifier.weight(1f)) {
                                selected     = flatIndex
                                questionTime = elapsedTime
                                showResult   = true
                            }
                        }
                    }
                }
            }
        } else {
            // 결과 화면
            Spacer(Modifier.height(100.dp))
            // 정답 인덱스 찾기
            val correctIndex = item.options.indexOfFirst { it.isCorrect }
            val isCorrect    = (selected == correctIndex)

            Text(
                text     = if (isCorrect) "정답이에요!" else "오답이에요!",
                fontSize = 32.sp,
                color    = if (isCorrect) Color(0xFF00A651) else Color(0xFFE2101A)
            )
            Spacer(Modifier.height(16.dp))

            questionTime?.let {
                Text("풀이 시간: ${it}초", fontSize = 18.sp)
                Spacer(Modifier.height(16.dp))
            }

            Image(
                painter            = painterResource(if (isCorrect) R.drawable.ch else R.drawable.wr),
                contentDescription = null,
                modifier           = Modifier.size(300.dp),
                contentScale       = ContentScale.Fit
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text     = if (isCorrect) "정말 잘 기억하셨어요😊" else "다시 기억해볼까요?",
                fontSize = 20.sp
            )
            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (isCorrect) {
                        scope.launch {
                            // 서버 응답 전송
                            val idToken = Firebase.auth.currentUser
                                ?.getIdToken(false)?.await()?.token
                            if (idToken.isNullOrBlank()) {
                                Toast.makeText(context, "인증 토큰 없음", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val bodyJson = JSONObject().apply {
                                put("quizId", item.questionId.toString())
                                put("selected_index", selected)
                                put("duration", questionTime ?: 0)
                            }.toString()
                            val req = Request.Builder()
                                .url(url)
                                .addHeader("Authorization", "Bearer $idToken")
                                .post(bodyJson.toRequestBody("application/json".toMediaType()))
                                .build()
                            withContext(Dispatchers.IO) {
                                client.newCall(req).execute().close()
                            }
                            onNext()
                        }
                    } else {
                        selected   = null
                        showResult = false
                    }
                },
                modifier = Modifier
                    .width(130.dp)
                    .height(56.dp),
                shape  = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text(if (isCorrect) "다음 문제로" else "다시 풀기", color = Color.White)
            }
        }
    }
}

@Composable
private fun OptionButton(
    text: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
    ) {
        Text(text, color = Color.White)
    }
}















