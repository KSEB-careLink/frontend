// app/src/main/java/com/example/myapplication/network/ApiService.kt
package com.example.myapplication.network

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class PatientInfo(
    val id: String,
    val name: String,
    val age: Int
)

data class VerifyResponse(
    val role: String
)

interface ApiService {
    // 1) 회원가입: 이메일/비번/이름/역할 전송 → uid, joinCode 반환
    @POST("auth/signup")
    suspend fun signup(
        @Body request: SignupRequest
    ): Response<SignupResponse>

    // 2) 토큰 검증 → 역할 반환
    @GET("auth/verify-token")
    suspend fun verifyToken(): Response<VerifyResponse>

    // 3) 보호된 환자 정보 조회
    @GET("protected/getPatient")
    suspend fun getPatient(): Response<PatientInfo>
}




