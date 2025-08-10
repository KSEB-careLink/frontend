package com.example.myapplication

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import com.example.myapplication.viewmodel.QuizStatsViewModel

class MainActivity : ComponentActivity() {

    // 🔧 lateint → nullable + 등록 플래그
    private var receiver: LockOverlayReceiver? = null
    private var isReceiverRegistered = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 잠금화면에 뜨게하는 채널 생성
        LockOverlayNotificationHelper.createChannel(this)


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
                                // memoryinfo로 이동
                                navController.navigate("memoryinfo/{patientId}") {
                                    popUpTo("onboarding/$patientId") { inclusive = true }
                                }
                            }
                        )
                    }

                    composable("splash") { SplashScreen(navController) }

                    composable("guardianSignup") { GuardianSignUpScreen(navController) }
                    composable("patient") { PatientSignUpScreen(navController) }
                    composable("choose") { ChoosePositionPage(navController) }
                    composable("p_login") { PatientLoginScreen(navController) }
                    composable("G_login") { Guardian_Login(navController) }

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

                    composable("main") { Main_Page(navController) }

                    composable(
                        route = "main2/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        Main_Page2(navController = navController, patientId = patientId)
                    }
//                    composable("alarm") {
//                        Guardian_Alarm(navController)
//                    }

                    composable(
                        route = "alarm/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        Guardian_Alarm(navController = navController, patientId = patientId)
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
                            onSelectVoice = { voiceId -> selectedVoice = voiceId }
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

                        )
                    }

                    composable(
                        route = "quiz/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        Patient_Quiz(navController = navController, patientId = patientId)
                    }

                    composable(
                        route = "stats/{patientId}",
                        arguments = listOf(navArgument("patientId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val patientId = backStackEntry.arguments?.getString("patientId") ?: return@composable
                        val quizStatsViewModel: QuizStatsViewModel = viewModel()
                        LaunchedEffect(Unit) { quizStatsViewModel.setPatientId(patientId) }
                        QuizStatsScreen()
                    }

                    composable("alert") { Patient_Alert(navController) }
                }
            }
        }
    }

    // 🔒 동적 리시버 등록/해제
    override fun onStart() {
        super.onStart()
        if (receiver == null) receiver = LockOverlayReceiver()
        if (!isReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("DEPRECATION")
                registerReceiver(receiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    override fun onStop() {
        // onStop에서 먼저 해제 → 중복 해제 방지
        if (isReceiverRegistered) {
            try { unregisterReceiver(receiver) } catch (_: IllegalArgumentException) {}
            isReceiverRegistered = false
        }
        super.onStop()
    }

    override fun onDestroy() {
        // 혹시 남아있으면 한 번 더 안전하게
        if (isReceiverRegistered) {
            try { unregisterReceiver(receiver) } catch (_: IllegalArgumentException) {}
            isReceiverRegistered = false
        }
        receiver = null
        super.onDestroy()
    }
}






