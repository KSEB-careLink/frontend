package com.example.myapplication.Lock

import android.app.Application
import android.content.Intent
import android.content.IntentFilter

class MyApplication : Application() {
    private val receiver = LockOverlayReceiver()

    override fun onCreate() {
        super.onCreate()

        // 화면 켜질 때만 알림 발송
        registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_ON))

        // 알림 채널 한 번만 생성
        LockOverlayNotificationHelper.createChannel(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        unregisterReceiver(receiver)
    }
}


