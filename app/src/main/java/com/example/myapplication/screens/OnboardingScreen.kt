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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.R
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.text.style.TextAlign

/**
 * 1번 페이지: 개인정보 이용 동의 화면
 */
@Composable
fun ConsentScreen(modifier: Modifier = Modifier) {
    val scroll = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)     // 좌우 여백만 주고
            .verticalScroll(scroll)          // 세로 스크롤 허용
            .navigationBarsPadding(),        // 하단 제스처바 피하기
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(84.dp))

        // 1) 상단 스크린샷: 화면이 작아도 너무 커지지 않게 최대 높이 제한
        Image(
            painter = painterResource(id = R.drawable.ic_0),
            contentDescription = "개인정보 이용 안내",
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 360.dp),     // ← 여기로 크기 캡
            contentScale = ContentScale.Fit
        )

        Spacer(Modifier.height(24.dp))

        // 2) 타이틀
        Text(
            text = "위의 사진과 같은 문제를 \n 생성합니다.",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        // 3) 정보 수집 항목 카드
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "본 앱에서는 다음 정보를 수집·이용합니다",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(Modifier.height(12.dp))
                Text("• 사진 및 메모리 정보", style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Text("• 환자 프로필 정보", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(20.dp))

        // 4) 동의 질문
        Text(
            text = "위 개인정보 수집·이용에 동의하십니까?",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(24.dp))
    }
}


/**
 * 2~7번 페이지: 튜토리얼 페이지
 */
@Composable
fun TutorialPage(
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navController: NavController,
    patientId: String,
    onFinish: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val dividerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

    // 페이지 수: 0=Consent, 1~7=Tutorial
    val pagesCount = 8
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { pagesCount })
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 32.dp)
        ) { page ->
            when (page) {
                0 -> ConsentScreen(Modifier.fillMaxSize())
                1 -> TutorialPage("회상정보 입력 페이지의 첫 화면 입니다!", R.drawable.ic_1, Modifier.fillMaxSize())
                2 -> TutorialPage("사진 선택 버튼을 눌러 사진을 업로드 하세요.", R.drawable.ic_2, Modifier.fillMaxSize())
                3 -> TutorialPage("사진을 선택하세요.", R.drawable.ic_3, Modifier.fillMaxSize())
                4 -> TutorialPage("회상정보의 카테고리를 정하세요.", R.drawable.ic_4, Modifier.fillMaxSize())
                5 -> TutorialPage("회상정보를 모두 기입하세요.", R.drawable.ic_5, Modifier.fillMaxSize())
                6 -> TutorialPage("회상 정보 업로드 버튼을 누르세요.", R.drawable.ic_6, Modifier.fillMaxSize())
                7 -> TutorialPage("회상정보를 9번 입력하고 앱을 이용해 보세요!", R.drawable.ic_7, Modifier.fillMaxSize())
            }
        }

        // 점선 구분선
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .offset(y = (-4).dp)
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

        // 하단 내비게이션
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
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

            // 인디케이터
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
                        if (pagerState.currentPage < pagesCount - 1) {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        } else {
                            // 마지막 페이지에서 onFinish 호출
                            onFinish()
                        }
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
























