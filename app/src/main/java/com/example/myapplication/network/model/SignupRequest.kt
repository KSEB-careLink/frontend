package com.example.myapplication.network

/**
 * POST /auth/signup 에 JSON 바디로 보낼 데이터 모델
 */
data class SignupRequest(
    val name: String,
    val email: String,
    val password: String,
    val role: String    // "guardian" or "patient"
)

