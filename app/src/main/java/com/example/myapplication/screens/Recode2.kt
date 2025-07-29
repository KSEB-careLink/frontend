package com.example.myapplication.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.audio.AudioRecorder
import com.example.myapplication.audio.AutoScrollingText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun Recode2(navController: NavController, patientId: String) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isRecording by remember { mutableStateOf(false) }
    var audioPath by remember { mutableStateOf<String?>(null) }
    var isAutoScrollEnabled by remember { mutableStateOf(true) }

    // 권한 요청 런처
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            if (!granted) {
                Toast.makeText(context, "녹음 권한이 필요합니다", Toast.LENGTH_SHORT).show()
            }
        }
    )
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val recorder = remember { AudioRecorder(context) }

    // OkHttp 클라이언트 (로깅 + 타임아웃 설정)
    val client = remember {
        OkHttpClient.Builder().apply {
            // 로깅
            val logging = HttpLoggingInterceptor { msg -> Log.d("OkHttp", msg) }
            logging.level = HttpLoggingInterceptor.Level.BODY
            addInterceptor(logging)

            // 타임아웃: 연결, 쓰기, 읽기 모두 120초
            connectTimeout(120, TimeUnit.SECONDS)
            writeTimeout(120, TimeUnit.SECONDS)
            readTimeout(120, TimeUnit.SECONDS)
        }.build()
    }

    // UI 상수
    val topPadding = 120.dp
    val betweenTitleAndSub = 8.dp
    val greyBoxTopGap = 16.dp
    val greyBoxHeight = 400.dp
    val greyBoxCorner = 12.dp

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = topPadding, start = 24.dp, end = 24.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (!isRecording) "녹음 전" else "녹음 중",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
            Spacer(Modifier.height(betweenTitleAndSub))
            Text(
                text = if (!isRecording)
                    "버튼을 눌러 녹음을 시작하세요"
                else
                    "최대한 생동감 있게 텍스트를 읽어주세요",
                fontSize = 16.sp,
                color = Color.DarkGray
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 4.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("자동 스크롤", fontSize = 14.sp, color = Color.Gray)
                Switch(
                    checked = isAutoScrollEnabled,
                    onCheckedChange = { isAutoScrollEnabled = it }
                )
            }
            Spacer(Modifier.height(greyBoxTopGap))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(greyBoxHeight)
                    .background(Color(0xFFCCCCCC), RoundedCornerShape(greyBoxCorner)),
                contentAlignment = Alignment.Center
            ) {
                AutoScrollingText(
                    text = "그날 아침, 윤하는 평소보다 일찍 깼다.\n" +
                            "여름의 끝자락은 생각보다 조용했고, 창밖의 은행나무도 바람에 살며시 흔들릴 뿐이었다.\n" +
                            "그녀는 부엌에서 커피를 내리고, 습관처럼 우편함을 확인하러 나갔다.\n\n" +
                            "아파트 현관 앞, 녹슨 우편함 사이로 작은 흰 봉투 하나가 눈에 띄었다.\n" +
                            "주소도, 발신인도 없이 단순히 이렇게 적혀 있었다.\n" +
                            "“101호에게”\n\n" +
                            "윤하는 고개를 갸웃하며 봉투를 집어 들었다. 왠지 낯설지 않았다.\n" +
                            "손에 쥔 촉감은 오래된 종이의 그것처럼 따뜻했고, 안에는 조심스레 접힌 편지지 한 장이 들어 있었다.\n\n" +
                            "“안녕하세요.\n" +
                            "아마도 이 편지를 읽고 있는 당신은 낯선 이름일지도 모르겠지만,\n" +
                            "나는 늘 이 복도 끝에서 당신을 바라보았습니다.\n" +
                            "하루하루 말없이 지나는 모습에서, 이상하리만큼 위로를 받았습니다.\n" +
                            "그런 당신에게 언젠가 한 번쯤 고맙다고 말하고 싶었어요.\n" +
                            "이 편지가 너무 갑작스럽지 않기를 바랍니다.\n" +
                            "오늘도 평온한 하루가 되시길.”\n\n" +
                            "그 편지를 읽는 순간, 윤하는 이상하게 눈시울이 따뜻해졌다.\n" +
                            "무언가가 빠르게 지나갔다. 이름도 없는 한 문장 한 문장들이\n" +
                            "요란한 삶 속에서 무심히 지나쳤던 작은 순간들을 조명처럼 비췄다.\n\n" +
                            "그녀는 생각했다.\n" +
                            "이름도 모르는 누군가가, 그녀의 일상 속에서\n" +
                            "그렇게 작고도 다정한 눈길을 보내고 있다는 사실이 참 낯설고 따뜻하게 느껴졌다.\n\n" +
                            "그날 이후, 윤하는 매일 아침 우편함을 열었다.\n" +
                            "편지가 또 오리라는 보장은 없었지만,\n" +
                            "그 시간만큼은 그녀도 누군가를 위해 무언가를 기다리는 사람이 되었다.\n\n" +
                            "그리고 언젠가,\n" +
                            "그녀도 한 통의 편지를 보냈다.\n" +
                            "“당신이 이 편지를 읽게 된다면, 나도 당신의 하루가 평온하기를 바라고 있어요.”\n\n" +
                            "윤하는 그날 이후로 매일 아침 편지를 기대하게 되었다.\n" +
                            "늘 열지 않던 우편함을 하루에도 두세 번씩 열어보기도 했다.\n" +
                            "그 안에 아무것도 없다는 사실이 아쉽기는 했지만,\n" +
                            "이상하게도 그 기다림 자체가 위로가 되었다.\n\n" +
                            "그러던 어느 날,\n" +
                            "작은 흰 봉투가 다시 도착해 있었다.\n" +
                            "이번에는 반듯하게 붙인 스티커 하나가 붙어 있었다.\n\n" +
                            "“101호, 그 편지… 잘 받았습니다.”\n\n" +
                            "편지를 펼치자, 이전보다 조금 더 긴 문장이 그녀를 반겼다.\n\n" +
                            "“사실 저는 302호에 살고 있어요.\n" +
                            "당신이 매일 아침 커피를 들고 나오는 모습이 좋아서,\n" +
                            "혼자 창문 너머로 바라보던 시간이 참 많았습니다.\n" +
                            "어느 날은 당신이 하늘을 바라보며 웃고 있었고,\n" +
                            "또 어느 날은 조용히 벤치에 앉아 무언가를 쓰고 있었죠.\n\n" +
                            "사실은...\n" +
                            "그날 처음 편지를 썼을 때, 답장이 올 거라 생각하지 않았습니다.\n" +
                            "하지만 당신의 말, 당신의 손글씨를 보며\n" +
                            "오랫동안 마음속에만 있던 '인사'를 꺼내고 싶어졌습니다.\n\n" +
                            "감사합니다.\n" +
                            "그냥… 그렇게 거기 있어줘서.”\n\n" +
                            "편지를 다 읽은 윤하는 마치 영화의 한 장면처럼 천천히 고개를 들어 복도를 바라보았다.\n" +
                            "문득, 302호 현관 앞에 조심스레 놓인 작은 화분 하나가 눈에 들어왔다.\n" +
                            "화분 안에는 작은 종이 쪽지가 꽂혀 있었다.\n\n" +
                            "“오늘도, 그대의 하루가 평온하길.”\n\n" +
                            "그날 이후, 두 사람은 서로의 이름을 모른 채\n" +
                            "한동안 편지를 주고받았다.\n\n" +
                            "이름이 없는 대화, 얼굴이 없는 교감.\n" +
                            "하지만 오히려 그래서 더 조심스럽고, 더 진심이었는지도 모른다.\n\n" +
                            "그리고 어느 흐린 오후,\n" +
                            "윤하가 작은 종이봉투 하나를 우편함에 넣었다.\n\n" +
                            "“오늘 저녁 7시, 벤치에 앉아 있을게요.\n" +
                            "괜찮다면, 당신의 이름을 듣고 싶어요.”\n\n" +
                            "하늘은 구름으로 가득했지만,\n" +
                            "그날따라 윤하의 마음은 말갛게 맑았다.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(greyBoxHeight),
                    autoScroll = isAutoScrollEnabled
                )
            }
        }

        Button(
            onClick = {
                // 권한 체크
                val hasPermission = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                if (!hasPermission) {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    return@Button
                }

                if (!isRecording) {
                    // 녹음 시작
                    try {
                        audioPath = recorder.startRecording()
                        isRecording = true
                    } catch (e: Exception) {
                        Toast.makeText(context, "녹음 시작 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // 녹음 종료
                    val savedPath = recorder.stopRecording()
                    isRecording = false
                    Toast.makeText(context, "녹음 저장: $savedPath", Toast.LENGTH_LONG).show()

                    // 업로드
                    audioPath?.let { path ->
                        coroutineScope.launch {
                            // Firebase ID 토큰
                            val firebaseUser = Firebase.auth.currentUser
                            val idToken = try {
                                firebaseUser?.getIdToken(true)?.await()?.token ?: ""
                            } catch (e: Exception) {
                                ""
                            }

                            // B 방식: uid, name 필드
                            val uid = firebaseUser?.uid ?: ""
                            val name = firebaseUser?.displayName ?: "GuardianName"

                            val uploadUrl = "https://backend-f61l.onrender.com/registerVoice"

                            val file = File(path)
                            val mime = "audio/wav"
                            val body = MultipartBody.Builder()
                                .setType(MultipartBody.FORM)
                                .addFormDataPart("guardian_uid", uid)
                                .addFormDataPart("name", name)
                                .addFormDataPart(
                                    "file", file.name,
                                    file.asRequestBody(mime.toMediaTypeOrNull())
                                )
                                .build()

                            val requestBuilder = Request.Builder()
                                .url(uploadUrl)
                                .post(body)
                            if (idToken.isNotEmpty()) {
                                requestBuilder.addHeader("Authorization", "Bearer $idToken")
                            }
                            val request = requestBuilder.build()

                            val resp = withContext(Dispatchers.IO) {
                                client.newCall(request).execute()
                            }
                            withContext(Dispatchers.Main) {
                                if (resp.isSuccessful) {
                                    Toast.makeText(context, "목소리 등록 성공", Toast.LENGTH_LONG).show()
                                    navController.navigate("recode")
                                } else {
                                    Toast.makeText(context, "등록 실패: ${resp.code}", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-170).dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text(
                text = if (!isRecording) "녹음 시작" else "녹음 완료",
                color = Color.White,
                fontSize = 18.sp
            )
        }
    }
}







