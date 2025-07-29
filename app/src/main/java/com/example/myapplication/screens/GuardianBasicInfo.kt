package com.example.myapplication.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun GuardianBasicInfoScreen() {
    var name by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var relationship by remember { mutableStateOf("") }
    var tone by remember { mutableStateOf("다정한") }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val client = remember { OkHttpClient() }

    fun savePatientIdToPrefs(context: Context, patientId: String) {
        val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        prefs.edit().putString("patient_id", patientId).apply()
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp)
    ) {
        val (
            logo, title, nameLabel, nameInput,
            birthdayLabel, birthdayInput,
            question1, relationshipInput,
            question2, subText, toneButtons, submitButton
        ) = createRefs()

        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "Logo",
            modifier = Modifier
                .size(80.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        Text(
            "기본 정보 입력",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(logo.bottom, margin = 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        Text(
            "이름",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(nameLabel) {
                top.linkTo(title.bottom, margin = 32.dp)
                start.linkTo(parent.start)
            }
        )

        TextField(
            value = name,
            onValueChange = { name = it },
            placeholder = { Text("홍길동") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .constrainAs(nameInput) {
                    top.linkTo(nameLabel.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(56.dp)
        )

        Text(
            "생일",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(birthdayLabel) {
                top.linkTo(nameInput.bottom, margin = 24.dp)
                start.linkTo(parent.start)
            }
        )

        TextField(
            value = birthday,
            onValueChange = { birthday = it },
            placeholder = { Text("예: 1990-01-01") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .constrainAs(birthdayInput) {
                    top.linkTo(birthdayLabel.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(56.dp)
        )

        Text(
            "1. 보호 대상을 부르는 호칭을 알려주세요",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(question1) {
                top.linkTo(birthdayInput.bottom, margin = 24.dp)
                start.linkTo(parent.start)
            }
        )

        TextField(
            value = relationship,
            onValueChange = { relationship = it },
            placeholder = { Text("기타") },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFFFEBEE),
                unfocusedContainerColor = Color(0xFFFFEBEE)
            ),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .constrainAs(relationshipInput) {
                    top.linkTo(question1.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(56.dp)
        )

        Text(
            "2. 원하는 말투",
            fontSize = 16.sp,
            color = Color.Black,
            modifier = Modifier.constrainAs(question2) {
                top.linkTo(relationshipInput.bottom, margin = 32.dp)
                start.linkTo(parent.start)
            }
        )

        Text(
            "(의상 문장 생성, 보호자 음성에 이용)",
            fontSize = 12.sp,
            color = Color.Gray,
            modifier = Modifier.constrainAs(subText) {
                top.linkTo(question2.bottom, margin = 4.dp)
                start.linkTo(parent.start)
            }
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .constrainAs(toneButtons) {
                    top.linkTo(subText.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
        ) {
            listOf("다정한", "밝은", "차분한").forEach { option ->
                val selected = tone == option
                Button(
                    onClick = { tone = option },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) Color(0xFF9C27B0) else Color(0xFFE1BEE7),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                ) {
                    Text(option)
                }
            }
        }

        Button(
            onClick = {
                coroutineScope.launch {
                    val user = Firebase.auth.currentUser
                    if (user == null) {
                        Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val idToken = user.getIdToken(true).await().token
                    if (idToken.isNullOrBlank()) {
                        Toast.makeText(context, "토큰 가져오기 실패", Toast.LENGTH_SHORT).show()
                        return@launch
                    }

                    val json = JSONObject().apply {
                        put("patientUid", targetPatientUid)
                        put("name", name)
                        put("birthDate", birthday)
                        put("relationship", relationship)
                        put("tone", tone)
                    }

                    val requestBody = json.toString()
                        .toRequestBody("application/json".toMediaType())

                    val request = Request.Builder()
                        .url("${BuildConfig.BASE_URL}/register/register")
                        .addHeader("Authorization", "Bearer $idToken")
                        .post(requestBody)
                        .build()

                    try {
                        withContext(Dispatchers.IO) {
                            client.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    val responseBody = response.body?.string()
                                    val responseJson = JSONObject(responseBody ?: "")
                                    val patientId = responseJson.getString("patientId")
                                    savePatientIdToPrefs(context, patientId)
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "등록 완료", Toast.LENGTH_SHORT).show()
                                        // navController.navigate(...) 가능
                                    }
                                } else {
                                    val error = response.body?.string()
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "실패: $error", Toast.LENGTH_LONG).show()
                                    }
                                }
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        Toast.makeText(context, "예외 발생: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4DD0E1)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .constrainAs(submitButton) {
                    top.linkTo(toneButtons.bottom, margin = 40.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(48.dp)
        ) {
            Text("완료", color = Color.White, fontSize = 16.sp)
        }

    }
}



