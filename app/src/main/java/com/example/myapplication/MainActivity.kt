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
import androidx.navigation.navArgument
import com.example.myapplication.viewmodel.QuizStatsViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

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

                var voices by remember { mutableStateOf(listOf("기본")) }
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

                    composable(
                        "code2/{patientId}",
                        arguments = listOf(navArgument("patientId") {
                            type = NavType.StringType
                        })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId")
                            ?: return@composable
                        Code2(navController, patientId)
                    }

                    composable("main") {
                        Main_Page(navController)
                    }
                    composable(
                        "main2/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        Main_Page2(navController = navController, patientId = patientId)
                    }
                    composable("alarm") {
                        Guardian_Alarm(navController)
                    }
                    composable(
                        "guardian_basic_info/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        GuardianBasicInfoScreen(patientId)
                    }
                    composable(
                        "memoryinfo/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        MemoryInfoInputScreen(navController, patientId)
                    }
                    composable(
                        "memorylist/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        MemoryInfoListScreen(navController, patientId)
                    }
                    composable(
                        "recode/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        Recode(
                            navController = navController,
                            patientId = patientId,
                            voices = voices,
                            onSelectVoice = { selectedVoice = it }
                        )
                    }
                    composable(
                        "recode2/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        Recode2(navController, patientId)
                    }

                    composable(
                        "location/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        LocationScreen(navController, patientId)
                    }
                    composable(
                        "sentence/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        Patient_Sentence(navController, patientId)
                    }
                    composable(
                        "quiz/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        Patient_Quiz(navController, patientId)
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

//                        QuizStatsScreen(quizStatsViewModel = quizStatsViewModel)
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





