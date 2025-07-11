package com.example.myapplication.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R

@Composable
fun MemoryInputScreen() {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var description by remember { mutableStateOf("") }
    val context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 상단 로고
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text("회상정보 ", fontSize = 24.sp)
        Text("입력", fontSize = 24.sp, color = Color(0xFF00BFA5))

        Spacer(modifier = Modifier.height(16.dp))

        // 사진 선택
        Box(
            modifier = Modifier
                .size(250.dp)
                .background(Color.LightGray, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (imageUri != null) {
                val inputStream = imageUri?.let { context.contentResolver.openInputStream(it) }
                val bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap?.let {
                    Image(
                        bitmap = it.asImageBitmap(),
                        contentDescription = "선택된 사진",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Text("사진 또는 영상 선택", color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = { launcher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF80DEEA))
        ) {
            Text("사진 업로드", color = Color.White)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 설명 입력
        Text("해당 미디어에 대한 설명을 해주세요")
        TextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("이 사진은 ...") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(8.dp)
        )
    }
}
