package com.example.myapplication.screens

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension

@Composable
fun Guardian_Login(navController: NavController) {
    val auth = Firebase.auth
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        val (logo, textLogo, title, emailLabel, emailField, passwordLabel, passwordField,
            loginButton, registerButton) = createRefs()

        // 로고
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(160.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 64.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        // 텍스트 로고
        Image(
            painter = painterResource(id = R.drawable.ai_text),
            contentDescription = "텍스트 로고",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .size(120.dp)
                .constrainAs(textLogo) {
                    top.linkTo(logo.bottom, margin = 24.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        // 타이틀
        Text(
            text = "보호자 로그인",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(textLogo.bottom, margin = 32.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        // 이메일 라벨
        Text(
            text = "이메일 주소",
            fontSize = 16.sp,
            modifier = Modifier.constrainAs(emailLabel) {
                top.linkTo(title.bottom, margin = 24.dp)
                start.linkTo(parent.start)
            }
        )

        // 이메일 입력
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            placeholder = { Text("example@mail.com") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(emailField) {
                    top.linkTo(emailLabel.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            singleLine = true
        )

        // 비밀번호 라벨
        Text(
            text = "비밀번호",
            fontSize = 16.sp,
            modifier = Modifier.constrainAs(passwordLabel) {
                top.linkTo(emailField.bottom, margin = 16.dp)
                start.linkTo(parent.start)
            }
        )

        // 비밀번호 입력
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            placeholder = { Text("••••••••") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(passwordField) {
                    top.linkTo(passwordLabel.bottom, margin = 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            singleLine = true
        )

        // 로그인 버튼
        Button(
            onClick = {
                coroutineScope.launch {
                    try {
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "로그인 성공", Toast.LENGTH_SHORT).show()
                                    navController.navigate("guardian")
                                } else {
                                    Toast.makeText(context, "로그인 실패", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } catch (e: Exception) {
                        Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(loginButton) {
                    top.linkTo(passwordField.bottom, margin = 32.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("로그인", color = Color.White, fontSize = 16.sp)
        }

        // 회원가입 버튼
        Button(
            onClick = { navController.navigate("guardian") },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .constrainAs(registerButton) {
                    top.linkTo(loginButton.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                },
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C4B4))
        ) {
            Text("회원가입", color = Color.White, fontSize = 16.sp)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewLogin2() {
    Guardian_Login(navController = rememberNavController())
}

