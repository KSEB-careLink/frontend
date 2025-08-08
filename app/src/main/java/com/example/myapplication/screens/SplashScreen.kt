// SplashScreen.kt
package com.example.myapplication.screens

import android.content.Context
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import kotlinx.coroutines.delay
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState

@Composable
fun SplashScreen(navController: NavController) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(3000)
        val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val patientId = prefs.getString("patientId", null)

        if (patientId != null) {
            navController.navigate("sentence/$patientId") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("choose") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

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












