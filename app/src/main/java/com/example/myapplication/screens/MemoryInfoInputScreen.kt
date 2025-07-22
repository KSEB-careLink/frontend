package com.example.myapplication.screens

import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
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
import androidx.navigation.NavController
import com.example.myapplication.R
import com.example.myapplication.BuildConfig
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException
import android.os.Handler
import android.os.Looper
import android.content.Context

@Composable
fun MemoryInfoInputScreen(navController: NavController) {
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
            logo, title1, title2, imageBox, selectButton,
            uploadButton, descriptionLabel, descriptionInput
        ) = createRefs()

        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            modifier = Modifier
                .size(80.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 24.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

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
                Text("사진 선택", color = Color.White)
            }
        }

        // 사진 선택 버튼
        Button(
            onClick = { launcher.launch("image/*") },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF80DEEA)),
            modifier = Modifier.constrainAs(selectButton) {
                top.linkTo(imageBox.bottom, margin = 8.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ) {
            Text("사진 선택", color = Color.White)
        }

        // 업로드 버튼
        Button(
            onClick = {
                if (imageUri == null || description.isBlank()) {
                    Toast.makeText(context, "사진과 설명을 모두 입력해주세요", Toast.LENGTH_SHORT).show()
                } else {
                    uploadMemory(
                        context = context,
                        imageUri = imageUri!!,
                        description = description,
                        onSuccess = {
                            Toast.makeText(context, "업로드 성공!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        },
                        onFailure = { error ->
                            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
            modifier = Modifier.constrainAs(uploadButton) {
                top.linkTo(selectButton.bottom, margin = 8.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ) {
            Text("회상 정보 업로드", color = Color.White)
        }

        Text(
            "해당 미디어에 대한 설명을 해주세요",
            fontSize = 16.sp,
            modifier = Modifier.constrainAs(descriptionLabel) {
                top.linkTo(uploadButton.bottom, margin = 16.dp)
                start.linkTo(parent.start)
            }
        )

        TextField(
            value = description,
            onValueChange = { description = it },
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

// 이미지 URI → ByteArray
fun uriToByteArray(context: android.content.Context, uri: Uri): ByteArray? {
    return context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
}

// Firebase 업로드 요청
fun uploadMemory(
    context: Context,
    imageUri: Uri,
    description: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val token = prefs.getString("jwt_token", null)
    val patientId = prefs.getString("patient_id", null)

    if (token == null || patientId == null) {
        onFailure("로그인이 필요합니다.")
        return
    }

    val imageBytes = uriToByteArray(context, imageUri)
    if (imageBytes == null) {
        onFailure("이미지를 불러올 수 없습니다.")
        return
    }

    val client = OkHttpClient()

    val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
        .addFormDataPart("description", description)
        .addFormDataPart("patientId", patientId)
        .addFormDataPart(
            "media", "image.jpg",
            RequestBody.create("image/jpeg".toMediaTypeOrNull(), imageBytes)
        )
        .build()

    val request = Request.Builder()
        .url("${BuildConfig.BASE_URL}/memory/upload")
        .addHeader("Authorization", "Bearer $token")
        .post(requestBody)
        .build()

    val mainHandler = Handler(Looper.getMainLooper())

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            mainHandler.post {
                onFailure("업로드 실패: ${e.message}")
            }
        }

        override fun onResponse(call: Call, response: Response) {
            mainHandler.post {
                if (response.isSuccessful) {
                    onSuccess()
                } else {
                    onFailure("서버 오류: ${response.code}")
                }
            }
        }
    })
}


