package com.example.myapplication.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun EmergencyAlertPopup(
    patientName: String = "이도하",
    alertTime: String = "2025-07-14 14:32", // 환자가 버튼 누른 시간
    location: String = "수원시 영통구",     // 환자 위치
    onDismiss: () -> Unit = {}
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "🚨 긴급 알림",
                fontSize = 20.sp,
                color = Color.Red
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("👤 환자 이름: $patientName")
                Text("⏰ 발생 시간: $alertTime")
                Text("📍 위치 정보: $location")
                Text("❗ 환자가 긴급 알림을 했습니다.")
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFBF0310)
                )
            ) {
                Text("확인", color = Color.White)
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

@Preview(showBackground = true)
@Composable
fun EmergencyAlertPopupPreview() {
    EmergencyAlertPopup()
}


