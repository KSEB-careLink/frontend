// OnboardingScreen.kt
package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.R
import kotlinx.coroutines.launch

data class OnboardPage(val title: String, val drawableRes: Int)

@Composable
fun OnboardingScreen(
    navController: NavController,
    patientId: String,
    onFinish: () -> Unit
) {
    // 튜토리얼 페이지 리스트
    val pages = listOf(
        OnboardPage("오렌지!", R.drawable.ic_1),
        OnboardPage("레몬!",   R.drawable.ic_3),
        OnboardPage("수박!",   R.drawable.ic_4)
    )

    // 페이저 상태
    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size }
    )
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.weight(1f))

        // 이미지 + 텍스트 페이저
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(4f)
        ) { page ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = pages[page].drawableRes),
                    contentDescription = pages[page].title,
                    modifier = Modifier.size(240.dp)
                )
                Spacer(Modifier.height(16.dp))
                Text(text = pages[page].title)
            }
        }

        // ●○ 인디케이터
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            repeat(pages.size) { idx ->
                Text(if (idx == pagerState.currentPage) "●" else "○")
            }
        }

        Spacer(Modifier.weight(1f))

        // 이전 / 다음(또는 시작하기) 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // 이전 버튼 (첫 페이지에서는 비활성)
            TextButton(
                onClick = {
                    if (pagerState.currentPage > 0) {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                        }
                    }
                },
                enabled = pagerState.currentPage > 0
            ) {
                Text("이전")
            }

            // 다음 또는 시작하기
            Button(onClick = {
                scope.launch {
                    if (pagerState.currentPage < pages.lastIndex) {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    } else {
                        // 마지막 페이지에서 완료 콜백 호출
                        onFinish()
                    }
                }
            }) {
                Text(if (pagerState.currentPage < pages.lastIndex) "다음" else "시작하기")
            }
        }
    }
}

