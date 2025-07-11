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
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
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

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val (
            logo, title1, title2, imageBox, uploadButton,
            descriptionLabel, descriptionInput
        ) = createRefs()

        // 로고
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            modifier = Modifier
                .size(80.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        // 제목
        Text(
            "회상정보 ",
            fontSize = 24.sp,
            modifier = Modifier.constrainAs(title1) {
                top.linkTo(logo.bottom, margin = 16.dp)
                start.linkTo(parent.start)
            }
        )

        Text(
            "입력",
            fontSize = 24.sp,
            color = Color(0xFF00BFA5),
            modifier = Modifier.constrainAs(title2) {
                top.linkTo(logo.bottom, margin = 16.dp)
                start.linkTo(title1.end)
            }
        )

        // 사진 선택 박스
        Box(
            modifier = Modifier
                .size(250.dp)
                .background(Color.LightGray, RoundedCornerShape(8.dp))
                .constrainAs(imageBox) {
                    top.linkTo(title1.bottom, margin = 24.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                },
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

        // 업로드 버튼
        Button(
            onClick = { launcher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF80DEEA)),
            modifier = Modifier.constrainAs(uploadButton) {
                top.linkTo(imageBox.bottom, margin = 8.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ) {
            Text("사진 업로드", color = Color.White)
        }

        // 설명 입력 안내 텍스트
        Text(
            "해당 미디어에 대한 설명을 해주세요",
            fontSize = 16.sp,
            modifier = Modifier.constrainAs(descriptionLabel) {
                top.linkTo(uploadButton.bottom, margin = 16.dp)
                start.linkTo(parent.start)
            }
        )

        // 설명 입력 필드
        TextField(
            value = description,
            onValueChange = { description = it },
            placeholder = { Text("이 사진은 ...") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .constrainAs(descriptionInput) {
                    top.linkTo(descriptionLabel.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(100.dp)
        )
    }
}

