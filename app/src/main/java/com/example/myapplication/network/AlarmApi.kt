package com.example.myapplication.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response
import retrofit2.http.Header

data class AlarmRequest(val patientId: String, val message: String, val time: String)
data class EmergencyRequest(
    val guardianId: String,
    val message: String
)


interface AlarmApi {
    @POST("/alarms")
    suspend fun postAlarm(
        @Body req: AlarmRequest,
     //   @Header("Authorization") auth: String   // ✅ 토큰 헤더 파라미터 추가 -> RetrofitInstance에서 자동으로 앞에붙이도록 수정해서 지움
    ): Response<Unit>

    @POST("/emergency")
    suspend fun sendEmergency(
        @Body req: EmergencyRequest
    ): Response<Unit>
}