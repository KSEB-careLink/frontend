// app/src/main/java/com/example/myapplication/network/SignupResponse.kt
package com.example.myapplication.network

data class SignupResponse(
    val uid: String,
    val joinCode: String? = null
)
