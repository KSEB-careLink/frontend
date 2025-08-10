package com.example.myapplication.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.example.myapplication.network.EmergencyRequest
import com.example.myapplication.network.RetrofitInstance
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Composable
fun Patient_Alert(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val api = remember { RetrofitInstance.api }

    var sending by remember { mutableStateOf(false) }

    // 현재 route (하단 탭 유지용)
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            val navColors = NavigationBarItemDefaults.colors(
                indicatorColor      = Color.Transparent,
                selectedIconColor   = Color(0xFF00C4B4),
                unselectedIconColor = Color(0xFF888888),
                selectedTextColor   = Color(0xFF00C4B4),
                unselectedTextColor = Color(0xFF888888)
            )
            NavigationBar {
                listOf(
                    "sentence/{patientId}" to "회상문장",
                    "quiz/{patientId}"     to "회상퀴즈",
                    "alert"                to "긴급 알림"
                ).forEach { (route, label) ->
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Star, contentDescription = label) },
                        label = { Text(label) },
                        selected = currentRoute == route,
                        onClick = {
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        },
                        colors = navColors
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(210.dp))

            Text(
                buildAnnotatedString {
                    append("불편함이나 ")
                    withStyle(SpanStyle(color = Color(0xFFE2101A))) { append("위험") }
                    append("을 느끼시나요?")
                },
                fontSize = 25.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF00C4B4))) { append("보호자") }
                    append("에게 연락할게요")
                },
                fontSize = 25.sp
            )

            Spacer(Modifier.height(16.dp))
            Text(
                text = "긴급 알림",
                fontSize = 39.sp,
                color = Color(0xFFE2101A),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(16.dp))

            // HELP 버튼
            Box(
                modifier = Modifier
                    .size(260.dp)
                    .background(color = Color(0xFFBF0310), shape = CircleShape)
                    .clickable(enabled = !sending) {
                        if (sending) return@clickable

                        scope.launch(Dispatchers.IO) {
                            sending = true
                            try {
                                // 1) 현재 로그인 uid
                                val uid = Firebase.auth.currentUser?.uid
                                if (uid.isNullOrBlank()) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                                    }
                                    return@launch
                                }

                                // 2) Firestore에서 환자 문서 읽고 linkedGuardian 가져오기
                                val snap = Firebase.firestore
                                    .collection("patients")
                                    .document(uid)
                                    .get()
                                    .await()

                                val guardianId = snap.getString("linkedGuardian")
                                if (guardianId.isNullOrBlank()) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "연결된 보호자가 없습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                    return@launch
                                }

                                // 3) 긴급 알림 전송
                                val res = api.sendEmergency(
                                    EmergencyRequest(
                                        guardianId = guardianId,
                                        message = "환자에게서 긴급 요청이 도착했어요."
                                    )
                                )

                                if (res.isSuccessful) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "보호자에게 긴급 알림을 보냈어요", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    val code = res.code()
                                    val body = res.errorBody()?.string()
                                    android.util.Log.e("Emergency", "fail code=$code body=$body")
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "전송 실패 ($code)", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("Emergency", "exception", e)
                                withContext(Dispatchers.Main) {
                                    Toast.makeText(context, "전송 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } finally {
                                sending = false
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.help_icon),
                    contentDescription = "HELP",
                    modifier = Modifier.size(140.dp),
                    contentScale = ContentScale.Fit
                )
            }

            if (sending) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator()
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPatient_Alert() {
    Patient_Alert(navController = rememberNavController())
}
