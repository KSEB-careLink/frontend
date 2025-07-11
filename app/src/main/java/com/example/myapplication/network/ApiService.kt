// app/src/main/java/com/example/myapplication/network/ApiService.kt
package com.example.myapplication.network

import retrofit2.Response
import retrofit2.http.*

/** — 요청/응답 데이터 모델 — **/

// 1) 보호자 회원가입 요청/응답
data class GuardianSignupRequest(
    val email: String,
    val password: String,
    val name: String
)
data class GuardianSignupResponse(
    val uid: String,
    val joinCode: String
)

// 2) 보호자 추가 정보 등록 요청 (registerInfo)
data class RegisterInfoRequest(
    val name: String
)

// 3) 환자 연동 요청/응답 (linkPatient)
data class LinkPatientRequest(
    val joinCode: String,
    val patientName: String    // ← 기존 name → patientName 으로 변경
)
data class LinkPatientResponse(
    val message: String
)

// 4) 보호자의 환자 목록 조회 응답 (getPatients)
data class PatientInfo(
    val id: String,
    val name: String,
    val age: Int
)
data class PatientsListResponse(
    val patients: List<PatientInfo>
)

// 5) 환자가 볼 보호자 정보 조회 응답 (getGuardian)
data class GuardianInfoResponse(
    val guardianId: String,
    val guardianName: String
)

interface ApiService {

    /** 1) POST /signup-guardian **/
    @POST("/signup-guardian")
    suspend fun signupGuardian(
        @Body req: GuardianSignupRequest
    ): Response<GuardianSignupResponse>

    /** 2) POST /register-info **/
    @POST("/register-info")
    suspend fun registerInfo(
        @Body req: RegisterInfoRequest
    ): Response<GuardianSignupResponse>

    /** 3) POST /link-patient **/
    @POST("/link-patient")
    suspend fun linkPatient(
        @Body req: LinkPatientRequest
    ): Response<LinkPatientResponse>

    /** 4) GET /get-patients/{guardianId} **/
    @GET("/get-patients/{guardianId}")
    suspend fun getPatients(
        @Path("guardianId") guardianId: String
    ): Response<PatientsListResponse>

    /** 5) GET /get-guardian/{patientId} **/
    @GET("/get-guardian/{patientId}")
    suspend fun getGuardian(
        @Path("patientId") patientId: String
    ): Response<GuardianInfoResponse>
}

