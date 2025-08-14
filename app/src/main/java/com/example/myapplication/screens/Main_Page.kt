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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.myapplication.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import kotlin.math.min

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

        // 🔸 기존: Spacer(weight=1f) + 고정 폰트 크기의 이모지 Box
        // 🔸 변경: 남은 공간을 채우는 BoxWithConstraints로 가용 크기 측정 → 폰트 크기 자동 조절
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            val density = LocalDensity.current
            // 남은 공간의 너비/높이(dp) 중 작은 값 기준으로 폰트 크기를 결정
            val basisDp = min(maxWidth.value, maxHeight.value)    // dp 값(Float)
            // 남은 공간의 약 70%를 차지하도록 목표 sp 계산 (사용자 폰 글꼴 배율 고려)
            val fittedSp = (basisDp * 0.70f) / density.fontScale   // Float (sp 값)
            // 너무 크거나 너무 작지 않게 가드 (최소 80sp, 최대 250sp)
            val targetSp = fittedSp.coerceIn(80f, 250f)
            // 부드럽게 변화
            val animatedSp by animateFloatAsState(
                targetValue = targetSp,
                animationSpec = tween(durationMillis = 250),
                label = "emojiSize"
            )

            Text("📱", fontSize = animatedSp.sp, color = Color(0x9900C4B4))
        }

        Spacer(modifier = Modifier.height(24.dp))


        Button(
            onClick = { navController.navigate("choose") },
            modifier = Modifier
                .fillMaxWidth()
                .height(66.dp)
                .padding(bottom = 16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4)),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("역활 선택 화면으로", color = Color.White, fontSize = 18.sp)
        }
    }
}





