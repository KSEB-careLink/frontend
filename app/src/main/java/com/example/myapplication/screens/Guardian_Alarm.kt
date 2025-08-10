package com.example.myapplication.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.service.NotificationService
import java.util.*

//알림 POST요청 보낼때 필요한 임포트
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.example.myapplication.network.RetrofitInstance
import com.example.myapplication.network.AlarmApi

import com.example.myapplication.network.AlarmRequest

import android.util.Log
import kotlinx.coroutines.Dispatchers

// 알림 보낼 때 필요한 인증처리 import
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await


@Composable
fun Guardian_Alarm(navController: NavController, patientId: String) {
    val context = LocalContext.current
    //여기 FCM토큰 이용한 알림 보낼때 필요한 두 줄
    val scope = rememberCoroutineScope()
    val alarmApi = RetrofitInstance.api  // RetrofitInstance가 미리 정의돼 있어야 합니다



    // 보호자 알림 설정
    var time1 by remember { mutableStateOf<Calendar?>(null) }
    var time2 by remember { mutableStateOf<Calendar?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.Center, //화면 세로 중앙 정렬
        horizontalAlignment = Alignment.CenterHorizontally// 화면 가로 중앙 정렬
    ) {
        Text("❤보호자 정기 알림", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(32.dp))

        TimeSelector(label = "시간 1 설정") { time1 = it }
        Spacer(modifier = Modifier.height(16.dp))
        TimeSelector(label = "시간 2 설정") { time2 = it }
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                time1?.let { cal ->
                    val label = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                    NotificationService.scheduleNotification(
                        context,
                        cal.timeInMillis,
                        "첫 번째 알림: $label",
                        showToast = true
                    )
                }
                time2?.let { cal ->
                    val label = String.format("%02d:%02d", cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
                    NotificationService.scheduleNotification(
                        context,
                        cal.timeInMillis,
                        "두 번째 알림: $label",
                        showToast = true
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("보호자 알림 예약", fontSize = 18.sp, color = Color.White)
        }

        Spacer(modifier = Modifier.height(48.dp))

        // 환자 알림 설정 UI
        Text("🩺 환자 정기 알림", fontSize = 24.sp)
        Spacer(modifier = Modifier.height(32.dp))

        var pTime1 by remember { mutableStateOf<Calendar?>(null) }
        var pTime2 by remember { mutableStateOf<Calendar?>(null) }

        TimeSelector(label = "시간 1 설정") { pTime1 = it }
        Spacer(modifier = Modifier.height(16.dp))
        TimeSelector(label = "시간 2 설정") { pTime2 = it }
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
                // 선택된 두 시간 중 null 아닌 것만 리스트로
                listOfNotNull(pTime1, pTime2).forEach { baseCal ->
                    // 0분, 10분, 20분 오프셋 적용 → 총 3회
                    repeat(3) { i ->
                        //cal 정의
                        val cal = (baseCal.clone() as Calendar).apply {
                            add(Calendar.MINUTE, i * 10)
                        }
                        val timeStr = String.format(
                            "%02d:%02d",
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE)
                        )
                        // POST body 준비
                        val request = AlarmRequest(
                            patientId = patientId,
                            message   = "오늘도 회상 퀴즈 풀며 기억해봐요! ($timeStr)",
                            time      = timeStr
                        )

                        // 비동기로 API 호출
                        scope.launch(Dispatchers.IO) {
                            try {
                                val res = alarmApi.postAlarm(request) // 헤더 자동 첨부됨

                                if (res.isSuccessful) {
                                    Log.d("환자알림예약", "성공: $timeStr")
                                } else {
                                    Log.e(
                                        "환자알림예약",
                                        "실패 code=${res.code()} body=${res.errorBody()?.string()}"
                                    )
                                }

                                //  로그인 토큰 가져오기 (강제 갱신)
                               // val idToken = Firebase.auth.currentUser
                                  //  ?.getIdToken(true)?.await()?.token
                                  //  ?: throw Exception("ID 토큰이 null입니다. 로그인 상태를 확인하세요.")


                              //  if (res.isSuccessful) {  //  토큰을 Authorization 헤더에 붙여서 호출
                                  //  val res = alarmApi.postAlarm(request, "Bearer $idToken")

                                 //   Log.d("환자알림예약", "성공: $timeStr")
                             //   } else {
                              //      Log.e(
                               //         "환자알림예약",
                               //         "실패 code=${res.code()} body=${res.errorBody()?.string()}"
                               //     )
                               // }

                            } catch (e: Exception) {
                                Log.e("환자알림예약", "예외 발생", e)
                            }
                        }
                    }
                }


            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("환자 알림 예약", fontSize = 18.sp, color = Color.White)
        }
    }
}

@Composable
fun TimeSelector(label: String, onTimeSelected: (Calendar) -> Unit) {
    val context = LocalContext.current
    var selectedTime by remember { mutableStateOf("") }

    Button(
        onClick = {
            val now = Calendar.getInstance()
            TimePickerDialog(
                context,
                { _, hour, minute ->
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    selectedTime = String.format("%02d:%02d", hour, minute)
                    onTimeSelected(cal)
                },
                now.get(Calendar.HOUR_OF_DAY),
                now.get(Calendar.MINUTE),
                true
            ).show()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
    ) {
        Text(
            text = "$label${if (selectedTime.isNotEmpty()) " ($selectedTime)" else ""}",
            fontSize = 16.sp,
            color = Color.White
        )
    }
}
