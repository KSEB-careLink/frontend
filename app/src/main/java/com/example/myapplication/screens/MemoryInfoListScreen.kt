package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.myapplication.R

@Composable
fun MemoryInfoListScreen() {
    val memoryList = listOf(
        "1번 사진. 이 사진은 12월 25일 크리스마스...",
        "2번 영상. 이 영상은 4월 4일 봄가족여행...",
        "3번 사진. 이 사진은 3월 4일 손녀의 입학식...",
        "",
        ""
    )

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val (logo, title, list) = createRefs()

        // 상단 로고
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(64.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        // 제목 텍스트
        Text(
            text = "회상 정보 데이터 확인",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(logo.bottom, margin = 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        // 데이터 리스트
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .constrainAs(list) {
                    top.linkTo(title.bottom, margin = 24.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.fillToConstraints
                }
                .fillMaxWidth()
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
