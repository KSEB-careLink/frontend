package com.example.myapplication.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.myapplication.R
import com.example.myapplication.viewmodel.EmotionViewModel
import java.util.*
import androidx.compose.foundation.Image

@Composable
fun HomeScreen(
    navController: NavHostController,
    emotionViewModel: EmotionViewModel
) {
    val context = LocalContext.current
    var selectedEmotion by remember { mutableStateOf<String?>(null) }

    // 랜덤 메시지 리스트와 상태 추가
    val messages = listOf("오늘도 멋진 하루예요!", "당신은 소중해요", "마음껏 표현해보세요!")
    var randomMessage by remember { mutableStateOf(messages.random()) }

    val primaryColor = Color(0xFF5A8F7B)
    val backgroundColor = Color(0xFFFAFDFB)
    val cardColor = Color(0xFFE6F2EF)
    val borderColor = Color(0xFFD0E2D2)
    val textDark = Color(0xFF333333)
    val ivoryColor = Color(0xFFFFF8EC)
    val pastelOlive = Color(0xFFCBD8B3) // 연한 올리브색

    Scaffold(
        containerColor = backgroundColor,
        topBar = {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(cardColor)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.rogo),
                        contentDescription = "Main Banner",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .fillMaxWidth(0.4f)
                            .height(80.dp)
                    )

                    IconButton(
                        onClick = {
                            Toast.makeText(context, "햄버거 메뉴 클릭됨", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 16.dp)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Menu,
                            contentDescription = "Menu",
                            tint = Color.Black,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.Black)
                )
            }
        },
        bottomBar = {
            Column {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .background(Color.Black)
                )
                BottomNavigationBar(navController, context)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(backgroundColor)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // 랜덤 메시지 박스 (클릭 시 메시지 변경)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 4.dp,
                color = cardColor,
                border = BorderStroke(1.dp, borderColor)
            ) {
                Box(
                    modifier = Modifier
                        .padding(vertical = 16.dp)
                        .clickable {
                            // 현재 메시지를 제외하고 다른 메시지 랜덤 선택
                            randomMessage = messages.filter { it != randomMessage }.random()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = randomMessage,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium,
                        color = textDark
                    )
                }
            }

            // 감정 선택 영역 - 배경이 화면 가로 꽉 차도록 수정됨
            Box(
                modifier = Modifier
                    .fillMaxWidth() // 화면 가로 꽉 채움
                    .clip(RoundedCornerShape(16.dp))
                    .background(ivoryColor)
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),  // 가로 꽉 채움
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "오늘의 감정을 선택해주세요",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textDark,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        textAlign = TextAlign.Center
                    )
                    EmotionGrid(selectedEmotion) { emoji ->
                        selectedEmotion = emoji
                        val today = Calendar.getInstance().let {
                            "${it.get(Calendar.YEAR)}-${it.get(Calendar.MONTH) + 1}-${it.get(Calendar.DAY_OF_MONTH)}"
                        }
                        emotionViewModel.saveEmotion(today, emoji)
                        Toast.makeText(context, "오늘 기분이 기록되었습니다", Toast.LENGTH_SHORT).show()
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "최근 감정: 😊 😊 😭 😠 😊",
                            fontSize = 15.sp,
                            color = Color(0xFF4E4E4E)
                        )
                    }
                }
            }

            // 분석 버튼
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = { navController.navigate("camera1") },
                    colors = ButtonDefaults.buttonColors(containerColor = pastelOlive),
                    shape = RoundedCornerShape(30.dp),
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(50.dp)
                ) {
                    Text(
                        "내 그림 감정 분석하기",
                        color = Color.Black,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun EmotionGrid(
    selectedEmotion: String?,
    onEmotionSelected: (String) -> Unit
) {
    val emotions = listOf("😊", "😐", "😭", "😡", "😵", "😍")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        emotions.chunked(3).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(vertical = 6.dp)
            ) {
                row.forEach { emoji ->
                    val isSelected = selectedEmotion == emoji
                    val backgroundColor by animateColorAsState(
                        if (isSelected) Color(0xFFFFF0E5) else Color.White,
                        label = ""
                    )
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(backgroundColor)
                            .border(1.dp, Color.LightGray, CircleShape)
                            .clickable { onEmotionSelected(emoji) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 24.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BottomNavigationBar(navController: NavHostController, context: Context) {
    NavigationBar(containerColor = Color(0xFFE6F2EF)) {
        NavigationBarItem(
            selected = false,
            onClick = { navController.navigate("emotion_list") },
            icon = { Icon(Icons.Filled.Favorite, contentDescription = "기록 보기") },
            label = { Text("기록") }
        )
        NavigationBarItem(
            selected = false,
            onClick = { openGalleryFolder(context) },
            icon = { Icon(Icons.Filled.PhotoLibrary, contentDescription = "갤러리") },
            label = { Text("갤러리") }
        )
    }
}

fun openGalleryFolder(context: Context) {
    val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    if (picturesDir.exists()) {
        val uri = Uri.parse(picturesDir.path)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "image/*")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            Toast.makeText(context, "갤러리 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
        }
    } else {
        Toast.makeText(context, "사진 폴더가 존재하지 않습니다.", Toast.LENGTH_SHORT).show()
    }
}




























































