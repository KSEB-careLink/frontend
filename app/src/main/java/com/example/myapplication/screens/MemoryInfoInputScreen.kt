// app/src/main/java/com/example/myapplication/screens/MemoryInfoInputScreen.kt
package com.example.myapplication.screens

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit

@Composable
fun MemoryInfoInputScreen(navController: NavController, patientId: String) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)

    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }

    var isLoading by remember { mutableStateOf(false) }

    var timeText by remember { mutableStateOf("") }
    var placeText by remember { mutableStateOf("") }
    var charactersText by remember { mutableStateOf("") }
    var memorableText by remember { mutableStateOf("") }

    var selectedCategory by remember { mutableStateOf("가족") }
    val categoryOptions = listOf("가족", "동네", "학창시절", "여행", "환자가 좋아하는 것")
    var expanded by remember { mutableStateOf(false) }

    // OkHttpClient (타임아웃 + 로깅)
    val client = remember {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(180, TimeUnit.SECONDS)
            .readTimeout(180, TimeUnit.SECONDS)
            .callTimeout(210, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val coroutineScope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        imageUri = uri
    }

    // 이미지 프리뷰 비동기 로드(큰 사진도 안전)
    LaunchedEffect(imageUri) {
        previewBitmap = null
        imageUri?.let { uri ->
            previewBitmap = withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    BitmapFactory.decodeStream(input)
                }
            }
        }
    }

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
        val guideLabel = createRef()
        val timeLabel = createRef()
        val timeQ = createRef()
        val timeInput = createRef()
        val placeLabel = createRef()
        val placeQ = createRef()
        val placeInput = createRef()
        val charLabel = createRef()
        val charQ = createRef()
        val charInput = createRef()
        val memorableLabel = createRef()
        val memorableQ = createRef()
        val memorableInput = createRef()
        val uploadButton = createRef()

        // 제목
        Row(
            modifier = Modifier.constrainAs(title) {
                top.linkTo(parent.top, margin = 16.dp)
                start.linkTo(parent.start); end.linkTo(parent.end)
            },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("회상정보 ", fontSize = 34.sp, color = Color.Black)
            Text("입력", fontSize = 34.sp, color = Color(0xFF00BFA5))
        }

        // 이미지 박스
        Box(
            modifier = Modifier
                .size(150.dp)
                .then(
                    if (previewBitmap == null)
                        Modifier.background(Color.LightGray, RoundedCornerShape(8.dp))
                    else Modifier
                )
                .constrainAs(imageBox) {
                    top.linkTo(title.bottom, margin = 24.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                },
            contentAlignment = Alignment.Center
        ) {
            if (previewBitmap != null) {
                Image(
                    bitmap = previewBitmap!!.asImageBitmap(),
                    contentDescription = "선택된 사진",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
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
                start.linkTo(parent.start); end.linkTo(parent.end)
            }
        ) {
            Text("사진 선택", color = Color.White)
        }

        // 카테고리
        Text(
            "카테고리:",
            modifier = Modifier.constrainAs(categoryLabel) {
                top.linkTo(selectButton.bottom, margin = 24.dp)
                start.linkTo(parent.start)
            }
        )
        Box(
            modifier = Modifier
                .constrainAs(categoryDropdown) {
                    top.linkTo(categoryLabel.bottom, margin = 4.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .background(Color.White, RoundedCornerShape(8.dp))
        ) {
            Column {
                Text(
                    selectedCategory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = true }
                        .padding(12.dp)
                )
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    categoryOptions.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                selectedCategory = cat
                                expanded = false
                            }
                        )
                    }
                }
            }
        }

        // 안내 라벨
        Text(
            "아래 질문에 답해주세요:",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.constrainAs(guideLabel) {
                top.linkTo(categoryDropdown.bottom, margin = 24.dp)
                start.linkTo(parent.start)
            }
        )

        // 🕒 시간
        Text(
            "🕒 시간",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(timeLabel) {
                top.linkTo(guideLabel.bottom, margin = 16.dp)
                start.linkTo(parent.start)
            }
        )
        Text(
            "Q: 언제의 기억인가요?",
            fontSize = 12.sp,
            color = Color.DarkGray,
            modifier = Modifier.constrainAs(timeQ) {
                top.linkTo(timeLabel.bottom, margin = 4.dp)
                start.linkTo(parent.start)
            }
        )
        TextField(
            value = timeText,
            onValueChange = { timeText = it },
            placeholder = { Text("ex) 2017년 여름, 작년 설날") },
            modifier = Modifier.constrainAs(timeInput) {
                top.linkTo(timeQ.bottom, margin = 4.dp)
                start.linkTo(parent.start); end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            )
        )

        // 📍 장소
        Text(
            "📍 장소",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(placeLabel) {
                top.linkTo(timeInput.bottom, margin = 16.dp)
                start.linkTo(parent.start)
            }
        )
        Text(
            "Q: 어디에서 있었던 일인가요?",
            fontSize = 12.sp,
            color = Color.DarkGray,
            modifier = Modifier.constrainAs(placeQ) {
                top.linkTo(placeLabel.bottom, margin = 4.dp)
                start.linkTo(parent.start)
            }
        )
        TextField(
            value = placeText,
            onValueChange = { placeText = it },
            placeholder = { Text("ex) 제주도 땡땡계곡, 을왕리 바닷가") },
            modifier = Modifier.constrainAs(placeInput) {
                top.linkTo(placeQ.bottom, margin = 4.dp)
                start.linkTo(parent.start); end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            )
        )

        // 👥 등장인물
        Text(
            "👥 등장인물",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(charLabel) {
                top.linkTo(placeInput.bottom, margin = 16.dp)
                start.linkTo(parent.start)
            }
        )
        Text(
            "Q: 누구와 함께 있었나요?",
            fontSize = 12.sp,
            color = Color.DarkGray,
            modifier = Modifier.constrainAs(charQ) {
                top.linkTo(charLabel.bottom, margin = 4.dp)
                start.linkTo(parent.start)
            }
        )
        TextField(
            value = charactersText,
            onValueChange = { charactersText = it },
            placeholder = { Text("ex) 형이랑 친구랑 나, 아버지") },
            modifier = Modifier.constrainAs(charInput) {
                top.linkTo(charQ.bottom, margin = 4.dp)
                start.linkTo(parent.start); end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            )
        )

        // 🌟 가장 기억에 남는 것
        Text(
            "🌟 가장 기억에 남는 것",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(memorableLabel) {
                top.linkTo(charInput.bottom, margin = 16.dp)
                start.linkTo(parent.start)
            }
        )
        Text(
            "Q: 그날 가장 기억에 남는 장면이나 일이 있다면요?",
            fontSize = 12.sp,
            color = Color.DarkGray,
            modifier = Modifier.constrainAs(memorableQ) {
                top.linkTo(memorableLabel.bottom, margin = 4.dp)
                start.linkTo(parent.start)
            }
        )
        TextField(
            value = memorableText,
            onValueChange = { memorableText = it },
            placeholder = { Text("ex) 형이 물에 빠져서 허우적댐") },
            modifier = Modifier.constrainAs(memorableInput) {
                top.linkTo(memorableQ.bottom, margin = 4.dp)
                start.linkTo(parent.start); end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            )
        )

        // 업로드 버튼
        Button(
            onClick = {
                // 입력 검증
                if (imageUri == null || listOf(timeText, placeText, charactersText, memorableText).any { it.isBlank() }) {
                    Toast.makeText(context, "모든 항목을 입력하고 사진을 선택해주세요", Toast.LENGTH_SHORT).show()
                    return@Button
                }
                val description = """
                    시간: $timeText
                    장소: $placeText
                    등장인물: $charactersText
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
                        val idToken = Firebase.auth.currentUser
                            ?.getIdToken(true)?.await()?.token
                            ?: throw Exception("토큰 획득 실패")

                        val (bytes, mime, fileName) = withContext(Dispatchers.IO) {
                            // 바이트 읽기 + 리사이즈/압축
                            val mimeDetected = getMimeType(context, uri) ?: "image/jpeg"
                            val finalFileName = getFileName(context, uri) ?: "upload.${mimeDetected.substringAfterLast('/')}"
                            val compressed = readAndCompressImage(context, uri, maxDim = 2048, quality = 88)
                            Triple(compressed, mimeDetected, finalFileName)
                        }

                        val multipart = MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("guardian_id", guardianId)
                            .addFormDataPart("patient_id", patientIdFromPrefs)
                            .addFormDataPart("category", selectedCategory)
                            .addFormDataPart("description", description)
                            .addFormDataPart(
                                "image",
                                fileName,
                                bytes.toRequestBody(mime.toMediaTypeOrNull())
                            )
                            .build()

                        val url = BuildConfig.BASE_URL.trimEnd('/') + "/photos"
                        val request = Request.Builder()
                            .url(url)
                            .addHeader("Authorization", "Bearer $idToken")
                            .post(multipart)
                            .build()

                        val (ok, code, body) = withContext(Dispatchers.IO) {
                            client.newCall(request).execute().use { resp ->
                                Triple(resp.isSuccessful, resp.code, resp.body?.string().orEmpty())
                            }
                        }

                        if (ok) {
                            // 업로드 카운트 갱신
                            val prevCount = prefs.getInt("upload_count", 0)
                            val newCount = prevCount + 1
                            prefs.edit().putInt("upload_count", newCount).apply()

                            if (newCount < 9) {
                                Toast.makeText(
                                    context,
                                    "회상정보 ${newCount}개 저장되었습니다. ${9 - newCount}개 더 입력해주세요.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                // 필드 초기화
                                imageUri = null
                                previewBitmap = null
                                timeText = ""
                                placeText = ""
                                charactersText = ""
                                memorableText = ""
                            } else {
                                Toast.makeText(
                                    context,
                                    "회상정보 9개 입력 완료! 앱을 이용할 수 있습니다.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                navController.navigate("main2/$patientId") {
                                    popUpTo("MemoryInfoInput/$patientId") { inclusive = true }
                                }
                            }
                        } else {
                            Toast.makeText(
                                context,
                                "업로드 실패: $code - $body",
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
                top.linkTo(memorableInput.bottom, margin = 32.dp)
                start.linkTo(parent.start); end.linkTo(parent.end)
            }
        ) {
            Text("회상 정보 업로드", color = Color.White)
        }

        // 로딩 인디케이터
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

/* ===========================
   Helper functions (IO safe)
   =========================== */

private fun getMimeType(context: Context, uri: Uri): String? {
    return context.contentResolver.getType(uri)
}

private fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor.use {
            if (it != null && it.moveToFirst()) {
                result = it.getString(it.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

/**
 * 이미지를 다운샘플링 후 JPEG/WEBP로 압축해서 바이트 배열 반환.
 * - maxDim: 긴 변 기준 최대 픽셀
 * - quality: 0~100
 */
private suspend fun readAndCompressImage(
    context: Context,
    uri: Uri,
    maxDim: Int,
    quality: Int
): ByteArray = withContext(Dispatchers.IO) {
    // 1) 먼저 bounds만 읽어 사이즈 파악
    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, boundsOpts)
    }

    val (srcW, srcH) = boundsOpts.outWidth to boundsOpts.outHeight
    if (srcW <= 0 || srcH <= 0) {
        // 이름 모를 포맷이면 그냥 원본 스트림으로 읽어서 리턴
        return@withContext context.contentResolver.openInputStream(uri)?.use(InputStream::readBytes)
            ?: ByteArray(0)
    }

    // 2) 샘플링 비율 계산
    var inSample = 1
    val longSide = maxOf(srcW, srcH)
    if (longSide > maxDim) {
        inSample = longSide / maxDim
        if (inSample < 1) inSample = 1
    }

    // 3) 실제 디코딩
    val decodeOpts = BitmapFactory.Options().apply { inSampleSize = inSample }
    val bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
        BitmapFactory.decodeStream(input, null, decodeOpts)
    } ?: return@withContext ByteArray(0)

    // 4) 필요시 추가 리사이즈 (긴 변 기준 maxDim)
    val (bw, bh) = bitmap.width to bitmap.height
    val scale = (maxDim.toFloat() / maxOf(bw, bh)).coerceAtMost(1f) // 1보다 크지 않게(늘리지 않음)
    val finalBitmap = if (scale < 1f) {
        Bitmap.createScaledBitmap(bitmap, (bw * scale).toInt(), (bh * scale).toInt(), true)
            .also { if (it != bitmap) bitmap.recycle() }
    } else bitmap

    // 5) JPEG로 압축 (PNG 원본이어도 전송 효율상 JPEG 권장)
    val bos = ByteArrayOutputStream()
    finalBitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(60, 95), bos)
    if (finalBitmap != bitmap) finalBitmap.recycle()
    bos.toByteArray()
}








