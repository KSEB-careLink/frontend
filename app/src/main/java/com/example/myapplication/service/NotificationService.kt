package com.example.myapplication.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import com.example.myapplication.BuildConfig
import com.example.myapplication.receiver.NotificationReceiver
import com.google.firebase.auth.FirebaseAuth
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

object NotificationService {

    /**
     * FCM 토큰을 백엔드에 저장/갱신.
     * Authorization: Bearer {Firebase ID Token}
     * Body: {"fcmToken":"..."}
     */
    fun sendFcmTokenToServer(context: Context, token: String) {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            Log.w("FCM", "로그인된 사용자 없음 → 토큰 전송 보류")
            return
        }

        user.getIdToken(true).addOnSuccessListener { res ->
            val jwt = res.token
            if (jwt.isNullOrBlank()) {
                Log.e("FCM", "ID 토큰이 비어있음 → 전송 불가")
                return@addOnSuccessListener
            }

            val client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build()

            val json = "{\"fcmToken\":\"$token\"}"
                .toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url("${BuildConfig.BASE_URL}/auth/save-token")
                .addHeader("Authorization", "Bearer $jwt")
                .post(json)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e("FCM", "백엔드 토큰 저장 실패(네트워크)", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    val body = response.body?.string()
                    Log.d("FCM", "백엔드 토큰 저장 응답: ${response.code} body=$body")
                    response.close()
                }
            })
        }.addOnFailureListener { e ->
            Log.e("FCM", "ID 토큰 획득 실패 → 전송 불가", e)
        }
    }

    fun scheduleNotification(
        context: Context,
        timeInMillis: Long,
        message: String,
        showToast: Boolean = false
    ) {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra("message", message)
            putExtra("id", timeInMillis.toInt())
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            timeInMillis.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    timeInMillis,
                    pendingIntent
                )
            } else {
                Log.e("Alarm", "정확한 알람을 예약할 권한이 없습니다.")
                val intentSettings = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intentSettings.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                context.startActivity(intentSettings)
            }
        } else {
            // 필요 시 단발성 정확 알람:
            // alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }

        if (showToast) {
            Toast.makeText(context, "알림 예약됨: $message", Toast.LENGTH_SHORT).show()
        }
    }
}
