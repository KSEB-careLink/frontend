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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import androidx.compose.foundation.Image
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

// SharedPreferences에서 환자 ID 가져오기 헬퍼
fun getPatientIdFromPrefs(context: Context): String? =
    context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        .getString("patient_id", null)

// 메모리 아이템 데이터 클래스 (category 추가)
data class MemoryItemCloud(
    val id: String,
    var description: String,
    val mediaUrl: String,
    val mediaType: String,
    val category: String
)

@Composable
fun MemoryInfoListScreen(navController: NavController, patientId: String) {
    val memoryList = remember { mutableStateListOf<MemoryItemCloud>() }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    // 카테고리 필터 상태
    val categoryOptions = listOf("가족", "동네", "학창시절", "여행", "환자가 좋아하는 것\n")
    var selectedCategory by remember { mutableStateOf("전체") }
    var expanded by remember { mutableStateOf(false) }

    // 수정/삭제 다이얼로그 상태
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MemoryItemCloud?>(null) }
    var newDescription by remember { mutableStateOf("") }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<MemoryItemCloud?>(null) }

    // 데이터 로드
    LaunchedEffect(Unit) {
        val pid = getPatientIdFromPrefs(context).orEmpty()
        if (pid.isBlank()) {
            Log.e("MemoryList", "환자 ID 없음")
            return@LaunchedEffect
        }
        val idToken = Firebase.auth.currentUser?.getIdToken(true)?.await()?.token.orEmpty()
        if (idToken.isBlank()) {
            Log.e("MemoryList", "토큰 획득 실패")
            return@LaunchedEffect
        }
        try {
            val req = Request.Builder()
                .url("${BuildConfig.BASE_URL}/memory/list/$pid")
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
                        mediaUrl = obj.getString("mediaUrl"),
                        mediaType = obj.getString("mediaType"),
                        category = obj.getString("category")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e("MemoryList", "데이터 로드 실패: ${e.message}")
            Toast.makeText(context, "불러오기 실패: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val (logo, title, filterLabel, filterDropdown, listBox) = createRefs()

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
            modifier = Modifier.constrainAs(title) {
                top.linkTo(logo.bottom, margin = 12.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        Text(
            text = "카테고리 필터:",
            fontSize = 14.sp,
            modifier = Modifier.constrainAs(filterLabel) {
                top.linkTo(title.bottom, margin = 16.dp)
                start.linkTo(parent.start)
            }
        )

        Box(
            modifier = Modifier
                .constrainAs(filterDropdown) {
                    top.linkTo(filterLabel.bottom, margin = 4.dp)
                    start.linkTo(parent.start)
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

        Box(
            modifier = Modifier
                .constrainAs(listBox) {
                    top.linkTo(filterDropdown.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.fillToConstraints
                }
                .fillMaxWidth()
                .background(Color(0xFFFDEFF1), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            val filtered = remember(memoryList, selectedCategory) {
                if (selectedCategory == "전체") memoryList
                else memoryList.filter { it.category == selectedCategory }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filtered, key = { it.id }) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFC9D8), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(item.description, fontSize = 14.sp, color = Color.Black)
                        Spacer(Modifier.height(8.dp))
                        AsyncImage(
                            model = item.mediaUrl,
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
                        val idx = memoryList.indexOfFirst { it.id == item.id }
                        if (idx != -1) {
                            memoryList[idx] = item.copy(description = newDescription)
                            coroutineScope.launch {
                                val pid = getPatientIdFromPrefs(context).orEmpty()
                                val token = Firebase.auth.currentUser
                                    ?.getIdToken(true)?.await()?.token.orEmpty()
                                val json = JSONObject().put("description", newDescription)
                                val req = Request.Builder()
                                    .url("${BuildConfig.BASE_URL}/memory/$pid/${item.id}")
                                    .addHeader("Authorization", "Bearer $token")
                                    .put(
                                        json.toString()
                                            .toRequestBody("application/json".toMediaType())
                                    )
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
                            val pid = getPatientIdFromPrefs(context).orEmpty()
                            val token = Firebase.auth.currentUser
                                ?.getIdToken(true)?.await()?.token.orEmpty()
                            withContext(Dispatchers.IO) {
                                client.newCall(
                                    Request.Builder()
                                        .url("${BuildConfig.BASE_URL}/memory/$pid/${item.id}")
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
















