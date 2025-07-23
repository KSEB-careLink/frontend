package com.example.myapplication.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.R
import com.example.myapplication.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

// SharedPreferences에서 환자 ID 가져오기 헬퍼
fun getPatientIdFromPrefs(context: Context): String? {
    return context
        .getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        .getString("patient_id", null)
}

// 메모리 아이템 데이터 클래스

data class MemoryItemCloud(
    val id: String,
    var description: String,
    val imageUrl: String,
    val mediaPath: String
)

@Composable
fun MemoryInfoListScreen(navController: NavController) {
    val memoryList = remember { mutableStateListOf<MemoryItemCloud>() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MemoryItemCloud?>(null) }
    var newDescription by remember { mutableStateOf("") }

    var itemToDelete by remember { mutableStateOf<MemoryItemCloud?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // 데이터 로드: 백엔드 API 호출 + 서명 URL 사용
    LaunchedEffect(Unit) {
        val patientId = getPatientIdFromPrefs(context)
        if (patientId.isNullOrBlank()) {
            Log.e("MemoryList", "환자 ID 없음")
            return@LaunchedEffect
        }
        val idToken = Firebase.auth.currentUser
            ?.getIdToken(true)
            ?.await()
            ?.token
        if (idToken.isNullOrBlank()) {
            Log.e("MemoryList", "토큰 획득 실패")
            return@LaunchedEffect
        }
        try {
            val req = Request.Builder()
                .url("${BuildConfig.BASE_URL}/memory/list/$patientId")
                .addHeader("Authorization", "Bearer $idToken")
                .get()
                .build()
            val res = withContext(Dispatchers.IO) { client.newCall(req).execute() }
            if (!res.isSuccessful) {
                Log.e("MemoryList", "API 호출 실패: ${res.code}")
                return@LaunchedEffect
            }
            val bodyStr = res.body?.string().orEmpty()
            val arr = JSONObject(bodyStr).getJSONArray("memoryItems")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                memoryList.add(
                    MemoryItemCloud(
                        id = obj.getString("id"),
                        description = obj.getString("description"),
                        imageUrl = obj.getString("imageUrl"),
                        mediaPath = obj.getString("mediaPath")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("MemoryList", "데이터 로드 실패: ${e.message}")
            Toast.makeText(context, "불러오기 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // UI 레이아웃
    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val (logo, title, listBox) = createRefs()

        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(100.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 50.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        Text(
            text = "회상 정보 데이터 확인",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(logo.bottom, margin = 12.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        Box(
            modifier = Modifier
                .constrainAs(listBox) {
                    top.linkTo(title.bottom, margin = -14.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.wrapContent
                }
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 600.dp)
                .background(Color(0xFFFDEFF1), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(memoryList, key = { it.id }) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFC9D8), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(item.description, fontSize = 14.sp, color = Color.Black)
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = "기억 사진",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "수정",
                                fontSize = 12.sp,
                                color = Color.Blue,
                                modifier = Modifier
                                    .padding(end = 16.dp)
                                    .clickable {
                                        editingItem = item
                                        newDescription = item.description
                                        showDialog = true
                                    }
                            )
                            Text(
                                text = "삭제",
                                fontSize = 12.sp,
                                color = Color.Red,
                                modifier = Modifier.clickable {
                                    itemToDelete = item
                                    showDeleteConfirm = true
                                }
                            )
                        }
                    }
                }
            }
        }

        // 다이얼로그 등... (수정/삭제 로직은 서버 호출로 변경됨)
        // 수정 다이얼로그
        if (showDialog && editingItem != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("설명 수정") },
                text = {
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("설명") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        editingItem?.let { item ->
                            val idx = memoryList.indexOfFirst { it.id == item.id }
                            if (idx != -1) {
                                memoryList[idx] = item.copy(description = newDescription)
                                coroutineScope.launch {
                                    val patientId = getPatientIdFromPrefs(context) ?: return@launch
                                    val token = Firebase.auth.currentUser
                                        ?.getIdToken(true)?.await()?.token ?: return@launch
                                    val json = JSONObject().put("description", newDescription)
                                    val req = Request.Builder()
                                        .url("${BuildConfig.BASE_URL}/memory/$patientId/${item.id}")
                                        .addHeader("Authorization", "Bearer $token")
                                        .put(json.toString().toRequestBody("application/json".toMediaType()))
                                        .build()
                                    withContext(Dispatchers.IO) {
                                        client.newCall(req).execute().close()
                                    }
                                }
                            }
                        }
                        showDialog = false
                    }) { Text("저장") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("취소") }
                }
            )
        }

        // 삭제 다이얼로그
        if (showDeleteConfirm && itemToDelete != null) {
            AlertDialog(
                onDismissRequest = {
                    showDeleteConfirm = false
                    itemToDelete = null
                },
                title = { Text("정말 삭제하시겠습니까?") },
                text = { Text("이 회상 정보를 영구적으로 삭제합니다.") },
                confirmButton = {
                    TextButton(onClick = {
                        itemToDelete?.let { item ->
                            coroutineScope.launch {
                                val patientId = getPatientIdFromPrefs(context) ?: return@launch
                                val token = Firebase.auth.currentUser
                                    ?.getIdToken(true)?.await()?.token ?: return@launch
                                withContext(Dispatchers.IO) {
                                    client.newCall(
                                        Request.Builder()
                                            .url("${BuildConfig.BASE_URL}/memory/$patientId/${item.id}")
                                            .addHeader("Authorization", "Bearer $token")
                                            .delete()
                                            .build()
                                    ).execute().use { res ->
                                        if (res.isSuccessful) memoryList.remove(item)
                                    }
                                }
                            }
                        }
                        showDeleteConfirm = false
                        itemToDelete = null
                    }) { Text("삭제", color = Color.Red) }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        itemToDelete = null
                    }) { Text("취소") }
                }
            )
        }
    }
}













