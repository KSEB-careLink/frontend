package com.example.myapplication.screens

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import com.example.myapplication.network.LinkPatientRequest


@Composable
fun PatientLinkPage(navController: NavController) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var joinCode    by remember { mutableStateOf("") }
    var patientName by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("환자 연동", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = joinCode,
            onValueChange = { joinCode = it },
            label = { Text("인증 코드") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = patientName,
            onValueChange = { patientName = it },
            label = { Text("환자 이름") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (joinCode.isBlank() || patientName.isBlank()) {
                    Toast.makeText(context, "코드와 이름을 모두 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                coroutineScope.launch {
                    try {
                        val req = LinkPatientRequest(
                            joinCode    = joinCode.trim(),
                            patientName = patientName.trim()
                        )
                        val res = RetrofitInstance.api.linkPatient(req)
                        if (res.isSuccessful) {
                            Toast.makeText(context, "연동 성공!", Toast.LENGTH_SHORT).show()
                            navController.navigate("PatientMain") {
                                popUpTo("PatientLinkPage") { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "연동 실패: ${res.code()}", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "통신 오류: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            enabled = joinCode.isNotBlank() && patientName.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("연동 시작")
        }
    }
}





