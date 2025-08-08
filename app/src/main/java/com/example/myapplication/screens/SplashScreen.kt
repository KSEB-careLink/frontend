// SplashScreen.kt
package com.example.myapplication.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    LaunchedEffect(Unit) {
        // 순수 스플래시만 보여주고 바로 choose로 이동 (자동 로그인 제거)
        delay(1200)
        navController.navigate("choose") {
            popUpTo("splash") { inclusive = true }
        }
    }

    // ===== 기존 애니메이션 UI 유지 =====
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        val screenW = maxWidth
        val screenH = maxHeight
        val logoSize = screenW * 0.5f
        val textSize = screenW * 0.3f
        val logoY = screenH * 0.35f
        val textY = screenH * 0.50f

        Box(modifier = Modifier.fillMaxSize()) {
            FadeScaleLogo(
                modifier = Modifier
                    .size(logoSize)
                    .align(Alignment.TopCenter)
                    .offset(y = logoY)
            )
            FadeInAiText(
                modifier = Modifier
                    .size(textSize)
                    .align(Alignment.TopCenter)
                    .offset(y = textY),
                delayMillis = 500,
                durationMillis = 1000
            )
        }
    }
}

@Composable
fun FadeScaleLogo(
    modifier: Modifier = Modifier,
    durationMillis: Int = 1000
) {
    var start by remember { mutableStateOf(false) }
    val transition = updateTransition(start, label = "logoTransition")
    val alpha by transition.animateFloat(
        transitionSpec = { tween(durationMillis = durationMillis) },
        label = "alpha"
    ) { state -> if (state) 1f else 0f }
    val scale by transition.animateFloat(
        transitionSpec = { tween(durationMillis = durationMillis, easing = FastOutSlowInEasing) },
        label = "scale"
    ) { state -> if (state) 1f else 0.5f }

    LaunchedEffect(Unit) { start = true }

    Image(
        painter = painterResource(id = R.drawable.rogo),
        contentDescription = "Logo",
        contentScale = ContentScale.Fit,
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.scaleX = scale
            this.scaleY = scale
        }
    )
}

@Composable
fun FadeInAiText(
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    durationMillis: Int = 800
) {
    var visible by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = durationMillis)
    )

    LaunchedEffect(Unit) {
        delay(delayMillis.toLong())
        visible = true
    }

    Image(
        painter = painterResource(id = R.drawable.ai_text),
        contentDescription = "AI Text",
        contentScale = ContentScale.Fit,
        modifier = modifier.graphicsLayer { this.alpha = alpha }
    )
}

@Preview(showBackground = true)
@Composable
fun SplashScreenPreview() {
    SplashScreen(navController = rememberNavController())
}














