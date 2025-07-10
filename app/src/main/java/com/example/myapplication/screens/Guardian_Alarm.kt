package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R

@Composable
fun Guardian_Alarm(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // 1) 로고
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2) 타이틀
        Text(
            text = "알림 전달",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 3) 정기 알림 버튼
        Button(
            onClick = { /* TODO: 정기 알림 설정 화면으로 */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("정기 알림", fontSize = 18.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4) 정기 알림 설명
        Text(
            text = "매일 보낼 알림 내용을 설정해 주세요!\n(예시: 약 복용 알림, 운동 알림, 아침 인사)",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 5) 설정된 정기 알림 목록
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AlertItem(number = 1, text = "약 드실 시간이에요!") {
                /* TODO: 시간 설정 다이얼로그 */
            }
            AlertItem(number = 2, text = "좋은 아침이에요!") {
                /* TODO: 시간 설정 다이얼로그 */
            }
        }

        Spacer(modifier = Modifier.height(150.dp))

        // 6) 비정기 알림 버튼
        Button(
            onClick = { /* TODO: 비정기 알림 설정 화면으로 */ },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("비정기 알림", fontSize = 18.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 7) 비정기 알림 설명
        Text(
            text = "가족 일정이나 병원 일정을 설정해 주세요!",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color.Black,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 8) 등록하기 버튼
        Button(
            onClick = { /* TODO: 등록 로직 */ },
            modifier = Modifier
                .width(150.dp)
                .height(44.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("등록하기", fontSize = 16.sp, color = Color.White)
        }
    }
}

@Composable
private fun AlertItem(
    number: Int,
    text: String,
    onTimeClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$number. $text",
            fontSize = 18.sp,
            color = Color.Black
        )
        Button(
            onClick = onTimeClick,
            modifier = Modifier
                .height(36.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("시간 설정", fontSize = 14.sp, color = Color.White)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun Preview_Guardian_Alarm() {
    Guardian_Alarm(navController = rememberNavController())
}

