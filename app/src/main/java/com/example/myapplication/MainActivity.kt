package com.example.myapplication

import com.example.myapplication.screens.EmotionListScreen

import com.example.myapplication.screens.HomeScreen
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.screens.*
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.viewmodel.EmotionViewModel
import com.example.myapplication.datastore.EmotionRepository
import com.example.myapplication.util.getOutputDirectory
import com.example.myapplication.util.uploadImageToServer
import java.io.File

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
                    composable("splash") { SplashScreen(navController) }
                    composable("home") { HomeScreen(navController, emotionViewModel) }

                    composable("camera1") {
                        CameraScreen(
                            onImageCaptured = { uri: Uri ->
                                val file = File(uri.path!!)
                                uploadImageToServer(
                                    context = context,
                                    file = file,
                                    onSuccess = {
                                        navController.navigate("result") // 또는 "analyzing"
                                    },
                                    onFailure = {
                                        // 실패 시 처리
                                    }
                                )
                            },
                            outputDirectory = getOutputDirectory(context)
                        )
                    }

                    composable("camera2") { Camera2Screen(navController) }
                    composable("result") { ResultScreen(navController) }
                    composable("emotion_list") { EmotionListScreen(navController, emotionViewModel) }
                }
            }
        }
    }
}

