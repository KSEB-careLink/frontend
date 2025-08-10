//package com.example.myapplication.screens
//
//import android.app.DatePickerDialog
//import android.app.TimePickerDialog
//import android.content.Context
//import android.util.Log
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.LocalActivity
//import androidx.compose.foundation.Image
//import androidx.compose.foundation.border
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.itemsIndexed
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.res.painterResource
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import androidx.lifecycle.viewmodel.compose.viewModel
//import androidx.navigation.NavController
//import androidx.navigation.compose.currentBackStackEntryAsState
//import com.example.myapplication.R
//import com.example.myapplication.viewmodel.OneTimeAlarmViewModel
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import okhttp3.*
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.json.JSONObject
//import java.io.IOException
//import java.util.*
//import com.example.myapplication.BuildConfig
//
//fun getTokenFromPrefs(context: Context): String {
//    val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
//    return prefs.getString("jwt_token", "") ?: ""
//}
//
////fun getPatientIdFromPrefs(context: Context): String {
////    val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
////    return prefs.getString("patient_id", "") ?: ""
////}
//
//fun postAlarmToServer(
//    context: Context,
//    patientId: String,
//    message: String,
//    time: String,
//    token: String,
//    scope: CoroutineScope
//) {
//    val json = JSONObject().apply {
//        put("patientId", patientId)
//        put("message", message)
//        put("time", time)
//    }
//
//    val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
//
//    val request = Request.Builder()
//        .url("${BuildConfig.BASE_URL}/alarms")
//        .post(body)
//        .addHeader("Authorization", "Bearer $token")
//        .build()
//
//    OkHttpClient().newCall(request).enqueue(object : Callback {
//        override fun onFailure(call: Call, e: IOException) {
//            scope.launch(Dispatchers.Main) {
//                Toast.makeText(context, "서버 요청 실패: ${e.message}", Toast.LENGTH_SHORT).show()
//                Log.e("postAlarmToServer", "IOException: ${e.message}")
//            }
//        }
//
//        override fun onResponse(call: Call, response: Response) {
//            scope.launch(Dispatchers.Main) {
//                if (response.isSuccessful) {
//                    Toast.makeText(context, "알림 등록 성공!", Toast.LENGTH_SHORT).show()
//                } else {
//                    val errorBody = response.body?.string()
//                    Log.e("ServerResponse", "Code: ${response.code}, Body: $errorBody")
//                    val errorMsg = try {
//                        JSONObject(errorBody ?: "{}").optString("error", "알림 등록 실패")
//                    } catch (e: Exception) {
//                        "서버 오류"
//                    }
//                    Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
//                }
//            }
//        }
//    })
//}
//
//
//@Composable
//fun Guardian_Alarm2(navController: NavController) {
//    val context = LocalContext.current
//    val calendar = Calendar.getInstance()
//    val activity = LocalActivity.current as ComponentActivity
//    val viewModel: OneTimeAlarmViewModel = viewModel(viewModelStoreOwner = activity)
//    val scope = rememberCoroutineScope()
//    val navBackStack by navController.currentBackStackEntryAsState()
//    val currentRoute = navBackStack?.destination?.route
//
//    Scaffold { innerPadding ->
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding)
//                .padding(24.dp),
//            horizontalAlignment = Alignment.CenterHorizontally
//        ) {
//            Spacer(Modifier.height(24.dp))
//
//            Image(
//                painter = painterResource(R.drawable.rogo),
//                contentDescription = "로고",
//                modifier = Modifier.size(200.dp)
//            )
//
//            Spacer(Modifier.height(16.dp))
//
//            Text("비정기 알림 설정", fontSize = 24.sp, fontWeight = FontWeight.Bold)
//
//            Spacer(Modifier.height(24.dp))
//
//            Text(
//                "가족 일정이나 병원 일정을\n날짜와 시간을 지정해 설정해 주세요!",
//                fontSize = 14.sp,
//                textAlign = TextAlign.Center,
//                modifier = Modifier.fillMaxWidth()
//            )
//
//            Spacer(Modifier.height(24.dp))
//
//            Button(
//                onClick = {
//                    DatePickerDialog(
//                        context,
//                        { _, year, month, day ->
//                            viewModel.onDateSelected("$year-${month + 1}-$day")
//                        },
//                        calendar.get(Calendar.YEAR),
//                        calendar.get(Calendar.MONTH),
//                        calendar.get(Calendar.DAY_OF_MONTH)
//                    ).show()
//                },
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .height(56.dp),
//                shape = RoundedCornerShape(8.dp),
//                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
//            ) {
//                Text("날짜 선택", fontSize = 18.sp, color = Color.White)
//            }
//
//            Spacer(Modifier.height(16.dp))
//
//            if (viewModel.showInputArea) {
//                OutlinedTextField(
//                    value = viewModel.scheduleText,
//                    onValueChange = viewModel::onTextChanged,
//                    label = { Text("일정을 입력하세요") },
//                    modifier = Modifier.fillMaxWidth()
//                )
//
//                Spacer(Modifier.height(8.dp))
//
//                Button(
//                    onClick = {
//                        TimePickerDialog(
//                            context,
//                            { _, hour, minute ->
//                                viewModel.onTimeSelected(hour, minute)
//                            },
//                            viewModel.selectedHour,
//                            viewModel.selectedMinute,
//                            false
//                        ).show()
//                    },
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(44.dp),
//                    shape = RoundedCornerShape(8.dp),
//                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
//                ) {
//                    Text(
//                        "시간 선택: %02d:%02d".format(viewModel.selectedHour, viewModel.selectedMinute),
//                        color = Color.White
//                    )
//                }
//
//                Spacer(Modifier.height(8.dp))
//
//                Button(
//                    onClick = {
//                        viewModel.addSchedule()
//                        val item = viewModel.schedules.last()
//                        val token = getTokenFromPrefs(context)
//                        Log.d("DEBUG_TOKEN", "Token: $token")
//                        val patientId = getPatientIdFromPrefs(context)
//                        val time = "%02d:%02d".format(item.hour, item.minute)
//
//                        postAlarmToServer(
//                            context = context,
//                            patientId = patientId,
//                            message = item.content,
//                            time = time,
//                            token = token,
//                            scope = scope
//                        )
//                    },
//                    modifier = Modifier
//                        .width(150.dp)
//                        .height(44.dp),
//                    shape = RoundedCornerShape(8.dp),
//                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
//                ) {
//                    Text(
//                        if (viewModel.editingIndex != null) "수정 완료" else "등록하기",
//                        color = Color.White
//                    )
//                }
//            }
//
//            Spacer(Modifier.height(24.dp))
//
//            if (viewModel.schedules.isNotEmpty()) {
//                Text("등록된 일정", fontWeight = FontWeight.Bold, fontSize = 18.sp)
//                Spacer(Modifier.height(8.dp))
//
//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .heightIn(min = 100.dp, max = 300.dp)
//                        .border(1.dp, Color.Gray, RoundedCornerShape(8.dp))
//                        .padding(8.dp)
//                ) {
//                    LazyColumn(
//                        modifier = Modifier.fillMaxSize(),
//                        verticalArrangement = Arrangement.spacedBy(8.dp)
//                    ) {
//                        itemsIndexed(viewModel.schedules) { idx, item ->
//                            Row(
//                                modifier = Modifier.fillMaxWidth(),
//                                verticalAlignment = Alignment.CenterVertically,
//                                horizontalArrangement = Arrangement.SpaceBetween
//                            ) {
//                                Text(
//                                    text = "- ${item.date} %02d:%02d".format(item.hour, item.minute) +
//                                            " : ${item.content}",
//                                    fontSize = 14.sp
//                                )
//                                Row {
//                                    Button(
//                                        onClick = { viewModel.editSchedule(idx) },
//                                        modifier = Modifier.height(30.dp),
//                                        shape = RoundedCornerShape(8.dp),
//                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
//                                    ) {
//                                        Text("수정", fontSize = 9.sp, color = Color.White)
//                                    }
//                                    Spacer(Modifier.width(8.dp))
//                                    Button(
//                                        onClick = {
//                                            viewModel.deleteSchedule(idx)
//                                            // 필요시 서버에도 삭제 요청 가능
//                                        },
//                                        modifier = Modifier.height(30.dp),
//                                        shape = RoundedCornerShape(8.dp),
//                                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
//                                    ) {
//                                        Text("삭제", fontSize = 9.sp, color = Color.White)
//                                    }
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//    }
//}














