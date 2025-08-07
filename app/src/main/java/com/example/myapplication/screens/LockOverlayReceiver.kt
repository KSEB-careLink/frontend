package com.example.myapplication.screens

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

class LockOverlayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("LockOverlayReceiver", "Received: ${intent.action}")
        if (intent.action == Intent.ACTION_SCREEN_ON) {
            val km = context.getSystemService(KeyguardManager::class.java)
            if (km?.isKeyguardLocked == true) {
                Log.d("LockOverlayReceiver", "Keyguard locked → scheduling full-screen notification")
                Handler(Looper.getMainLooper()).postDelayed({
                    LockOverlayNotificationHelper.showOverlayNotification(context)
                }, 500)  // 0.5초 정도 딜레이
            }
        }
    }
}










