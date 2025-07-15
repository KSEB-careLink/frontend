package com.example.myapplication.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.myapplication.R

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // ScheduledActivity.kt 에서 넘긴 내용을 꺼내 옵니다
        val id      = intent.getIntExtra("id", 0)
        val content = intent.getStringExtra("content") ?: "일정 알림이에요!"

        // 알림 채널 ID
        val channelId = "one_time_channel"
        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Oreo 이상은 채널 생성 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            mgr.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "비정기 알림 채널",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "GuardianAlarm2 스케줄 알림"
                }
            )
        }

        // 실제 알림 빌드
        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notif = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification) // 알림 전용 흰색 실루엣 아이콘
            .setContentTitle("📅 예정된 일정")
            .setContentText(content)
            .setAutoCancel(true)
            .setSound(sound)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        mgr.notify(id, notif)
    }
}
