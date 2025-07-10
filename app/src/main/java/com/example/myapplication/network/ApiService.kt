// app/src/main/java/com/example/myapplication/network/ApiService.kt
package com.example.myapplication.network

import retrofit2.Response
import retrofit2.http.*

// 기존 DTO
data class PatientInfo(
    val id: String,
    val name: String,
    val age: Int
)

// 토큰 검증 후 role을 받아올 DTO
data class VerifyResponse(
    val role: String
)

interface ApiService {
    @FormUrlEncoded
    @POST("/api/auth/signupGuardian")
    suspend fun signupGuardian(
        @Field("email") email: String,
        @Field("password") password: String
    ): Response<Void>

    @FormUrlEncoded
    @POST("/api/auth/signupPatient")
    suspend fun signupPatient(
        @Field("email") email: String,
        @Field("password") password: String
    ): Response<Void>

    @GET("/api/protected/getPatient")
    suspend fun getPatient(): Response<PatientInfo>

    // 로그인 성공 후 ID 토큰을 헤더에 담아 호출 → { "role": "guardian" } 등 반환
    @GET("/api/auth/verify-token")
    suspend fun verifyToken(): Response<VerifyResponse>
}

