// 1. ReminderWorker.kt
package com.example.myapplication.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.util.*

class ReminderWorker(
    private val appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        // Worker에 전달된 파라미터로 시작/종료 시각 읽기
        val startHour = inputData.getInt("startHour", 9)
        val endHour   = inputData.getInt("endHour", 20)

        val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (nowHour in startHour..endHour) {
            sendLocalNotification(
                title = "⏰ 시간이에요!",
                body  = "${String.format("%02d시", nowHour)}에 알림이 도착했습니다."
            )
        }
        return Result.success()
    }

    private fun sendLocalNotification(title: String, body: String) {
        val channelId = "hourly_local"
        val mgr = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Oreo+ 이상은 채널을 미리 생성
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "시간별 알림",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "매시간 정각 알림 채널" }
            mgr.createNotificationChannel(channel)
        }

        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notif = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(com.example.myapplication.R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setSound(sound)
            .setAutoCancel(true)
            .build()

        mgr.notify(System.currentTimeMillis().toInt(), notif)
    }
}
