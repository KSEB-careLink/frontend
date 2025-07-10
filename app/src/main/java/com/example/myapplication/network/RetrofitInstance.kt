// app/src/main/java/com/example/myapplication/network/RetrofitInstance.kt
package com.example.myapplication.network

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.runBlocking                  // ← 추가
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    private const val BASE_URL = "http://10.0.2.2:8000"

    private val authInterceptor = Interceptor { chain ->
        // runBlocking으로 감싸서 suspend 함수를 동기 호출
        val token = runBlocking {
            Firebase.auth.currentUser
                ?.getIdToken(false)
                ?.await()
                ?.token
        }

        val newReq: Request = chain.request().newBuilder()
            .apply { token?.let { addHeader("Authorization", "Bearer $it") } }
            .build()

        chain.proceed(newReq)
    }

    // 2) OkHttpClient에 인터셉터 등록
    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    // 3) Retrofit 인스턴스 & ApiService 생성
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}


