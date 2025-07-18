package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.compose.ui.graphics.Color

@Composable
fun ChoosePositionPage(navController: NavController) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        val (
            logoImage, textImage, elderButton, guardianButton
        ) = createRefs()

        // 1) 로고 이미지
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(160.dp)
                .constrainAs(logoImage) {
                    top.linkTo(parent.top, margin = 220.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        // 2) 텍스트 로고 이미지
        Image(
            painter = painterResource(id = R.drawable.ai_text),
            contentDescription = "텍스트 로고",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(120.dp)
                .constrainAs(textImage) {
                    top.linkTo(logoImage.bottom, margin = -32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        // 3) 어르신 버튼
        Button(
            onClick = { navController.navigate("p_login") },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(76.dp)
                .constrainAs(elderButton) {
                    top.linkTo(textImage.bottom, margin = 1.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("🧓 어르신으로 시작하기", color = Color.White, fontSize = 16.sp)
        }

        // 4) 보호자 버튼
        Button(
            onClick = { navController.navigate("G_login") },
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(76.dp)
                .constrainAs(guardianButton) {
                    top.linkTo(elderButton.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Black)
        ) {
            Text("👪 보호자로 시작하기", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewChoosePositionPage() {
    ChoosePositionPage(navController = rememberNavController())
}














