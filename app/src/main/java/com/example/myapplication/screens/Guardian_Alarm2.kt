// app/src/main/java/com/example/myapplication/screens/GuardianAlarm2.kt
package com.example.myapplication.screens

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.myapplication.R
import com.example.myapplication.receiver.AlarmReceiver
import com.example.myapplication.viewmodel.OneTimeAlarmViewModel
import java.util.Calendar

/**
 * dateStr = "YYYY-M-D"
 * content = 알림에 보여줄 문자열
 * hour/minute = 알림 시각
 * id = PendingIntent requestCode (일정 인덱스 사용 권장)
 */
private fun scheduleOneTimeAlarm(
    context: Context,
    id: Int,
    dateStr: String,
    content: String,
    hour: Int,
    minute: Int
) {
    // 날짜 파싱
    val parts = dateStr.split("-").map { it.toInt() }
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, parts[0])
        set(Calendar.MONTH, parts[1] - 1)
        set(Calendar.DAY_OF_MONTH, parts[2])
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }

    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    // Android 12+ 에서 정확 알람 권한 없으면 넘김
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
        // TODO: 사용자에게 설정 화면 안내 등 대체 로직 수행
        return
    }

    val intent = Intent(context, AlarmReceiver::class.java).apply {
        putExtra("id", id)
        putExtra("content", content)
    }
    val pi = PendingIntent.getBroadcast(
        context, id, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    try {
        // 정확 알람 시도
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    } catch (se: SecurityException) {
        // 권한 없거나 실패 시 일반 알람으로 폴백
        am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }
}

@Composable
fun Guardian_Alarm2(navController: NavController) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Activity-scoped ViewModel
    val activity = LocalActivity.current as ComponentActivity
    val viewModel: OneTimeAlarmViewModel =
        viewModel(viewModelStoreOwner = activity)

    Scaffold(
        bottomBar = { /* ...bottomBar 정의... */ }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)  // Scaffold 패딩 적용
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Image(
                painter = painterResource(R.drawable.rogo),
                contentDescription = "로고",
                modifier = Modifier.size(200.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(
                "비정기 알림 설정",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))
            Text(
                "가족 일정이나 병원 일정을\n날짜와 시간을 지정해 설정해 주세요!",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            // 날짜 선택
            Button(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, y, m, d ->
                            viewModel.onDateSelected("$y-${m + 1}-$d")
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("날짜 선택", fontSize = 18.sp, color = Color.White)
            }

            // 입력/수정 영역
            if (viewModel.showInputArea) {
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = viewModel.scheduleText,
                    onValueChange = viewModel::onTextChanged,
                    label = { Text("일정을 입력하세요") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // 시간 선택 Spinner 스타일 테마를 지정해서 휠 피커 모드로 띄운다
                TimePickerDialog(
                    context,
                    R.style.SpinnerTimePickerDialog,
                    { _, h, m -> viewModel.onTimeSelected(h, m) },
                    viewModel.selectedHour,
                    viewModel.selectedMinute,
                    /* 24시간제 여부 */ false   // ← false 면 AM/PM 휠 자동
                ).show()

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.addSchedule()
                        val idx = viewModel.schedules.lastIndex
                        val item = viewModel.schedules[idx]
                        scheduleOneTimeAlarm(
                            context = context,
                            id = idx,
                            dateStr = item.date,
                            content = item.content,
                            hour = item.hour,
                            minute = item.minute
                        )
                    },
                    modifier = Modifier
                        .width(150.dp)
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                ) {
                    Text(
                        text = if (viewModel.editingIndex != null) "수정 완료" else "등록하기",
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 등록된 일정 리스트
            if (viewModel.schedules.isNotEmpty()) {
                Text("등록된 일정", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(viewModel.schedules) { idx, item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "- ${item.date} %02d:%02d".format(item.hour, item.minute) +
                                        " : ${item.content}",
                                fontSize = 14.sp
                            )
                            Row {
                                Button(
                                    onClick = { viewModel.editSchedule(idx) },
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                                ) {
                                    Text("수정", fontSize = 9.sp, color = Color.White)
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.deleteSchedule(idx)
                                        val intent = Intent(context, AlarmReceiver::class.java)
                                        val pi = PendingIntent.getBroadcast(
                                            context, idx, intent,
                                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                                        )
                                        (context.getSystemService(Context.ALARM_SERVICE)
                                                as AlarmManager)
                                            .cancel(pi)
                                    },
                                    modifier = Modifier.height(30.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Text("삭제", fontSize = 9.sp, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}








