package com.example.myapplication.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response

data class AlarmRequest(val patientId: String, val message: String, val time: String)

interface AlarmApi {
    @POST("/alarms")
    suspend fun postAlarm(@Body req: AlarmRequest): Response<Unit>
}
