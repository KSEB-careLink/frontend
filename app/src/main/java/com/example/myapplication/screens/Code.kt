package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R

@Composable
fun Code(navController: NavController) {
    val codeLength = 5

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val (
            logo, title, codeRow, expirationText, button
        ) = createRefs()

        // 1) 로고
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            modifier = Modifier
                .size(200.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 80.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        // 2) 타이틀
        Text(
            text = "코드 주고 받기",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(logo.bottom, margin = 24.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        // 3) 코드 박스들
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.constrainAs(codeRow) {
                top.linkTo(title.bottom, margin = 32.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ) {
            repeat(codeLength) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = Color(0xFF333333),
                            shape = RoundedCornerShape(8.dp)
                        )
                )
            }
        }

        // 4) 유효기간 텍스트
        Text(
            text = "코드 유효 기간 xx/xx/xx",
            fontSize = 14.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(expirationText) {
                top.linkTo(codeRow.bottom, margin = 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        // 5) 하단 버튼
        Button(
            onClick = { navController.navigate("main") },
            modifier = Modifier
                .height(76.dp)
                .constrainAs(button) {
                    bottom.linkTo(parent.bottom, margin = 250.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("보호자 pin 코드 입력", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewCodeExchangePage() {
    Code(navController = rememberNavController())
}



