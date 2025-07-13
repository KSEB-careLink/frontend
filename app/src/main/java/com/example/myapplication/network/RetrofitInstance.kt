// app/src/main/java/com/example/myapplication/network/RetrofitInstance.kt
package com.example.myapplication.network

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {

    // ▶️ 반드시 끝에 슬래시(/) 추가
    private const val BASE_URL = "http://10.0.2.2:3000/"

    private val authInterceptor = Interceptor { chain ->
        // ▶️ 토큰 획득부를 안전하게 감싸기
        val token: String? = try {
            runBlocking {
                Firebase.auth.currentUser
                    ?.getIdToken(false)
                    ?.await()
                    ?.token
            }
        } catch (e: Exception) {
            Log.w("RetrofitInstance", "토큰 가져오기 실패, 인증 헤더 없이 요청 진행", e)
            null
        }

        // 헤더에 토큰이 있을 때만 추가
        val newReq: Request = chain.request().newBuilder().apply {
            token?.let { addHeader("Authorization", "Bearer $it") }
        }.build()

        chain.proceed(newReq)
    }

    // 2) OkHttpClient에 인터셉터 등록
    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .build()

    // 3) Retrofit 인스턴스 & ApiService 생성
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)  // ▶️ 여기서 슬래시 뒤에 상대경로가 정확히 붙음
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}



