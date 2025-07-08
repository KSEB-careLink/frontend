package com.example.carelink.util

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File

fun uploadImageToServer(
    context: Context,
    file: File,
    onSuccess: () -> Unit,
    onFailure: () -> Unit
) {
    val mediaType = "image/*".toMediaTypeOrNull()
    val requestBody = file.asRequestBody(mediaType)
    val body = MultipartBody.Part.createFormData("image", file.name, requestBody)

    val interceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    val client = OkHttpClient.Builder()
        .addInterceptor(interceptor)
        .build()

    val retrofit = Retrofit.Builder()
        .baseUrl("http://your-server-url.com/") // ← 수정 필요
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val api = retrofit.create(UploadApi::class.java)

    CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = api.uploadImage(body)
            if (response.isSuccessful) {
                onSuccess()
            } else {
                onFailure()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onFailure()
        }
    }
}
