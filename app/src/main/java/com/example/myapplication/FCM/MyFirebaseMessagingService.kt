package com.example.myapplication.fcm

import android.app.*
import android.content.*
import android.media.RingtoneManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import java.io.IOException
import okhttp3.RequestBody.Companion.toRequestBody
class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        Log.d("FCM", "🎯 새 FCM 토큰: $token")

        // 서버로 토큰 전송 (환자 전용)
        // 서버는 JS 코드 그대로 유지하므로, 반드시 /auth/save-token API가 있어야 함
        // 이 부분은 JS 수정 없이 사용자가 미리 구현해 둔 API 기준
        sendTokenToServer(token)
    }

    private fun sendTokenToServer(token: String) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        user.getIdToken(true).addOnSuccessListener { result ->
            val jwt = result.token ?: return@addOnSuccessListener

            val json = "{\"fcmToken\":\"$token\"}"
                .toRequestBody("application/json".toMediaTypeOrNull())

            val request = okhttp3.Request.Builder()
                .url("http://<서버주소>/auth/save-token")
                .post(json)
                .addHeader("Authorization", "Bearer $jwt")
                .build()

            OkHttpClient().newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    Log.e("FCM", "토큰 저장 실패: ${e.message}")
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    Log.i("FCM", "토큰 저장 성공")
                }
            })
        }
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val title = remoteMessage.notification?.title ?: "CareLink 알림"
        val body = remoteMessage.notification?.body ?: ""
        showNotification(title, body)
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "carelink_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Android 8.0 이상: 알림 채널 필요
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "CareLink 알림", NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification) // 꼭 아이콘 있어야 함
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        notificationManager.notify(0, builder.build())
    }
}

