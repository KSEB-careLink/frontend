package com.example.myapplication.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.example.myapplication.R
import com.example.myapplication.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import okhttp3.MediaType.Companion.toMediaType

class LocationUpdatesService : Service() {
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var callback: LocationCallback
    private val client = OkHttpClient()
    private val channelId = "location_updates_channel"

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        // 알림 채널 생성 (Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(
                NotificationChannel(
                    channelId,
                    "위치 전송 서비스",
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }

        callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { sendLocation(it) }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1) Foreground 서비스로 시작
        startForeground(1, createNotification())

        // 2) 권한 체크 후 위치 업데이트 요청
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            val req = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10_000L
            ).setMinUpdateIntervalMillis(5_000L).build()

            fusedClient.requestLocationUpdates(req, callback, mainLooper)
        } else {
            // 권한이 없으면 서비스 자동 중지
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(callback)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("위치 전송 중")
            .setContentText("10초마다 위치를 서버로 전송합니다")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()
    }

    private fun sendLocation(loc: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            val token = getSharedPreferences("MyPrefs", MODE_PRIVATE)
                .getString("jwt_token", null) ?: return@launch

            val json = JSONObject().apply {
                put("latitude", loc.latitude)
                put("longitude", loc.longitude)
            }.toString()

            val body = json.toRequestBody("application/json".toMediaType())
            val req = Request.Builder()
                .url("${BuildConfig.BASE_URL}/update")
                .addHeader("Authorization", "Bearer $token")
                .post(body)
                .build()

            client.newCall(req).execute().close()
        }
    }
}
