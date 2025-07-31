package com.example.myapplication

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.screens.*
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.worker.WorkScheduler
import com.example.myapplication.viewmodel.QuizStatsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // WorkManager 로 매일 9시~20시 1시간 간격 알림 스케줄링
        WorkScheduler.scheduleHourlyReminder(
            context = this,
            startHour = 9,
            endHour = 20
        )

        setContent {
            // Android 13+ 알림 권한 요청
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
                // 네비게이션 컨트롤러
                val navController = rememberNavController()

                // ★ 선택된 목소리 상태 (voiceId)
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
                    composable(
                        route = "code/{joinCode}",
                        arguments = listOf(navArgument("joinCode") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val joinCode = backStackEntry.arguments?.getString("joinCode").orEmpty()
                        Code(navController = navController, joinCode = joinCode)
                    }
                    composable(
                        route = "code2/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        Code2(navController, patientId)
                    }
                    composable("main") {
                        Main_Page(navController)
                    }
                    composable(
                        route = "main2/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        Main_Page2(navController = navController, patientId = patientId)
                    }
                    composable("alarm") {
                        Guardian_Alarm(navController)
                    }
                    composable(
                        route = "guardian_basic_info/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        GuardianBasicInfoScreen(patientId)
                    }
                    composable(
                        route = "memoryinfo/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        MemoryInfoInputScreen(navController, patientId)
                    }
                    composable(
                        route = "memorylist/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        MemoryInfoListScreen(navController, patientId)
                    }
                    // Recode(목소리 목록 + 선택) 화면
                    composable(
                        route = "recode/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        RecodeScreen(
                            navController = navController,
                            patientId = patientId,
                            // 선택 콜백: MainActivity.selectedVoice 업데이트
                            onSelectVoice = { voiceId ->
                                selectedVoice = voiceId
                            }
                        )
                    }

                    // Recode2(녹음) 화면
                    composable(
                        route = "recode2/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        Recode2(navController, patientId)
                    }

                    composable(
                        route = "location/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        LocationScreen(navController, patientId)
                    }
                    // 회상 문장 화면: 선택된 voiceId 넘겨주기
                    composable(
                        route = "sentence/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        // selectedVoice가 null이면 기본값("default")을 사용
                        Patient_Sentence(
                            navController = navController,
                            patientId = patientId,
                            voiceId = selectedVoice ?: "default"
                        )
                    }

                    // 회상 퀴즈 화면: 선택된 voiceId 넘겨주기
                    composable(
                        route = "quiz/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        Patient_Quiz(
                            navController = navController,
                            patientId = patientId,
                            voiceId = selectedVoice ?: "default"
                        )
                    }
                    composable(
                        route = "stats/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        val quizStatsViewModel: QuizStatsViewModel = viewModel()
                        LaunchedEffect(Unit) {
                            quizStatsViewModel.setPatientId(patientId)
                        }
                        QuizStatsScreen()
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




