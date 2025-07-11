package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R

@Composable
fun MemoryInfoListScreen() {
    // 더미 데이터 목록
    val memoryList = listOf(
        "1번 사진. 이 사진은 12월 25일 크리스마스...",
        "2번 영상. 이 영상은 4월 4일 봄가족여행...",
        "3번 사진. 이 사진은 3월 4일 손녀의 입학식...",
        "",
        ""
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 로고
        Image(
            painter = painterResource(id = R.drawable.rogo), // res/drawable에 logo.png 필요
            contentDescription = "Logo",
            modifier = Modifier.size(64.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 타이틀
        Text(
            text = "회상 정보 데이터 확인",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 데이터 리스트
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(memoryList) { item ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .background(Color(0xFFFFC9D8), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        text = item,
                        modifier = Modifier.padding(start = 16.dp),
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}
