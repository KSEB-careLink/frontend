package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R

@Composable
fun GuardianBasicInfoScreen() {
    var name by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf("다정한") }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        val (
            logo, title, nameLabel, nameInput,
            birthdayLabel, birthdayInput,
            question1, relationshipInput,
            question2, subText, toneButtons, submitButton
        ) = createRefs()

        // 상단 로고
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(80.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        // 제목
        Text(
            "기본 정보 입력",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(logo.bottom, margin = 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        // 이름 라벨
        Text(
            "이름",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(nameLabel) {
                top.linkTo(title.bottom, margin = 32.dp)
                start.linkTo(parent.start)
            }
        )

        // 이름 입력
        TextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("홍길동") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .constrainAs(nameInput) {
                    top.linkTo(nameLabel.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(56.dp)
        )

        // 생일 라벨
        Text(
            "생일",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(birthdayLabel) {
                top.linkTo(nameInput.bottom, margin = 24.dp)
                start.linkTo(parent.start)
            }
        )

        // 생일 입력
        TextField(
            value = birthday,
            onValueChange = { birthday = it },
            placeholder = { Text("예: 1990-01-01") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .constrainAs(birthdayInput) {
                    top.linkTo(birthdayLabel.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(56.dp)
        )

        // 질문 1: 호칭
        Text(
            "1. 보호 대상을 부르는 호칭을 알려주세요",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(question1) {
                top.linkTo(birthdayInput.bottom, margin = 24.dp)
                start.linkTo(parent.start)
            }
        )

        // 호칭 입력
        TextField(
            value = relationship,
            onValueChange = { relationship = it },
            placeholder = { Text("기타") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .constrainAs(relationshipInput) {
                    top.linkTo(question1.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(56.dp)
        )

        // 질문 2: 말투
        Text(
            "2. 원하는 말투",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(question2) {
                top.linkTo(relationshipInput.bottom, margin = 32.dp)
                start.linkTo(parent.start)
            }
        )

        // 서브텍스트
        Text(
            "(의상 문장 생성, 보호자 음성에 이용)",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.constrainAs(subText) {
                top.linkTo(question2.bottom, margin = 4.dp)
                start.linkTo(parent.start)
            }
        )

        // 말투 선택 버튼들
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .constrainAs(toneButtons) {
                    top.linkTo(subText.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
        ) {
            listOf("다정한", "밝은", "차분한").forEach { option ->
                val selected = tone == option
                Button(
                    onClick = { tone = option },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFF9C27B0) else Color(0xFFE1BEE7),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(option)
                }
            }
        }

        // 완료 버튼
        Button(
            onClick = {
                // TODO: name, birthday, relationship, tone 처리 로직 작성
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DD0E1)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .constrainAs(submitButton) {
                    top.linkTo(toneButtons.bottom, margin = 40.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(48.dp)
        ) {
            Text("완료", color = Color.White, fontSize = 16.sp)
        }
    }
}


