// OnboardingScreen.kt
package com.example.myapplication.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.navigation.NavController
import com.example.myapplication.R

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    patientId: String,
    onFinish: () -> Unit
) {
    val primaryColor = Color(0xFF6200EE)
    val dividerColor = Color.Gray.copy(alpha = 0.3f)

    // 1) 페이지 데이터
    val pages = listOf(
        "오렌지!" to R.drawable.ic_1,
        "레몬!"   to R.drawable.ic_3,
        "수박!"   to R.drawable.ic_4
    )

    // 2) PagerState
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size }
    )
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxSize()) {
        Spacer(modifier = Modifier.weight(1f))

        // 3) HorizontalPager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(4f)
        ) { page ->
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = pages[page].second),
                    contentDescription = pages[page].first,
                    modifier = Modifier.size(240.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = pages[page].first)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // 4) 점선 구분선
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .padding(horizontal = 40.dp)
        ) {
            val strokePx = with(density) { 2.dp.toPx() }
            drawLine(
                color = dividerColor,
                start = Offset.Zero,
                end = Offset(size.width, 0f),
                strokeWidth = strokePx,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5) 이전 • ●○ • 다음/시작하기
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 이전 버튼: 높이만 고정
            OutlinedButton(
                onClick = {
                    scope.launch {
                        val prev = pagerState.currentPage - 1
                        if (prev >= 0) pagerState.animateScrollToPage(prev)
                    }
                },
                enabled = pagerState.currentPage > 0,
                shape = CircleShape,
                border = BorderStroke(2.dp, primaryColor),
                modifier = Modifier.height(48.dp)
            ) {
                Text("이전", color = primaryColor)
            }

            // ●○ 인디케이터
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { idx ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (idx == pagerState.currentPage) primaryColor
                                else dividerColor,
                                shape = CircleShape
                            )
                    )
                }
            }

            // 다음/시작하기 버튼: 높이만 고정
            Button(
                onClick = {
                    scope.launch {
                        val next = pagerState.currentPage + 1
                        if (next < pages.size) {
                            pagerState.animateScrollToPage(next)
                        } else {
                            onFinish()
                        }
                    }
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage < pages.lastIndex) "다음" else "시작하기",
                    color = Color.White
                )
            }
        }
    }
}













