package com.example.myapplication.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.example.myapplication.network.RetrofitInstance
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch

@Composable
fun Patient_Login(navController: NavController) {
    // Firebase Auth & CoroutineScope
    val auth = Firebase.auth
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // ★ 위치 조절 변수 ★
    val imageGroupTopPadding = 80.dp
    val formTopPadding = 320.dp

    // 입력값 상태
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // 1) 가운데 로고
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = imageGroupTopPadding)
                .sizeIn(minWidth = 0.dp, minHeight = 0.dp),
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

        // 2) 로그인 폼
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = formTopPadding)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                text = "어르신 로그인",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(text = "이메일 주소", fontSize = 16.sp)
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                placeholder = { Text("example@mail.com") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(text = "비밀번호", fontSize = 16.sp)
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = { Text("••••••••") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation()
            )
        }

        // 3) 버튼
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 170.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(
                onClick = {
                    // 1) Firebase 로그인
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                // 2) 백엔드에서 환자 정보 조회
                                coroutineScope.launch {
                                    try {
                                        val res = RetrofitInstance.api.getPatient()
                                        if (res.isSuccessful) {
                                            // 3) 조회 성공 시 다음 화면으로 이동
                                            navController.navigate("sentence") {
                                                popUpTo("Patient_Login") { inclusive = true }
                                            }
                                        } else {
                                            Toast
                                                .makeText(context, "조회 실패: ${res.code()}", Toast.LENGTH_SHORT)
                                                .show()
                                        }
                                    } catch (e: Exception) {
                                        Toast
                                            .makeText(context, "통신 오류: ${e.message}", Toast.LENGTH_SHORT)
                                            .show()
                                    }
                                }
                            } else {
                                Toast
                                    .makeText(context, "로그인 실패: ${task.exception?.message}", Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("로그인", color = Color.White, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { navController.navigate("patientSignUp") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
            ) {
                Text("회원가입", color = Color.White, fontSize = 16.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPatientLogin() {
    Patient_Login(navController = rememberNavController())
}

