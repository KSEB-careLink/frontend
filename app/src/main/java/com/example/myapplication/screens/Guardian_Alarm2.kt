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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.myapplication.R
import com.example.myapplication.receiver.AlarmReceiver
import com.example.myapplication.viewmodel.OneTimeAlarmViewModel
import androidx.compose.foundation.border
import java.util.Calendar
import android.util.Log
import android.provider.Settings
import android.net.Uri

/**
 * 지정한 날짜·시간에 한 번만 울리는 알람을 예약합니다.
 */
private fun scheduleOneTimeAlarm(
    context: Context,
    id: Int,
    dateStr: String,
    content: String,
    hour: Int,
    minute: Int
) {
    val parts = dateStr.split("-").map { it.toInt() }
    val cal = Calendar.getInstance().apply {
        set(Calendar.YEAR, parts[0])
        set(Calendar.MONTH, parts[1] - 1)
        set(Calendar.DAY_OF_MONTH, parts[2])
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }

    if (cal.timeInMillis <= System.currentTimeMillis()) {
        Log.e("AlarmDebug", "⛔ 과거 시간으로 알람이 설정됨. 등록 취소됨.")
        return
    }

    val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (!alarmManager.canScheduleExactAlarms()) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) {
        // 정확 알람 권한이 없으면 예약하지 않습니다.
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
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    } catch (_: SecurityException) {
        am.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
    }
}

@Composable
fun Guardian_Alarm2(navController: NavController) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Activity 범위 ViewModel
    val activity = LocalActivity.current as ComponentActivity
    val viewModel: OneTimeAlarmViewModel =
        viewModel(viewModelStoreOwner = activity)

    // Coroutine scope
    val scope = rememberCoroutineScope()

    // BottomBar 용 현재 route
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(

    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))

            // 로고
            Image(
                painter = painterResource(R.drawable.rogo),
                contentDescription = "로고",
                modifier = Modifier.size(200.dp)
            )
            Spacer(Modifier.height(16.dp))

            // 제목
            Text(
                "비정기 알림 설정",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(24.dp))

            // 설명
            Text(
                "가족 일정이나 병원 일정을\n날짜와 시간을 지정해 설정해 주세요!",
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(24.dp))

            // 날짜 선택 버튼
            Button(
                onClick = {
                    DatePickerDialog(
                        context,
                        { _, year, month, day ->
                            viewModel.onDateSelected("$year-${month + 1}-$day")
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

            Spacer(Modifier.height(16.dp))

            // 텍스트 및 시간 선택
            if (viewModel.showInputArea) {
                OutlinedTextField(
                    value = viewModel.scheduleText,
                    onValueChange = viewModel::onTextChanged,
                    label = { Text("일정을 입력하세요") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                // TimePickerDialog
                Button(
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hour, minute ->
                                viewModel.onTimeSelected(hour, minute)
                            },
                            viewModel.selectedHour,
                            viewModel.selectedMinute,
                            false // 12시간제
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
                ) {
                    Text(
                        "시간 선택: %02d:%02d".format(
                            viewModel.selectedHour,
                            viewModel.selectedMinute
                        ),
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(8.dp))

                // 등록/수정 버튼
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
                        if (viewModel.editingIndex != null) "수정 완료" else "등록하기",
                        color = Color.White
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // 등록된 일정 리스트
            if (viewModel.schedules.isNotEmpty()) {
                Text("등록된 일정", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 300.dp)        // 최대 300dp 높이
                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(viewModel.schedules) { idx, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "- ${item.date} %02d:%02d".format(
                                        item.hour,
                                        item.minute
                                    ) +
                                            " : ${item.content}",
                                    fontSize = 14.sp
                                )
                                Row {
                                    Button(
                                        onClick = { viewModel.editSchedule(idx) },
                                        modifier = Modifier.height(30.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(
                                                0xFF00C4B4
                                            )
                                        )
                                    ) {
                                        Text("수정", fontSize = 9.sp, color = Color.White)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.deleteSchedule(idx)
                                            // 알람 취소 로직...
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
}














