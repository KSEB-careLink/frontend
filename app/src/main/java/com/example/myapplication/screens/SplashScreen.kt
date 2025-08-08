package com.example.myapplication.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import kotlinx.coroutines.delay
import androidx.compose.animation.core.*

import androidx.compose.ui.platform.LocalContext
import com.example.myapplication.service.NotificationService
import com.google.firebase.messaging.FirebaseMessaging

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current // ✅ context 가져오기
    // 3초 뒤 메인 화면으로 이동
    LaunchedEffect(Unit) {

        //  FCM 토큰 요청 및 서버 전송
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                Log.d("FCM", "토큰: $token")
                NotificationService.sendFcmTokenToServer(context, token)
            } else {
                Log.e("FCM", "토큰 획득 실패", task.exception)
            }
        }


        delay(3000)
        navController.navigate("choose") {
            popUpTo("splash") { inclusive = true }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenW = maxWidth
        val screenH = maxHeight
        val rogoSize = screenW * 0.5f
        val textSize = screenW * 0.3f
        val rogoY = screenH * 0.35f
        val textY = screenH * 0.50f

        Box(modifier = Modifier.fillMaxSize()) {
            // 로고: 페이드 + 스케일
            FadeScaleLogo(
                modifier = Modifier
                    .size(rogoSize)
                    .align(Alignment.TopCenter)
                    .offset(y = rogoY)
            )
            // AI 텍스트: 500ms 딜레이 후 페이드-인
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
    val dummyNavController = rememberNavController()
    SplashScreen(navController = dummyNavController)
}











