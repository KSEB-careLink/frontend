package com.example.myapplication.screens

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import com.example.myapplication.R

data class MemoryItem(val id: Int, var description: String, val imageResId: Int)

@Composable
fun MemoryInfoListScreen(navController: NavController) {
    val memoryList = remember {
        mutableStateListOf(
            MemoryItem(1, "1번 사진. 이 사진은 12월 25일 크리스마스...", R.drawable.photo1)
        )
    }

    var showDialog by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<MemoryItem?>(null) }
    var newDescription by remember { mutableStateOf("") }

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
                    top.linkTo(parent.top, margin = 80.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        Text(
            text = "회상 정보 데이터 확인",
            fontSize = 20.sp,
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
                    top.linkTo(title.bottom, margin = 24.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom)
                    height = Dimension.wrapContent
                }
                .fillMaxWidth()
                .heightIn(min = 100.dp, max = 500.dp)
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

                        Image(
                            painter = painterResource(id = item.imageResId),
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
                                    memoryList.remove(item)
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
                                val index = memoryList.indexOf(item)
                                if (index != -1) {
                                    memoryList[index] = item.copy(description = newDescription)
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
    }
}



