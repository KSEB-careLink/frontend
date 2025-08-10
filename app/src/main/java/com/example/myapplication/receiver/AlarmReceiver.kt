//package com.example.myapplication.receiver
//
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.PendingIntent
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.media.RingtoneManager
//import android.os.Build
//import androidx.core.app.NotificationCompat
//import com.example.myapplication.MainActivity // 🔁 알림 클릭 시 열고 싶은 액티비티
//import com.example.myapplication.R
//import android.util.Log // 상단에 추가
//
//class AlarmReceiver : BroadcastReceiver() {
//    override fun onReceive(context: Context, intent: Intent) {
//        Log.d("AlarmReceiver", "✅ onReceive 호출됨!")
//        val id = intent.getIntExtra("id", 0)
//        val content = intent.getStringExtra("content") ?: "일정 알림이에요!"
//
//        val channelId = "one_time_channel"
//        val mgr = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
//
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            mgr.createNotificationChannel(
//                NotificationChannel(
//                    channelId,
//                    "비정기 알림 채널",
//                    NotificationManager.IMPORTANCE_HIGH
//                ).apply {
//                    description = "GuardianAlarm2 스케줄 알림"
//                }
//            )
//        }
//
//        // ✅ 알림 누르면 앱의 MainActivity 실행
//        val notificationIntent = Intent(context, MainActivity::class.java).apply {
//            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//        }
//
//        val pendingIntent = PendingIntent.getActivity(
//            context,
//            id, // 요청 코드 (알림 ID 재사용 가능)
//            notificationIntent,
//            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
//        )
//
//        val sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
//        val notif = NotificationCompat.Builder(context, channelId)
//            .setSmallIcon(R.drawable.ic_notification)
//            .setContentTitle("📅 예정된 일정")
//            .setContentText(content)
//            .setAutoCancel(true)
//            .setSound(sound)
//            .setPriority(NotificationCompat.PRIORITY_HIGH)
//            .setContentIntent(pendingIntent) // ✅ 누르면 실행
//            .build()
//
//        mgr.notify(id, notif)
//    }
//}
//
