package com.example.myapplication.network

/**
 * Firebase 가입 후,
 * Auth ID 토큰을 검증한 뒤 프로필(name, role)만 저장할 때 사용하는 바디 모델
 */
data class ProfileRequest(
    val name: String,
    val role: String      // "guardian" 또는 "patient"
)
