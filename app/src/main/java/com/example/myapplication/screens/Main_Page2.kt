package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import com.example.myapplication.R

@Composable
fun Main_Page2(navController: NavController) {
    val buttonColor = Color(0xFF2ECCD1)

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        val (logo, btn1, btn2, btn3, btn4, btn5, btn6) = createRefs()

        // 로고
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "CareLink 로고",
            modifier = Modifier
                .size(200.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 80.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        // 버튼 공통 Modifier
        val buttonModifier = Modifier
            .fillMaxWidth()
            .height(56.dp)

        // 각 버튼
        Button(
            onClick = { navController.navigate("guardian_basic_info") },
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .constrainAs(btn1) {
                    top.linkTo(logo.bottom, margin = 30.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(56.dp)
        ) {
            Text("기본 정보 입력", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Button(
            onClick = { navController.navigate("memoryinfo") },
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .constrainAs(btn2) {
                    top.linkTo(btn1.bottom, margin = 20.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(56.dp)
        ) {
            Text("회상 정보 입력", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Button(
            onClick = { navController.navigate("memorylist") },
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .constrainAs(btn3) {
                    top.linkTo(btn2.bottom, margin = 20.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(56.dp)
        ) {
            Text("회상 정보 데이터 확인", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Button(
            onClick = { navController.navigate("alarm") },
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .constrainAs(btn4) {
                    top.linkTo(btn3.bottom, margin = 20.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(56.dp)
        ) {
            Text("알림 전달", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Button(
            onClick = { navController.navigate("Recode") },
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .constrainAs(btn5) {
                    top.linkTo(btn4.bottom, margin = 20.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(56.dp)
        ) {
            Text("보호자 음성 등록", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }

        Button(
            onClick = { navController.navigate("location") },
            colors = ButtonDefaults.buttonColors(containerColor = buttonColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .constrainAs(btn6) {
                    top.linkTo(btn5.bottom, margin = 20.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom, margin = 20.dp)
                    width = Dimension.fillToConstraints
                }
                .height(56.dp)
        ) {
            Text("위치 확인", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}




