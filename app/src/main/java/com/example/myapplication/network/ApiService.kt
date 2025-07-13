// app/src/main/java/com/example/myapplication/network/ApiService.kt
package com.example.myapplication.network

import retrofit2.Response
import retrofit2.http.*

data class SignupResponse(
    val uid: String,
    val joinCode: String? = null  // guardian인 경우에만 반환됨
)

data class PatientInfo(
    val id: String,
    val name: String,
    val age: Int
)

data class VerifyResponse(
    val role: String
)

interface ApiService {

    // 1) 회원가입: name, email, password, role 모두 전송
       @POST("auth/signup")
       suspend fun signup(
               @Body request: SignupRequest
       ): Response<SignupResponse>

    // 2) 토큰 검증
    @GET("auth/verify-token")
    suspend fun verifyToken(): Response<VerifyResponse>

    // 3) 보호된 환자 정보 조회
    @GET("protected/getPatient")
    suspend fun getPatient(): Response<PatientInfo>
}



