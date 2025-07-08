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
import com.example.myapplication.screens.PatientSigninPage



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()


        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                NavHost(navController = navController, startDestination = "splash") {
                    // 스플래시 화면
                    composable("splash") {
                        SplashScreen(
                            navController = navController
                        )
                    }
                    // 홈 화면
                    composable("home") {
                        HomeScreen(
                            navController = navController,
                        )
                    }
                    // Guardian Sign-in 페이지
                    composable("guardian") {
                        GuardianSignInPage(
                            navController = navController
                        )
                    }
                    // Patient Sign-in 페이지
                    composable("patient") {
                        PatientSignInPage(
                            navController = navController
                        )
                    }
                    // 결과 페이지
                    composable("result") {
                        ResultScreen(
                            navController = navController
                        )
                    }

                    // 위치 선택 페이지
                    composable("choose") {
                        ChoosePositionPage(
                            navController = navController
                        )
                    }
                }
            }
        }
    }
}

