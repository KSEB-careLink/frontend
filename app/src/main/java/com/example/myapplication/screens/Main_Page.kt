package com.example.myapplication.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

@Composable
fun Main_Page(navController: NavController) {
    val context = LocalContext.current

    // 🔹 SharedPreferences 저장 함수
    fun savePatientIdToPrefs(context: Context, patientId: String) {
        val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("patient_id", patientId).apply()
    }

    // 1) 로그인된 보호자 UID
    val guardianUid = Firebase.auth.currentUser?.uid

    // 2) Firestore 에서 linkedPatients 불러오기
    var linkedPatients by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(guardianUid) {
        guardianUid?.let { uid ->
            try {
                val doc = Firebase.firestore
                    .collection("guardians")
                    .document(uid)
                    .get()
                    .await()
                linkedPatients = doc.get("linkedPatients") as? List<String> ?: emptyList()
            } catch (e: Exception) {
                linkedPatients = emptyList()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // ─── 1) 로고 ─────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.rogo),
                contentDescription = "로고",
                modifier = Modifier
                    .size(200.dp)
                    .offset(y = (-20).dp),
                contentScale = ContentScale.Fit
            )
            Image(
                painter = painterResource(id = R.drawable.ai_text),
                contentDescription = "텍스트 로고",
                modifier = Modifier
                    .size(150.dp)
                    .offset(y = 90.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(54.dp))

        // ─── 2) 연결된 환자 리스트 ─────────────────────
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (linkedPatients.isEmpty()) {
                Text("연결된 환자가 없습니다.", color = Color.Gray, fontSize = 16.sp)
            } else {
                linkedPatients.forEach { patientId ->
                    Button(
                        onClick = {
                            savePatientIdToPrefs(context, patientId)
                            navController.navigate("main2/$patientId")
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(66.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = patientId, color = Color.White, fontSize = 18.sp)
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "Next",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // ─── 3) 스마트폰 이모지 ─────────────────────
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text("📱", fontSize = 250.sp, color = Color(0x9900C4B4))
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ─── 4) 환자 추가 버튼 ─────────────────────
        Button(
            onClick = { navController.navigate("addDevice") },
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("환자의 기기 추가", color = Color.White, fontSize = 18.sp)
        }
    }
}




