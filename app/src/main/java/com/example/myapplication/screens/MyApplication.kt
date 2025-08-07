package com.example.myapplication.screens

import android.app.Application
import android.content.IntentFilter
import android.content.Intent
import com.example.myapplication.screens.LockOverlayReceiver

class MyApplication : Application() {
    private val receiver = LockOverlayReceiver()

    override fun onCreate() {
        super.onCreate()
        // 화면 켜짐·꺼짐 이벤트 계속 수신
        registerReceiver(receiver, IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        })
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(receiver)
    }
}

