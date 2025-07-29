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
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import android.content.Context

@Composable
fun MemoryInfoInputScreen(navController: NavController, patientId: String){
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var description by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val client = remember { OkHttpClient() }
    val coroutineScope = rememberCoroutineScope()

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
                val inputStream = context.contentResolver.openInputStream(imageUri!!)
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

        Button(
            onClick = {
                if (imageUri == null || description.isBlank()) {
                    Toast.makeText(context, "사진과 설명을 모두 입력해주세요", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val uri = imageUri!!
                val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                val patientId = prefs.getString("patient_id", null)
                if (patientId.isNullOrEmpty()) {
                    Toast.makeText(context, "환자 ID가 없습니다.", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                coroutineScope.launch {
                    isLoading = true
                    try {
                        // 이미지 바이트 읽기
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: throw Exception("이미지를 불러올 수 없습니다.")
                        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val ext = mime.substringAfterLast('/')
                        val fileName = "media.$ext"

                        // Firebase ID 토큰 획득
                        val idToken = Firebase.auth.currentUser
                            ?.getIdToken(true)
                            ?.await()
                            ?.token
                            ?: throw Exception("토큰 획득 실패")

                        // Multipart 요청 구성
                        val multipart = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("description", description)
                            .addFormDataPart("patientId", patientId)
                            .addFormDataPart(
                                "media", fileName,
                                RequestBody.create(mime.toMediaTypeOrNull(), bytes)
                            )
                            .build()

                        // 서버 업로드
                        val request = Request.Builder()
                            .url("${BuildConfig.BASE_URL}/memory/upload")
                            .addHeader("Authorization", "Bearer $idToken")
                            .post(multipart)
                            .build()
                        val response = withContext(Dispatchers.IO) {
                            client.newCall(request).execute()
                        }
                        if (response.isSuccessful) {
                            Toast.makeText(context, "업로드 성공!", Toast.LENGTH_SHORT).show()
                            navController.popBackStack()
                        } else {
                            val err = response.body?.string().orEmpty()
                            Toast.makeText(
                                context,
                                "업로드 실패: ${response.code} - $err",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    } catch (e: Exception) {
                        Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_LONG).show()
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(8.dp),
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

        if (isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color(0x88000000)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}




