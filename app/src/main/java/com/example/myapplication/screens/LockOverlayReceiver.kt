package com.example.myapplication.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class LockOverlayReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SCREEN_ON) {
            Log.d("LockOverlayReceiver", "SCREEN_ON -> show notification")
            LockOverlayNotificationHelper.showWakeNotification(context)
        }
    }
}











