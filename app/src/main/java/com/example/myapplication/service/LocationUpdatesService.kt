package com.example.myapplication.service

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.R
import com.example.myapplication.network.GeofenceAlertBody
import com.example.myapplication.network.LocationUpdateBody
import com.example.myapplication.network.RetrofitInstance
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LocationUpdatesService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var callback: LocationCallback
    private val channelId = "location_updates_channel"

    /** 에뮬레이터 과호출 방지용 쿨다운(지오펜스 전용) */
    private val geofenceCooldownMillis = 30_000L
    @Volatile private var lastGeofenceSentAt = 0L

    override fun onCreate() {
        super.onCreate()
        fusedClient = LocationServices.getFusedLocationProviderClient(this)

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
                result.lastLocation?.let { loc ->
                    Log.d("LocationSvc", "new location: ${loc.latitude},${loc.longitude}")

                    // 1) Firestore 갱신용: 백엔드에 현재 위치 업로드
                    sendLocationUpdate(loc)

                    // 2) 지오펜스 판정/푸시 트리거 (쿨다운 적용)
                    sendGeofenceAlert(loc)
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: android.content.Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationSvc", "onStartCommand() called")

        startForeground(1, createNotification())

        val hasFine = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            val req = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10_000L              // 요청 간격 10초
            ).setMinUpdateIntervalMillis(5_000L) // 최소 업데이트 5초
                .build()

            fusedClient.requestLocationUpdates(req, callback, mainLooper)
        } else {
            Log.e("LocationSvc", "No location permission. Stop service.")
            stopSelf()
        }

        return START_STICKY
    }

    override fun onDestroy() {
        fusedClient.removeLocationUpdates(callback)
        super.onDestroy()
    }

    override fun onBind(intent: android.content.Intent?): IBinder? = null

    private fun createNotification(): Notification =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("위치 전송 중")
            .setContentText("환자 위치 저장 & 지오펜스 감지")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

    /** (1) 현재 위치를 백엔드로 업로드 → 서버가 Firestore patients/{uid}.location 갱신 */
    private fun sendLocationUpdate(loc: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser ?: run {
                    Log.e("LocationSvc", "No Firebase user; skip location update")
                    return@launch
                }
                val res = RetrofitInstance.api.postLocationUpdate(
                    LocationUpdateBody(
                        latitude = loc.latitude,
                        longitude = loc.longitude
                    )
                )
                if (!res.isSuccessful) {
                    Log.e("LocationSvc", "location/update fail code=${res.code()} body=${res.errorBody()?.string()}")
                } else {
                    Log.d("LocationSvc", "location/update OK")
                }
            } catch (e: Exception) {
                Log.e("LocationSvc", "location/update exception", e)
            }
        }
    }

    /** (2) 지오펜스 이탈 판정 트리거 (쿨다운 적용) */
    private fun sendGeofenceAlert(loc: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = FirebaseAuth.getInstance().currentUser ?: run {
                    Log.e("LocationSvc", "No Firebase user; skip geofence alert")
                    return@launch
                }

                val now = System.currentTimeMillis()
                if (now - lastGeofenceSentAt < geofenceCooldownMillis) {
                    Log.d("LocationSvc", "geofence call throttled (cooldown)")
                    return@launch
                }
                lastGeofenceSentAt = now

                val res = RetrofitInstance.api.sendGeofenceAlert(
                    GeofenceAlertBody(
                        latitude = loc.latitude,
                        longitude = loc.longitude
                    )
                )
                if (!res.isSuccessful) {
                    Log.e("LocationSvc", "geofence-alert fail code=${res.code()} body=${res.errorBody()?.string()}")
                } else {
                    Log.d("LocationSvc", "geofence-alert OK")
                }
            } catch (e: Exception) {
                Log.e("LocationSvc", "geofence-alert exception", e)
            }
        }
    }
}

