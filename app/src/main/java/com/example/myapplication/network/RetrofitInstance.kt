package com.example.myapplication.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object RetrofitInstance {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://backend-f61l.onrender.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: AlarmApi by lazy {
        retrofit.create(AlarmApi::class.java)
    }
}


