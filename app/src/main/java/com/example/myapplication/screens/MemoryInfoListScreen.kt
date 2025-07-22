package com.example.myapplication.screens

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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.myapplication.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import androidx.compose.foundation.Image
import com.google.firebase.firestore.Query
import android.util.Log
import androidx.compose.ui.platform.LocalContext

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
    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MemoryItemCloud?>(null) }
    var newDescription by remember { mutableStateOf("") }

    var itemToDelete by remember { mutableStateOf<MemoryItemCloud?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Firebase에서 데이터 로드
    LaunchedEffect(Unit) {
        val db = FirebaseFirestore.getInstance()
        val storage = FirebaseStorage.getInstance()

        val currentPatientId = getPatientIdFromPrefs(context) // SharedPreferences에서 불러오기
        db.collection("memoryItems")
            .whereEqualTo("patientId", currentPatientId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()

            .addOnSuccessListener { snapshot ->
                if (snapshot.isEmpty) {
                    Log.d("MemoryList", "🔥 Firestore: No memory items found.")
                }

                snapshot.documents.forEach { doc ->
                    val description = doc.getString("description") ?: ""
                    val mediaPath = doc.getString("mediaPath") ?: ""
                    val id = doc.id

                    Log.d("MemoryList", "📄 Found doc: id=$id, path=$mediaPath")

                    val storageRef = FirebaseStorage.getInstance().getReference(mediaPath)
                    storageRef.downloadUrl
                        .addOnSuccessListener { uri ->
                            memoryList.add(
                                MemoryItemCloud(
                                    id = id,
                                    description = description,
                                    imageUrl = uri.toString(),
                                    mediaPath = mediaPath
                                )
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.e(
                                "MemoryList",
                                "❌ Failed to get downloadUrl for $mediaPath: ${e.message}"
                            )
                        }
                }
            }
            .addOnFailureListener {
                Log.e("MemoryList", "❌ Firestore query failed: ${it.message}")
            }
    }

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
                        Text(
                            text = item.description,
                            fontSize = 14.sp,
                            color = Color.Black
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        AsyncImage(
                            model = item.imageUrl,
                            contentDescription = "기억 사진",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )

                        Spacer(modifier = Modifier.height(8.dp))

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

        // 설명 수정 다이얼로그
        if (showDialog && editingItem != null) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text(text = "설명 수정") },
                text = {
                    OutlinedTextField(
                        value = newDescription,
                        onValueChange = { newDescription = it },
                        label = { Text("설명") },
                        singleLine = false,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            editingItem?.let { item ->
                                val index = memoryList.indexOfFirst { it.id == item.id }
                                if (index != -1) {
                                    memoryList[index] = item.copy(description = newDescription)
                                    FirebaseFirestore.getInstance()
                                        .collection("memoryItems")
                                        .document(item.id)
                                        .update("description", newDescription)
                                }
                            }
                            showDialog = false
                        }
                    ) {
                        Text("저장")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) {
                        Text("취소")
                    }
                }
            )
        }

        // 삭제 확인 다이얼로그
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
                            FirebaseFirestore.getInstance()
                                .collection("memoryItems")
                                .document(item.id)
                                .delete()
                            FirebaseStorage.getInstance()
                                .getReference(item.mediaPath)
                                .delete()
                            memoryList.remove(item)
                        }
                        showDeleteConfirm = false
                        itemToDelete = null
                    }) {
                        Text("삭제", color = Color.Red)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showDeleteConfirm = false
                        itemToDelete = null
                    }) {
                        Text("취소")
                    }
                }
            )
        }
    }
}




