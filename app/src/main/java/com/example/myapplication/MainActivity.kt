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
        // 🔧 반드시 첫 줄에 호출해야 앱이 크래시나지 않음
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                // 전체 음성 리스트 상태 (초기값은 "기본" 하나)
                var voices by remember { mutableStateOf(listOf("기본")) }

                // 선택된 음성 상태
                var selectedVoice by remember { mutableStateOf<String?>(null) }

                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen(navController)
                    }

                    composable("guardian") {
                        GuardianSignInPage(navController)
                    }
                    

                    composable("choose") {
                        ChoosePositionPage(navController)
                    }

                    composable("p_login") {
                        PatientLinkPage(navController)
                    }

                    composable("G_login") {
                        Guardian_Login(navController)
                    }

                    composable("code") {
                        Code(navController)
                    }

                    composable("main") {
                        Main_Page(navController)
                    }

                    composable("main2") {
                        Main_Page2(navController)
                    }

                    composable("alarm") {
                        Guardian_Alarm(navController)
                    }

                    composable("guardian_basic_info") {
                        GuardianBasicInfoScreen()
                    }

                    composable("memoryinfo") {
                        MemoryInputScreen()
                    }

                    composable("memoryinfolist") {
                        MemoryInfoListScreen()
                    }

                    composable("recode") {
                        Recode(
                            navController = navController,
                            voices = voices,
                            onSelectVoice = { voiceName ->
                                selectedVoice = voiceName
                                Log.d("Recode", "선택된 음성: $voiceName")
                            }
                        )
                    }

                    composable("recode2") {
                        Recode2(navController)
                    }

                    composable("location") {
                        LocationScreen(navController)
                    }

                    composable("sentence") {
                        Patient_Sentence(navController)
                    }

                    composable("quiz") {
                        Patient_Quiz(navController)
                    }

                    composable("alert") {
                        Patient_Alert(navController)
                    }

                    composable("result") {
                        ResultScreen(navController)
                    }
                }
            }
        }
    }
}




