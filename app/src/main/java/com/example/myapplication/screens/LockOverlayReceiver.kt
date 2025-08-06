package com.example.myapplication.screens

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class LockOverlayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_OFF) {
            // 화면이 꺼지는 (즉, 키가드 진입 직전) 순간에 딱 한 번 알림 발송
            LockOverlayNotificationHelper.showOverlayNotification(context)
        }
    }
}





