package com.example.myapplication

import android.Manifest
import android.content.Context
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.screens.*
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.worker.WorkScheduler
import com.example.myapplication.viewmodel.QuizStatsViewModel

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
                // 온보딩 완료 여부 플래그
                val context = LocalContext.current
                val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
                var onboardingDone by remember {
                    // 테스트 중엔 항상 false로 시작
                    mutableStateOf(false)
                }

                // 네비게이션 컨트롤러
                val navController = rememberNavController()

                // 선택된 목소리 상태 (voiceId)
                var selectedVoice by remember { mutableStateOf<String?>(null) }

                // main2 경로 진입 시 온보딩이 안 되어 있으면 튜토리얼로 리다이렉트
                DisposableEffect(navController, onboardingDone) {
                    val listener = NavController.OnDestinationChangedListener { controller, destination, args ->
                        val route = destination.route
                        if (route != null && route.startsWith("main2/") && !onboardingDone) {
                            val patientId = args?.getString("patientId")
                            if (!patientId.isNullOrEmpty()) {
                                controller.navigate("onboarding/$patientId") {
                                    popUpTo(route) { inclusive = true }
                                }
                            }
                        }
                    }
                    navController.addOnDestinationChangedListener(listener)
                    onDispose {
                        navController.removeOnDestinationChangedListener(listener)
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = "splash"
                ) {
                    // 1회성 튜토리얼 (patientId를 받아서 Main_Page2로 복귀)
                    composable(
                        route = "onboarding/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        OnboardingScreen(
                            navController = navController,
                            patientId = patientId,
                            onFinish = {
                                // 완료 플래그 저장
                                prefs.edit().putBoolean("onboarding_completed", true).apply()
                                onboardingDone = true
                                // Main_Page2로 이동
                                navController.navigate("main2/$patientId") {
                                    popUpTo("onboarding/$patientId") { inclusive = true }
                                }
                            }
                        )
                    }

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
                    composable(
                        route = "recode/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        RecodeScreen(
                            navController = navController,
                            patientId = patientId,
                            onSelectVoice = { voiceId ->
                                selectedVoice = voiceId
                            }
                        )
                    }
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
                    composable(
                        route = "sentence/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        Patient_Sentence(
                            navController = navController,
                            patientId = patientId,
                            voiceId = selectedVoice ?: "default"
                        )
                    }
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





