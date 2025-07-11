// app/src/main/java/com/example/myapplication/network/RetrofitInstance.kt
package com.example.myapplication.network

import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.runBlocking       // runBlocking 임포트
import kotlinx.coroutines.tasks.await      // await 확장 함수
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // ⚠️ 백엔드가 3000번 포트에서 구동 중이므로 8000이 아니라 3000으로 맞춰야 합니다.
    private const val BASE_URL = "http://10.0.2.2:3000"

    // Firebase ID 토큰을 동기적으로 가져오기 위해 runBlocking 사용
    private val authInterceptor = Interceptor { chain ->
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

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}



