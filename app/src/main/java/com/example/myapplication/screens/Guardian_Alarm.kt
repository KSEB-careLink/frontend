package com.example.myapplication.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import java.util.Calendar

data class ScheduleItem(val date: String, val content: String)

@Composable
fun Guardian_Alarm(navController: NavController) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    var selectedDate by remember { mutableStateOf("") }
    var scheduleText by remember { mutableStateOf(TextFieldValue("")) }
    var schedules by remember { mutableStateOf(listOf<ScheduleItem>()) }
    var showInputArea by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            modifier = Modifier.size(200.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "알림 전달",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("정기 알림", fontSize = 18.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "매일 보낼 알림 내용을 설정해 주세요!\n(예시: 약 복용 알림, 운동 알림, 아침 인사)",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AlertItem(1, "약 드실 시간이에요!") {}
            AlertItem(2, "좋은 아침이에요!") {}
        }

        Spacer(modifier = Modifier.height(150.dp))

        Button(
            onClick = {
                val datePicker = DatePickerDialog(
                    context,
                    { _, year, month, day ->
                        selectedDate = "$year-${month + 1}-$day"
                        showInputArea = true
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH)
                )
                datePicker.show()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("비정기 알림", fontSize = 18.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "가족 일정이나 병원 일정을 설정해 주세요!",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        if (showInputArea && selectedDate.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = scheduleText,
                onValueChange = { scheduleText = it },
                label = { Text("일정을 입력하세요") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (scheduleText.text.isNotBlank()) {
                        schedules = schedules + ScheduleItem(selectedDate, scheduleText.text)
                        scheduleText = TextFieldValue("")
                        selectedDate = ""
                        showInputArea = false
                    }
                },
                modifier = Modifier
                    .width(150.dp)
                    .height(44.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("등록하기", fontSize = 16.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (schedules.isNotEmpty()) {
            Text("등록된 일정", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(schedules) { item ->
                    Text("- ${item.date} : ${item.content}", fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun AlertItem(number: Int, text: String, onTimeClick: () -> Unit) {
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
            modifier = Modifier.height(36.dp),
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
