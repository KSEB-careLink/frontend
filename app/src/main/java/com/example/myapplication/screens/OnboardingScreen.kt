// app/src/main/java/com/example/myapplication/screens/OnboardingScreen.kt
package com.example.myapplication.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset as GOffset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.R
import kotlinx.coroutines.launch

/**
 * MemoryInfoInputScreen 의 UI 파트만 떼어낸 Composable.
 * 실제 폼 UI(사진 선택, 카테고리, 질문/응답 필드 등)는 이곳에
 * 원본대로 복사·붙여넣으시면 됩니다.
 */
@Composable
fun MemoryInfoFormContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(24.dp)
    ) {
        Text("회상정보 입력", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(16.dp))
        // TODO: 여기부터 원본 MemoryInfoInputScreen 의
        // Image 선택 UI, 카테고리 드롭다운, 질문/응답 TextField 등을
        // “생략 없이” 모두 옮겨오세요.
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    patientId: String,
    onFinish: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    // 페이지 수를 9개로 설정 (0:폼 + 1~8: 튜토리얼)
    val pagesCount = 9
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pagesCount })
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxSize()) {
        // 1) Pager (weight = 1f 으로 상단 전체 차지)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) { page ->
            when (page) {
                0 -> MemoryInfoFormContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                )
                1 -> FruitTutorialPage("레몬!", R.drawable.ic_1, Modifier.fillMaxSize())
                2 -> FruitTutorialPage("수박!", R.drawable.ic_1, Modifier.fillMaxSize())
                3 -> FruitTutorialPage("사과!", R.drawable.ic_1, Modifier.fillMaxSize())
                4 -> FruitTutorialPage("바나나!", R.drawable.ic_1, Modifier.fillMaxSize())
                5 -> FruitTutorialPage("포도!", R.drawable.ic_1, Modifier.fillMaxSize())
                6 -> FruitTutorialPage("오렌지!", R.drawable.ic_1, Modifier.fillMaxSize())
                7 -> FruitTutorialPage("체리!", R.drawable.ic_1, Modifier.fillMaxSize())
                8 -> FruitTutorialPage("복숭아!", R.drawable.ic_1, Modifier.fillMaxSize())
            }
        }

        // 2) 점선 구분선 (살짝 위로 이동)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .offset(y = (-4).dp)
        ) {
            val strokePx = with(density) { 2.dp.toPx() }
            drawLine(
                color = dividerColor,
                start = GOffset.Zero,
                end = GOffset(size.width, 0f),
                strokeWidth = strokePx,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        // 3) 하단 네비게이션 버튼 & 인디케이터
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
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

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(pagesCount) { idx ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (idx == pagerState.currentPage) primaryColor else dividerColor,
                                shape = CircleShape
                            )
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    scope.launch {
                        if (pagerState.currentPage < pagesCount - 1)
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        else
                            onFinish()
                    }
                },
                shape = CircleShape,
                border = BorderStroke(2.dp, primaryColor),
                modifier = Modifier.height(48.dp)
            ) {
                Text(
                    text = if (pagerState.currentPage < pagesCount - 1) "다음" else "시작하기",
                    color = primaryColor
                )
            }
        }
    }
}

@Composable
fun FruitTutorialPage(
    title: String,
    drawableRes: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(text = title, style = MaterialTheme.typography.titleLarge)
    }
}




















