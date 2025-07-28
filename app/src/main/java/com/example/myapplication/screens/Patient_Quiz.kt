package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.myapplication.R
import com.example.myapplication.data.DatasetItem
import com.example.myapplication.viewmodel.QuizViewModel
import kotlinx.coroutines.delay
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
// OkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

// JSON
import org.json.JSONObject

// Android
import com.example.myapplication.BuildConfig

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope

// 기타
import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth
import kotlinx.coroutines.tasks.await

@Composable
fun Patient_Quiz(
    navController: NavController,
    viewModel: QuizViewModel = viewModel()
) {
    val items by viewModel.items.collectAsState()

    // 현재 보고 있는 문제 인덱스
    var currentIndex by remember { mutableStateOf(0) }

    Scaffold(
        bottomBar = { QuizBottomBar(navController) }
    ) { innerPadding ->
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
                Text("로딩 중… items.size = ${items.size}")
            } else {
                // onNext 에서 인덱스 범위 체크
                QuizContent(
                    item   = items[currentIndex],
                    onNext = {
                        if (currentIndex < items.size - 1) {
                            currentIndex++
                        } else {
                            // 마지막 문제까지 풀었을 때 동작
                            // 예: currentIndex = 0   // 처음으로 돌아가기
                            // 또는 결과 화면으로 네비게이션
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
fun QuizContent(
    item: DatasetItem,
    onNext: () -> Unit    // 다음 문제로 넘어갈 콜백
) {
    // — 레이아웃 값들 —
    val speakerGap  = 16.dp
    val greyGap     = 16.dp
    val greyHeight  = 450.dp
    val greyCorner  = 12.dp
    val questionGap = 16.dp
    val optionGap   = 12.dp

    // — 상태들 —
    var selected     by remember { mutableStateOf<Int?>(null) }
    var showResult   by remember { mutableStateOf(false) }
    var elapsedTime  by remember { mutableStateOf(0L) }
    var questionTime by remember { mutableStateOf<Long?>(null) }

    // ① 새로운 item이 들어올 때마다 상태 초기화
    LaunchedEffect(item.questionId) {
        selected     = null
        showResult   = false
        questionTime = null
    }

    // ② 타이머: showResult == false 일 때만 동작
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

    // 네트워크 호출 준비
    val client = remember { OkHttpClient() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // 서버 라우터에 붙인 경로에 맞춰 quiz-response 로
    val url = BuildConfig.BASE_URL.trimEnd('/') + "/quiz-response"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        // ───────── 타이머 ─────────
        if (!showResult) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Timer, contentDescription = "타이머", modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = String.format("%02d:%02d", elapsedTime / 60, elapsedTime % 60),
                    fontSize = 20.sp
                )
            }
            Spacer(Modifier.height(speakerGap))
        }

        if (!showResult) {
            // ───────── 리마인더 + 질문 ─────────
            Text(text = item.reminder, fontSize = 18.sp, lineHeight = 24.sp)
            Spacer(Modifier.height(greyGap))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(greyHeight)
                    .background(Color(0xFFEDE9F5), RoundedCornerShape(greyCorner))
            )

            Spacer(Modifier.height(questionGap))
            Text(item.question, fontSize = 28.sp, color = Color(0xFF00C4B4))
            Spacer(Modifier.height(questionGap))

            // ───────── 옵션 버튼 ─────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(optionGap)
            ) {
                item.options.chunked(2).forEachIndexed { rowIndex, rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(optionGap)
                    ) {
                        rowItems.forEachIndexed { indexInRow, text ->
                            val flatIndex = rowIndex * 2 + indexInRow
                            OptionButton(text = text, modifier = Modifier.weight(1f)) {
                                selected     = flatIndex
                                questionTime = elapsedTime
                                showResult   = true
                            }
                        }
                    }
                }
            }

        } else {
            // ───────── 결과 화면 ─────────
            Spacer(Modifier.height(100.dp))
            val isCorrect = selected == item.answer

            Text(
                text = if (isCorrect) "정답이에요!" else "오답이에요!",
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
                text = if (isCorrect) "정말 잘 기억하셨어요😊" else "다시 기억해볼까요?",
                fontSize = 20.sp
            )

            Spacer(Modifier.height(questionGap))
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isCorrect) {
                        // 로그: 서버 전송 시작
                        Log.d("QuizContent", "Sending answer to $url quizId=${item.questionId}, selected=$selected, duration=$questionTime")
                        scope.launch {
                            // 1) Firebase Auth ID 토큰 받아오기
                            val idToken = try {
                                Firebase.auth.currentUser
                                    ?.getIdToken(false)
                                    ?.await()
                                    ?.token
                            } catch (e: Exception) {
                                Log.e("QuizContent", "Failed to fetch ID token", e)
                                null
                            }
                            if (idToken.isNullOrBlank()) {
                                Log.e("QuizContent", "No ID token available")
                                Toast.makeText(context, "인증 토큰 없음", Toast.LENGTH_SHORT).show()
                                return@launch
                            }

                            // 2) 요청 바디
                            val bodyJson = JSONObject().apply {
                                put("quizId", item.questionId.toString())
                                put("selected_index", selected)
                                put("duration", questionTime ?: 0)
                            }.toString()
                            val reqBody = bodyJson.toRequestBody("application/json".toMediaType())

                            // 3) 헤더에 Authorization 추가
                            val request = Request.Builder()
                                .url(url)
                                .addHeader("Authorization", "Bearer $idToken")
                                .post(reqBody)
                                .build()

                            try {
                                val response = withContext(Dispatchers.IO) {
                                    client.newCall(request).execute()
                                }
                                Log.d("QuizContent", "Response code=${response.code}")
                                val respBody = response.body?.string()
                                Log.d("QuizContent", "Response body=$respBody")

                                if (response.isSuccessful && respBody != null) {
                                    val result = JSONObject(respBody).optString("result", "오류")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                                        onNext()
                                    }
                                } else {
                                    Log.e("QuizContent", "Server error: code=${response.code}")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "서버 오류: ${response.code}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("QuizContent", "Network error sending answer", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "네트워크 오류", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    } else {
                        // 오답이면 다시 풀기
                        selected   = null
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












