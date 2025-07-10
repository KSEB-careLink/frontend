// app/src/main/java/com/example/myapplication/network/ApiService.kt
package com.example.myapplication.network

import retrofit2.Response
import retrofit2.http.*

data class PatientInfo(val id: String, val name: String, val age: Int)

interface ApiService {
    @FormUrlEncoded
    @POST("/api/auth/signupGuardian")
    suspend fun signupGuardian(
        @Field("email") email: String,
        @Field("password") password: String
    ): Response<Void>

    @GET("/api/protected/getPatient")
    suspend fun getPatient(): Response<PatientInfo>
}
