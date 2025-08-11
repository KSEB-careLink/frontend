package com.example.myapplication.network

import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Path

data class AlarmRequest(val patientId: String, val message: String, val time: String)
data class EmergencyRequest(
    val guardianId: String,
    val message: String
)

data class GeofenceAlertBody(
    val latitude: Double,
    val longitude: Double
)
data class LatLngDto(
    val latitude: Double,
    val longitude: Double,
)

data class SafeZoneRequest(
    val center: LatLngDto,
    val radiusMeters: Int,
)

data class LocationUpdateBody(
    val latitude: Double,
    val longitude: Double
)

interface AlarmApi {
    @POST("/alarms")
    suspend fun postAlarm(
        @Body req: AlarmRequest,
     //   @Header("Authorization") auth: String   // 토큰 헤더 파라미터 추가 -> RetrofitInstance에서 자동으로 앞에붙이도록 수정해서 지움
    ): Response<Unit>

    @POST("/emergency")
    suspend fun sendEmergency(
        @Body req: EmergencyRequest
    ): Response<Unit>

    // Geofence 이탈 알림 전송
    @POST("/geofencealert")
    suspend fun sendGeofenceAlert(@Body body: GeofenceAlertBody): Response<Unit>

    @POST("patients/{patientId}/safezone")
    suspend fun setSafeZone(
        @Path("patientId") patientId: String,
        @Body body: SafeZoneRequest
    ): Response<Unit>

    @POST("/location/update")
    suspend fun postLocationUpdate(
        @Body body: LocationUpdateBody
    ): Response<Unit>

}