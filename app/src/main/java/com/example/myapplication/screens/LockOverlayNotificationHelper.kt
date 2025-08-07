package com.example.myapplication.screens

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.R

object LockOverlayNotificationHelper {
    private const val CHANNEL_ID = "overlay_channel"
    private const val NOTIFY_ID = 1001
    private const val TAG = "LockOverlayNotif"

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                CHANNEL_ID,
                "Lock Overlay",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Show lock-screen overlay"
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            context.getSystemService(NotificationManager::class.java)
                .createNotificationChannel(chan)
        }
    }

    fun showOverlayNotification(context: Context) {
        Log.d(TAG, "→ showOverlayNotification() called")

        // Android 13+ 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w(TAG, "Notification permission not granted")
            return
        }
        Log.d(TAG, "Notification permission granted")

        // ① 풀스크린 인텐트용 PendingIntent
        val fullScreenIntent = Intent(context, LockOverlayActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            0,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        Log.d(TAG, "Created fullScreenPendingIntent")

        // ② Notification 빌드 (IMPORTANCE_HIGH + CATEGORY_ALARM + setFullScreenIntent)
        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.rogo)
            .setContentTitle("오늘도 함께")
            .setContentText("소중한 하루를 채워보세요!")
            // ① 채널 우선순위(채널 생성 시 이미 HIGH 이지만, 호환성 차원에서 MAX 로)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            // ② 카테고리는 CALL 로 (ALARM 보다 더 강제 풀스크린에 잘 먹힘)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            // ③ 잠금화면 노출 모드
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            // ④ 풀스크린 인텐트
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .build()
        Log.d(TAG, "Built notification object")

        // ③ 알림 발송
        try {
            NotificationManagerCompat.from(context).notify(NOTIFY_ID, notif)
            Log.d(TAG, "Notification (ID=$NOTIFY_ID) sent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send full-screen notification", e)
        }
    }
}



