package com.example.myapplication.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.draw.clip
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import com.example.myapplication.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

@Composable
fun MemoryInfoInputScreen(navController: NavController, patientId: String) {
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    var whenText by remember { mutableStateOf("") }
    var whereText by remember { mutableStateOf("") }
    var howText by remember { mutableStateOf("") }
    var whatText by remember { mutableStateOf("") }
    var memorableText by remember { mutableStateOf("") }

    var selectedCategory by remember { mutableStateOf("가족") }
    val categoryOptions = listOf("가족", "동네", "학창시절", "여행", "환자가 좋아하는 것\n")
    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val client = remember { OkHttpClient() }
    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> imageUri = uri }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState())
    ) {
        val title = createRef()
        val imageBox = createRef()
        val selectButton = createRef()
        val categoryLabel = createRef()
        val categoryDropdown = createRef()
        val guide1 = createRef()
        val example = createRef()
        val whenLabel = createRef()
        val whenInput = createRef()
        val whereLabel = createRef()
        val whereInput = createRef()
        val howLabel = createRef()
        val howInput = createRef()
        val whatLabel = createRef()
        val whatInput = createRef()
        val memorableLabel = createRef()
        val memorableInput = createRef()
        val uploadButton = createRef()

        Row(
            modifier = Modifier.constrainAs(title) {
                top.linkTo(parent.top, margin = 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("회상정보 ", fontSize = 34.sp, color = Color.Black)
            Text("입력", fontSize = 34.sp, color = Color(0xFF00BFA5))
        }

        Box(
            modifier = Modifier
                .size(150.dp)
                .then(
                    if (imageUri == null) Modifier.background(Color.LightGray, RoundedCornerShape(8.dp))
                    else Modifier
                )
                .constrainAs(imageBox) {
                    top.linkTo(title.bottom, margin = 24.dp)
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
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
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

        Text("카테고리:", modifier = Modifier.constrainAs(categoryLabel) {
            top.linkTo(selectButton.bottom, margin = 24.dp)
            start.linkTo(parent.start)
        })

        Box(
            modifier = Modifier
                .constrainAs(categoryDropdown) {
                    top.linkTo(categoryLabel.bottom, margin = 4.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .background(Color.White, shape = RoundedCornerShape(8.dp))
        ) {
            Column {
                Text(
                    text = selectedCategory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                        .padding(12.dp)
                )
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    categoryOptions.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        Text(
            "다음 항목을 포함하여 상세하게 적어주세요:",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.constrainAs(guide1) {
                top.linkTo(categoryDropdown.bottom, margin = 24.dp)
                start.linkTo(parent.start)
            }
        )

        Text(
            "예: 2020년 봄, 가족들과 제주도에서 벚꽃을 보며 소풍을 즐겼어요.\n아빠가 꽃잎을 머리에 얹어줬던 장면이 가장 기억에 남아요.",
            fontSize = 12.sp,
            color = Color.DarkGray,
            modifier = Modifier.constrainAs(example) {
                top.linkTo(guide1.bottom, margin = 18.dp)
                start.linkTo(parent.start)
            }
        )

        Text("언제:", Modifier.constrainAs(whenLabel) {
            top.linkTo(example.bottom, margin = 16.dp)
            start.linkTo(parent.start)
        })
        TextField(value = whenText, onValueChange = { whenText = it },
            modifier = Modifier.constrainAs(whenInput) {
                top.linkTo(whenLabel.bottom, margin = 4.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            )
        )

        Text("어디서:", Modifier.constrainAs(whereLabel) {
            top.linkTo(whenInput.bottom, margin = 16.dp)
            start.linkTo(parent.start)
        })
        TextField(value = whereText, onValueChange = { whereText = it },
            modifier = Modifier.constrainAs(whereInput) {
                top.linkTo(whereLabel.bottom, margin = 4.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            )
        )

        Text("어떻게:", Modifier.constrainAs(howLabel) {
            top.linkTo(whereInput.bottom, margin = 16.dp)
            start.linkTo(parent.start)
        })
        TextField(value = howText, onValueChange = { howText = it },
            modifier = Modifier.constrainAs(howInput) {
                top.linkTo(howLabel.bottom, margin = 4.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            )
        )

        Text("무엇을:", Modifier.constrainAs(whatLabel) {
            top.linkTo(howInput.bottom, margin = 16.dp)
            start.linkTo(parent.start)
        })
        TextField(value = whatText, onValueChange = { whatText = it },
            modifier = Modifier.constrainAs(whatInput) {
                top.linkTo(whatLabel.bottom, margin = 4.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            )
        )

        Text("가장 기억에 남는 것:", Modifier.constrainAs(memorableLabel) {
            top.linkTo(whatInput.bottom, margin = 16.dp)
            start.linkTo(parent.start)
        })
        TextField(value = memorableText, onValueChange = { memorableText = it },
            modifier = Modifier.constrainAs(memorableInput) {
                top.linkTo(memorableLabel.bottom, margin = 4.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            )
        )

        Button(
            onClick = {
                if (imageUri == null || listOf(whenText, whereText, howText, whatText, memorableText).any { it.isBlank() }) {
                    Toast.makeText(context, "모든 항목을 입력하고 사진을 선택해주세요", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val description = """
                    언제: $whenText
                    어디서: $whereText
                    어떻게: $howText
                    무엇을: $whatText
                    가장 기억에 남는 것: $memorableText
                """.trimIndent()

                val uri = imageUri!!
                val patientIdFromPrefs = prefs.getString("patient_id", null)
                val guardianId = prefs.getString("guardian_id", null)

                if (patientIdFromPrefs.isNullOrEmpty() || guardianId.isNullOrEmpty()) {
                    Toast.makeText(context, "필수 정보가 없습니다.", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                coroutineScope.launch {
                    isLoading = true
                    try {
                        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            ?: throw Exception("이미지를 불러올 수 없습니다.")
                        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val ext = mime.substringAfterLast('/')
                        val fileName = "media.$ext"

                        val idToken = Firebase.auth.currentUser
                            ?.getIdToken(true)
                            ?.await()
                            ?.token ?: throw Exception("토큰 획득 실패")

                        val multipart = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("guardian_id", guardianId)
                            .addFormDataPart("patient_id", patientIdFromPrefs)
                            .addFormDataPart("category", selectedCategory)
                            .addFormDataPart("description", description)
                            .addFormDataPart("image_data", fileName, RequestBody.create(mime.toMediaTypeOrNull(), bytes))
                            .build()

                        val request = Request.Builder()
                            .url("${BuildConfig.BASE_URL}/photos/upload")
                            .addHeader("Authorization", "Bearer $idToken")
                            .post(multipart)
                            .build()

                        val response = withContext(Dispatchers.IO) {
                            client.newCall(request).execute()
                        }

                        if (response.isSuccessful) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "업로드 성공!", Toast.LENGTH_SHORT).show()
                                navController.navigate("MemoryInfoList/$patientId") {
                                    popUpTo("MemoryInfoInput") { inclusive = true }
                                }
                            }
                        } else {
                            val err = response.body?.string().orEmpty()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "업로드 실패: ${response.code} - $err", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
            modifier = Modifier.constrainAs(uploadButton) {
                top.linkTo(memorableInput.bottom, margin = 32.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ) {
            Text("회상 정보 업로드", color = Color.White)
        }

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






