package com.example.myapplication.screens

import android.net.Uri
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.myapplication.R // 여기에 sample_drawing이 있어야 함

@Composable
fun ResultScreen(navController: NavController) {
    val imageUri = navController.previousBackStackEntry?.savedStateHandle?.get<Uri>("imageUri")
    val analysisHome = navController.previousBackStackEntry?.savedStateHandle?.get<String>("analysisHome") ?: ""
    val analysisTree = navController.previousBackStackEntry?.savedStateHandle?.get<String>("analysisTree") ?: ""
    val analysisPerson = navController.previousBackStackEntry?.savedStateHandle?.get<String>("analysisPerson") ?: ""
    val summary = navController.previousBackStackEntry?.savedStateHandle?.get<String>("summary") ?: ""

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFFEF3))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "AI 그림 분석 결과",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF4B7A47),
            modifier = Modifier
                .background(Color(0xFFEAF4E0), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFB0C79A)),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
        ) {
            Image(
                painter = imageUri?.let { rememberAsyncImagePainter(it) }
                    ?: painterResource(id = R.drawable.sample_drawing),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "당신의 그림은...", color = Color(0xFF3B5F3B), style = MaterialTheme.typography.titleMedium)

        Spacer(modifier = Modifier.height(16.dp))

        AnalysisItem(icon = Icons.Default.Home, title = "자아와 안정감", content = analysisHome)
        AnalysisItem(icon = Icons.Default.Favorite, title = "활력과 성장 가능성", content = analysisTree)
        AnalysisItem(icon = Icons.Default.Person, title = "사회적 관계", content = analysisPerson)

        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFFB0C79A), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "종합 분석 요약",
                    color = Color(0xFF4B7A47),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = summary,
                    color = Color.Black,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(80.dp)) // 혹시 버튼 하단 공간 필요하면
    }
}

@Composable
fun AnalysisItem(icon: ImageVector, title: String, content: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFFB0C79A), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color(0xFF4B7A47),
            modifier = Modifier
                .size(28.dp)
                .padding(end = 12.dp)
        )
        Column {
            Text(text = title, color = Color(0xFF4B7A47), fontSize = 16.sp)
            Text(text = content, color = Color.Black, fontSize = 14.sp)
        }
    }
}
