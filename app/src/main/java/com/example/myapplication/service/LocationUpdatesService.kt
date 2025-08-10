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
import com.example.myapplication.BuildConfig
import com.example.myapplication.R
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class LocationUpdatesService : Service() {

    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var callback: LocationCallback
    private val channelId = "location_updates_channel"

    /** --- (1) Authorization 자동첨부 + 401 자동갱신 설정된 OkHttpClient --- */
    // 요청마다 Authorization 헤더 붙이기 (캐시 토큰)
    private class AuthHeaderInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val user = FirebaseAuth.getInstance().currentUser
            val token = try {
                if (user != null) Tasks.await(user.getIdToken(false)).token else null
            } catch (_: Exception) { null }

            val authed = if (!token.isNullOrBlank()) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else original

            return chain.proceed(authed)
        }
    }

    // 401(id-token-expired)이면 토큰 강제 갱신하여 1회 재시도
    private class FirebaseAuthenticator : Authenticator {
        override fun authenticate(route: Route?, response: Response): Request? {
            // 무한 루프 방지: priorResponse가 있으면 이미 재시도한 것
            if (response.priorResponse != null) return null

            val user = FirebaseAuth.getInstance().currentUser ?: return null
            val newToken = try { Tasks.await(user.getIdToken(true)).token } catch (_: Exception) { null }
                ?: return null

            Log.d("FirebaseAuthenticator", "새로 발급받은 토큰: $newToken")

            return response.request.newBuilder()
                .header("Authorization", "Bearer $newToken")
                .build()
        }
    }

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(AuthHeaderInterceptor())
            .authenticator(FirebaseAuthenticator())
            .build()
    }
  /** ---------------------------------------------------------------------- */

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
                result.lastLocation?.let {
                    Log.d("LocationSvc", "new location: ${it.latitude},${it.longitude}")
                    sendLocation(it)
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
                10_000L
            ).setMinUpdateIntervalMillis(5_000L).build()

            fusedClient.requestLocationUpdates(req, callback, mainLooper)
        } else {
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
            .setContentText("10초마다 위치를 서버로 전송합니다")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .build()

    private fun sendLocation(loc: Location) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 로그인 안돼 있으면 스킵
                val user = FirebaseAuth.getInstance().currentUser
                if (user == null) {
                    Log.e("LocationSvc", "No Firebase user; skip update")
                    return@launch
                }

                val json = JSONObject().apply {
                    put("latitude", loc.latitude)
                    put("longitude", loc.longitude)
                }.toString()
                val body = json.toRequestBody("application/json".toMediaType())

                val req = Request.Builder()
                    .url("${BuildConfig.BASE_URL}/location/update")
                    .post(body)
                    .build()

                val resp = client.newCall(req).execute()
                Log.d("LocationSvc", "update HTTP code=${resp.code}")
                resp.close()
            } catch (e: Exception) {
                Log.e("LocationSvc", "update failed", e)
            }
        }
    }
}

