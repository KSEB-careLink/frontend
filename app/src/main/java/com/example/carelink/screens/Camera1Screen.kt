package com.example.carelink.screens

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import android.content.Context
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.carelink.R
import kotlinx.coroutines.*
import okhttp3.*
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun Camera1NavGraph(navController: NavHostController) {
    val context = LocalContext.current
    NavHost(navController = navController, startDestination = "camera") {
        composable("camera") {
            CameraScreen(
                onImageCaptured = { uri ->
                    val file = File(uri.path!!)
                    uploadImageToServer(
                        context = context,
                        file = file,
                        onSuccess = { navController.navigate("analyzing") },
                        onFailure = { }
                    )
                },
                outputDirectory = getOutputDirectory(context)
            )
        }
        composable("analyzing") {
            CameraAnalyzingScreen()
        }
    }
}

@Composable
fun CameraScreen(
    onImageCaptured: (Uri) -> Unit,
    outputDirectory: File
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        imageCapture = ImageCapture.Builder().build()
        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner, cameraSelector, preview, imageCapture
            )
        } catch (e: Exception) {
            Log.e("CameraX", "Camera binding failed", e)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView({ previewView }, modifier = Modifier.fillMaxSize())
        CameraOverlay(onCaptureClick = {
            imageCapture?.let { capture ->
                val photoFile = File(
                    outputDirectory,
                    SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date()) + ".jpg"
                )
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
                capture.takePicture(
                    outputOptions,
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            onImageCaptured(Uri.fromFile(photoFile))
                        }
                        override fun onError(exception: ImageCaptureException) {
                            Toast.makeText(context, "촬영 실패: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        })
    }
}

@Composable
fun CameraOverlay(onCaptureClick: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(R.drawable.background), contentDescription = null, modifier = Modifier.fillMaxSize())
        Image(painter = painterResource(R.drawable.a4_guide), contentDescription = null, modifier = Modifier.fillMaxSize())
        Image(painter = painterResource(R.drawable.ai_statement), contentDescription = null, modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp))
        Image(painter = painterResource(R.drawable.ai_noti), contentDescription = null, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
        IconButton(onClick = onCaptureClick, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp)) {
            Image(painter = painterResource(R.drawable.cam_button), contentDescription = "Capture")
        }
    }
}

@Composable
fun CameraAnalyzingScreen() {
    val offsetY by rememberInfiniteTransition().animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Image(painter = painterResource(R.drawable.background), contentDescription = null, modifier = Modifier.fillMaxSize())
        Image(painter = painterResource(R.drawable.a4_guide), contentDescription = null, modifier = Modifier.fillMaxSize())
        Image(painter = painterResource(R.drawable.image_load), contentDescription = null, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp))
        Image(painter = painterResource(R.drawable.ai_statement), contentDescription = null, modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp))
        Image(painter = painterResource(R.drawable.scan_line), contentDescription = null, modifier = Modifier.align(Alignment.TopCenter).offset(y = offsetY.dp))
        Image(painter = painterResource(R.drawable.scan_move), contentDescription = null, modifier = Modifier.align(Alignment.TopCenter).offset(y = offsetY.dp))
    }
}

interface ApiService {
    @Multipart
    @POST("/upload")
    suspend fun uploadImage(@Part image: MultipartBody.Part): retrofit2.Response<ResponseBody>
}

object RetrofitClient {
    private const val BASE_URL = "https://your-server.com"

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(logging)
        .build()

    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
}

fun uploadImageToServer(context: Context, file: File, onSuccess: () -> Unit, onFailure: () -> Unit) {
    val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
    val body = MultipartBody.Part.createFormData("image", file.name, requestFile)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitClient.apiService.uploadImage(body)
            withContext(Dispatchers.Main) {
                if (response.isSuccessful) {
                    Toast.makeText(context, "업로드 성공!", Toast.LENGTH_SHORT).show()
                    onSuccess()
                } else {
                    Toast.makeText(context, "업로드 실패: ${response.code()}", Toast.LENGTH_SHORT).show()
                    onFailure()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "오류 발생: ${e.message}", Toast.LENGTH_SHORT).show()
                onFailure()
            }
        }
    }
}

fun getOutputDirectory(context: Context): File {
    val mediaDir = context.externalMediaDirs.firstOrNull()?.let {
        File(it, context.getString(R.string.app_name)).apply { mkdirs() }
    }
    return mediaDir ?: context.filesDir
}
