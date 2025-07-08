package com.example.myapplication.util

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UploadApi {
    @Multipart
    @POST("upload") // 서버 엔드포인트에 맞게 수정
    suspend fun uploadImage(
        @Part image: MultipartBody.Part
    ): Response<Unit>
}
