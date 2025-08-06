// app/src/main/java/com/example/myapplication/screens/LockOverlayActivity.kt
package com.example.myapplication.screens

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.NotificationManagerCompat
import com.example.myapplication.screens.LockOverlayNotificationHelper

class LockOverlayActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ★ 표시된 ID(1001)로 띄운 “풀스크린 인텐트” 알림을 바로 지웁니다.
        NotificationManagerCompat.from(this).cancel(1001)

        // (잠금화면 위에 띄우기 플래그 세팅)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(KeyguardManager::class.java))
                ?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Compose 오버레이 UI 표시
        setContent {
            LockOverlayScreen {
                // CareLink 앱 실행 예시
                val intent = packageManager.getLaunchIntentForPackage("com.example.carelink")
                if (intent != null) startActivity(intent)
                finish()
            }
        }
    }
}


