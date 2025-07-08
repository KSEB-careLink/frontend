package com.example.carelink

import android.content.Context
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
import com.example.carelink.datastore.EmotionRepository
import com.example.carelink.screens.*
import com.example.carelink.ui.theme.MyApplicationTheme
import com.example.carelink.viewmodel.EmotionViewModel

// Context 확장
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "emotion_prefs")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = EmotionRepository(applicationContext.dataStore)
        val emotionViewModel = EmotionViewModel(repository)

        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val context = LocalContext.current

                NavHost(navController = navController, startDestination = "splash") {
                    composable("splash") {
                        SplashScreen(navController)
                    }
                    composable("home") {
                        HomeScreen(navController, emotionViewModel)
                    }
                    composable("camera1") {

                    composable("camera2") {
                        Camera2Screen(navController)
                    }
                    composable("result") {
                        ResultScreen(navController)
                    }
                    composable("emotion_list") {
                        EmotionListScreen(navController, emotionViewModel)
                    }
                    // ★ Choose 화면 등록 (함수명이랑 route 이름을 동일하게)
                    composable("choose") {
                        ChoosePositionPage(navController)
                    }
                }
            }
        }
    }
}