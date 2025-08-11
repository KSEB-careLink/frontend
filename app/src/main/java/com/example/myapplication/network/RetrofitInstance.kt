//package com.example.myapplication.network
//
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import okhttp3.OkHttpClient
//import okhttp3.Interceptor
//import okhttp3.Authenticator
//import okhttp3.Request
//import okhttp3.Response
//import okhttp3.Route
//import com.google.android.gms.tasks.Tasks
//import com.google.firebase.auth.FirebaseAuth
//// (선택) 네트워크 로그가 필요하면 다음 라인 추가
//// import okhttp3.logging.HttpLoggingInterceptor
//
//object RetrofitInstance {
//
//    // ngrok / 고정 서버 중 하나만 사용하세요.
//    private const val BASE_URL = "https://pleasing-really-cow.ngrok-free.app/"
//    // private const val BASE_URL = "https://backend-f61l.onrender.com/"
//
//    /** 1) 모든 요청에 Authorization 헤더 자동 첨부 */
//    private class AuthHeaderInterceptor : Interceptor {
//        override fun intercept(chain: Interceptor.Chain): Response {
//            val original = chain.request()
//            val user = FirebaseAuth.getInstance().currentUser
//            val token = try {
//                if (user != null) Tasks.await(user.getIdToken(false)).token else null
//            } catch (_: Exception) { null }
//
//            val authed = if (!token.isNullOrBlank()) {
//                original.newBuilder()
//                    .header("Authorization", "Bearer $token")
//                    .build()
//            } else {
//                original
//            }
//            return chain.proceed(authed)
//        }
//    }
//
//    /** 2) 401 이면 토큰 강제 갱신 후 1회 재시도 */
//    private class FirebaseAuthenticator : Authenticator {
//        override fun authenticate(route: Route?, response: Response): Request? {
//            // 무한 루프 방지
//            if (responseCount(response) >= 2) return null
//
//            val user = FirebaseAuth.getInstance().currentUser ?: return null
//            val newToken = try {
//                Tasks.await(user.getIdToken(true)).token
//            } catch (_: Exception) { null } ?: return null
//
//            return response.request.newBuilder()
//                .header("Authorization", "Bearer $newToken")
//                .build()
//        }
//
//        private fun responseCount(resp: Response): Int {
//            var r: Response? = resp
//            var count = 1
//            while (r?.priorResponse != null) {
//                count++
//                r = r.priorResponse
//            }
//            return count
//        }
//    }
//
//    private val client by lazy {
//        val builder = OkHttpClient.Builder()
//            .addInterceptor(AuthHeaderInterceptor())
//            .authenticator(FirebaseAuthenticator())
//
//        // (선택) 디버깅용 네트워크 로그
//        // val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
//        // builder.addInterceptor(logging)
//
//        builder.build()
//    }
//
//    private val retrofit by lazy {
//        Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .client(client) // ✅ 반드시 넣어야 인터셉터/Authenticator가 작동합니다.
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//    }
//
//    val api: AlarmApi by lazy {
//        retrofit.create(AlarmApi::class.java)
//    }
//}
package com.example.myapplication.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.Interceptor
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
// 필요하면 네트워크 로그 켜기
// import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

object RetrofitInstance {

    // 고정/NGROK 중 하나만 사용
    private const val BASE_URL = "https://pleasing-really-cow.ngrok-free.app/"
    // private const val BASE_URL = "https://backend-f61l.onrender.com/"

    /** 모든 요청에 Firebase ID 토큰을 Authorization 헤더로 자동 첨부 */
    private class AuthHeaderInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val user = FirebaseAuth.getInstance().currentUser
            val token = try {
                if (user != null) Tasks.await(user.getIdToken(false)).token else null
            } catch (_: Exception) { null }

            val authed = if (!token.isNullOrBlank()) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }
            return chain.proceed(authed)
        }
    }

    /** 401(auth/id-token-expired 등)일 때 토큰 강제 갱신 후 1회 재시도 */
    private class FirebaseAuthenticator : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            // 무한 재시도 방지
            if (responseCount(response) >= 2) return null

            val user = FirebaseAuth.getInstance().currentUser ?: return null
            val newToken = try {
                Tasks.await(user.getIdToken(true)).token
            } catch (_: Exception) { null } ?: return null

            return response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        }

        private fun responseCount(resp: Response): Int {
            var r: Response? = resp
            var count = 1
            while (r?.priorResponse != null) {
                count++
                r = r.priorResponse
            }
            return count
        }
    }

    private val client by lazy {
        val builder = OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor())
            .authenticator(FirebaseAuthenticator())
            .retryOnConnectionFailure(true)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)

        // 디버그용 로그가 필요하면 주석 해제
        // val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        // builder.addInterceptor(logging)

        builder.build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // <- 인터셉터/Authenticator 적용
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // AlarmApi 한 번만 생성해서 알림/긴급/지오펜스 모두 사용
    val api: AlarmApi by lazy {
        retrofit.create(AlarmApi::class.java)
    }
}
