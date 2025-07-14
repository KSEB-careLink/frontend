// app/src/main/java/com/example/myapplication/network/SignupRequest.kt
package com.example.myapplication.network

data class SignupRequest(
    val email: String,
    val password: String,
    val name: String,
    val role: String
)


