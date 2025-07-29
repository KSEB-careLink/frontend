package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
fun Recode(
    navController: NavController,
    patientId: String,
    voices: List<String>,
    onSelectVoice: (String) -> Unit
) {
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val (
            logo, title, selectButton, voiceListBox, bottomButton
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
            text = "보호자 음성 등록",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(logo.bottom, margin = 24.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        // 3) 목소리 선택 버튼
        Button(
            onClick = { /* TODO: 파일 선택 로직 */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(selectButton) {
                    top.linkTo(title.bottom, margin = 32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("목소리 선택", color = Color.White, fontSize = 16.sp)
        }

        // 4) 음성 리스트 박스
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                .padding(8.dp)
                .constrainAs(voiceListBox) {
                    top.linkTo(selectButton.bottom, margin = 24.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(bottomButton.top, margin = 32.dp)
                    height = Dimension.preferredWrapContent
                }
        ) {
            voices.forEachIndexed { index, name ->
                Text(
                    text = "${index + 1}. $name",
                    fontSize = 16.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectVoice(name) }
                        .padding(vertical = 8.dp)
                )
                if (index < voices.lastIndex) Divider()
            }
        }

        // 5) 하단 녹음 버튼
        Button(
            onClick = { navController.navigate("recode2") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(bottomButton) {
                    bottom.linkTo(parent.bottom, margin = 40.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("녹음하러 가기", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewRecode() {
    val dummy = listOf("기본", "A", "B")
    Recode(
        navController = rememberNavController(),
        patientId = "dummyId", // ✅ 여기에 추가
        voices = dummy,
        onSelectVoice = { /* TODO */ }
    )
}
