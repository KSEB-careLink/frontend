package com.example.myapplication

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.screens.*
import com.example.myapplication.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                // ① 전체 음성 리스트 상태 (초기값은 "기본" 하나)
                var voices by remember { mutableStateOf(listOf("기본")) }

                // ② 선택된 음성 상태 (선택 시 이 값이 업데이트됩니다)
                var selectedVoice by remember { mutableStateOf<String?>(null) }

                NavHost(navController = navController, startDestination = "splash") {
                    // 스플래시
                    composable("splash") {
                        SplashScreen(navController)
                    }

                    // Guardian Sign-in
                    composable("guardian") {
                        GuardianSignInPage(navController)
                    }

                    // Patient Sign-in
                    composable("patient") {
                        PatientSignInPage(navController)
                    }

                    // 위치 선택 페이지
                    composable("choose") {
                        ChoosePositionPage(navController)
                    }

                    // 환자 로그인 페이지
                    composable("p_login") {
                        Patient_Login(navController)
                    }

                    // 보호자 로그인 페이지
                    composable("G_login") {
                        Guardian_Login(navController)
                    }

                    // 코드 페이지
                    composable("code") {
                        Code(navController)
                    }

                    // 메인 페이지
                    composable("main") {
                        Main_Page(navController)
                    }

                    // 메인2 페이지
                    composable("main2") {
                        Main_Page2(navController)
                    }

                    // Recode 화면: voices + onSelectVoice 전달
                    composable("recode") {
                        Recode(
                            navController = navController,
                            voices = voices,
                            onSelectVoice = { voiceName ->
                                // 사용자가 리스트에서 클릭했을 때 처리 로직
                                selectedVoice = voiceName
                                Log.d("Recode", "선택된 음성: $voiceName")
                            }
                        )
                    }

                    // 녹음2 페이지
                    composable("recode2") {
                        Recode2(navController)
                    }

                    // 환자 문장 페이지
                    composable("sentence") {
                        Patient_Sentence(navController)
                    }

                    // 환자 퀴즈 페이지
                    composable("quiz") {
                        Patient_Quiz(navController)
                    }

                    // 환자 알림 페이지
                    composable("alert") {
                        Patient_Alert(navController)
                    }

                    // 결과 페이지
                    composable("result") {
                        ResultScreen(navController)
                    }
                }
            }
        }
    }
}



