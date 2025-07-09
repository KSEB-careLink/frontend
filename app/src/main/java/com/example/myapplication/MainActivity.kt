package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.screens.*
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.util.getOutputDirectory
import com.example.myapplication.util.uploadImageToServer
import com.example.myapplication.screens.GuardianSignInPage
import com.example.myapplication.screens.PatientSignInPage
import com.example.myapplication.screens.Login
import com.example.myapplication.screens.Patient_Sentence



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

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

                    // 로그인 페이지 (route는 "login"으로 소문자)
                    composable("login") {
                        Login(navController)
                    }

                    // 환자 문장 페이지
                    composable("sentence") {
                        Patient_Sentence(navController)
                    }

                    // 환자 퀴즈 페이지
                    composable("quiz") {
                        Patient_Quiz(navController)
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



