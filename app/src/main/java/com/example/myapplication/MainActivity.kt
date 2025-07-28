package com.example.myapplication

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.screens.*
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.worker.WorkScheduler
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // WorkManager 로 매일 9시~20시 1시간 간격 알림 스케줄링
        WorkScheduler.scheduleHourlyReminder(
            context   = this,
            startHour = 9,   // 오전 9시부터
            endHour   = 20   // 오후 8시까지
        )

        setContent {
            // Android 13+ 에서 필요: POST_NOTIFICATIONS 런타임 권한 요청
            val notificationPermissionLauncher =
                rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                    Log.d("MainActivity", "알림 권한 granted=$granted")
                }

            LaunchedEffect(Unit) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }

            MyApplicationTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                // ① 전체 음성 리스트 상태
                var voices by remember { mutableStateOf(listOf("기본")) }
                // ② 선택된 음성 상태
                var selectedVoice by remember { mutableStateOf<String?>(null) }

                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen(navController)
                    }
                    composable("guardianSignup") {
                        GuardianSignUpScreen(navController)
                    }
                    composable("patient") {
                        PatientSignUpScreen(navController)
                    }
                    composable("choose") {
                        ChoosePositionPage(navController)
                    }
                    composable("p_login") {
                        PatientLoginScreen(navController)
                    }
                    composable("G_login") {
                        Guardian_Login(navController)
                    }
                    composable("code/{joinCode}") { backStackEntry ->
                        val joinCode = backStackEntry.arguments?.getString("joinCode") ?: ""
                        Code(navController = navController, joinCode = joinCode)
                    }
                    composable("code2") {
                        Code2(navController)
                    }

                    // Main_Page: 기기(환자) 선택 화면
                    composable("main") {
                        Main_Page(navController)
                    }

                    // Main_Page2: 선택한 환자에 대한 보호자 홈
                    composable("main2/{patientId}") { backStackEntry ->
                        // 안전하게 patientId 꺼내기
                        val patientId = backStackEntry.arguments?.getString("patientId")
                            ?: return@composable

                        Main_Page2(navController = navController, patientId = patientId)
                    }
                    composable("alarm") {
                        Guardian_Alarm(navController)
                    }
                    composable("guardian_basic_info") {
                        GuardianBasicInfoScreen()
                    }
                    composable("memoryinfo") {
                        MemoryInfoInputScreen(navController)
                    }
                    composable("memorylist") {
                        MemoryInfoListScreen(navController)
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

                    composable("stats") {
                        val patientId = Firebase.auth.currentUser?.uid ?: return@composable
                        QuizStatsScreen(patientId = patientId)
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





