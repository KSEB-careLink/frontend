package com.example.myapplication.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.example.myapplication.R

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // 새로운 FCM 토큰이 생성됐을 때 호출
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // TODO: 서버에 토큰 전송 (Push 발송 대상 관리용)
        sendRegistrationToServer(token)
    }

    // 백그라운드/포그라운드 상관없이 메시지 수신 시 호출
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 페이로드에서 title/body 가져오기
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"]
        val message = remoteMessage.notification?.body ?: remoteMessage.data["body"]

        if (title != null && message != null) {
            sendNotification(title, message)
        }
    }

    private fun sendRegistrationToServer(token: String) {
        // Retrofit, Ktor 등으로 내 서버에 토큰 전송
    }

    private fun sendNotification(title: String, message: String) {
        val channelId = "carelink_channel"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // O 이상은 채널 생성 필수
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "CareLink 알림",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "CareLink 앱 푸시 알림 채널"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // 앱에 맞는 아이콘
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)

        notificationManager.notify(
            System.currentTimeMillis().toInt(), // 고유 아이디
            notificationBuilder.build()
        )
    }
}
