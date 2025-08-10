package com.example.myapplication.screens

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.R
import com.example.myapplication.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import androidx.compose.foundation.Image

// SharedPreferences에서 보호자 ID 가져오기
fun getGuardianIdFromPrefs(context: Context): String? =
    context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        .getString("guardian_id", null)

// 사진 아이템 데이터 클래스
data class PhotoItem(
    val id: String,
    var description: String,
    val imageUrl: String,
    val category: String,
    val createdAt: String
)

@Composable
fun MemoryInfoListScreen(navController: NavController, patientId: String) {
    // 상태 및 의존성
    val photoList = remember { mutableStateListOf<PhotoItem>() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }
    val storage = Firebase.storage

    // 카테고리 필터 상태
    val categoryOptions = listOf("전체", "가족", "동네", "학창시절", "여행", "환자가 좋아하는 것")
    var selectedCategory by remember { mutableStateOf("전체") }
    var expanded by remember { mutableStateOf(false) }

    // 수정/삭제 다이얼로그 상태
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<PhotoItem?>(null) }
    var newDescription by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<PhotoItem?>(null) }

    // 데이터 로드
    LaunchedEffect(Unit) {
        val guardianId = getGuardianIdFromPrefs(context).orEmpty()
        if (guardianId.isBlank()) {
            Log.e("PhotosList", "보호자 ID 없음")
            return@LaunchedEffect
        }
        val idToken = Firebase.auth.currentUser
            ?.getIdToken(true)?.await()?.token.orEmpty()
        if (idToken.isBlank()) {
            Log.e("PhotosList", "토큰 획득 실패")
            return@LaunchedEffect
        }
        try {
            val req = Request.Builder()
                .url("${BuildConfig.BASE_URL}/photos/$guardianId")
                .addHeader("Authorization", "Bearer $idToken")
                .get()
                .build()
            val res = withContext(Dispatchers.IO) { client.newCall(req).execute() }
            if (!res.isSuccessful) {
                Log.e("PhotosList", "API 호출 실패: ${res.code}")
                return@LaunchedEffect
            }
            val bodyStr = res.body?.string().orEmpty()
            val arr = JSONObject(bodyStr).getJSONArray("photos")
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                photoList.add(
                    PhotoItem(
                        id = obj.getString("id"),
                        description = obj.getString("description"),
                        imageUrl = obj.getString("image_url"),
                        category = obj.getString("category"),
                        createdAt = obj.getString("created_at")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("PhotosList", "데이터 로드 실패: ${e.message}", e)
            Toast.makeText(context, "불러오기 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    // 화면 구성
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF0F2))
            .padding(16.dp)
    ) {
        // 로고 및 제목
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(80.dp)
                .align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "회상 정보 데이터 확인",
            fontSize = 24.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.height(16.dp))

        // 카테고리 필터
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(8.dp))
        ) {
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
        Spacer(Modifier.height(16.dp))

        // 필터 적용된 리스트 + 스크롤
        val filtered = remember(photoList, selectedCategory) {
            if (selectedCategory == "전체") photoList
            else photoList.filter { it.category == selectedCategory }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filtered, key = { it.id }) { item ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFFC9D8), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text(item.description, fontSize = 14.sp)
                    Spacer(Modifier.height(8.dp))

                    var imageModel by remember { mutableStateOf<String?>(null) }
                    LaunchedEffect(item.imageUrl) {
                        imageModel = try {
                            if (item.imageUrl.startsWith("https://storage.googleapis.com/")) item.imageUrl
                            else storage.getReferenceFromUrl(item.imageUrl)
                                .downloadUrl.await().toString()
                        } catch (e: Exception) {
                            Log.e("PhotosList", "URL 결정 실패", e)
                            null
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.LightGray),
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageModel != null) {
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "기억 사진",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        } else CircularProgressIndicator()
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "수정",
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
                        val idx = photoList.indexOfFirst { it.id == item.id }
                        if (idx != -1) {
                            photoList[idx] = item.copy(description = newDescription)
                            coroutineScope.launch {
                                val token = Firebase.auth.currentUser
                                    ?.getIdToken(true)?.await()?.token.orEmpty()
                                val json = JSONObject().put("description", newDescription)
                                Request.Builder()
                                    .url("${BuildConfig.BASE_URL}/photos/${item.id}")
                                    .addHeader("Authorization", "Bearer $token")
                                    .put(json.toString().toRequestBody("application/json".toMediaType()))
                                    .build()
                                    .also { req ->
                                        withContext(Dispatchers.IO) {
                                            client.newCall(req).execute().close()
                                        }
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
                            val token = Firebase.auth.currentUser
                                ?.getIdToken(true)?.await()?.token.orEmpty()
                            Request.Builder()
                                .url("${BuildConfig.BASE_URL}/photos/${item.id}")
                                .addHeader("Authorization", "Bearer $token")
                                .delete()
                                .build()
                                .also { req ->
                                    withContext(Dispatchers.IO) {
                                        client.newCall(req).execute().use { res ->
                                            if (res.isSuccessful) photoList.remove(item)
                                        }
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




















