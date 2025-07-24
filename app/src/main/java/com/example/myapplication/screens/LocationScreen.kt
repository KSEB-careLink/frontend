// LocationScreen.kt
@file:OptIn(ExperimentalPermissionsApi::class)
@file:SuppressLint("MissingPermission")

package com.example.myapplication.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.navigation.NavController
import com.example.myapplication.R
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

@Composable
fun LocationScreen(navController: NavController) {
    val context = LocalContext.current

    // 1) 위치 권한 상태
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    // 2) 연동된 환자 ID 읽기
    val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val patientId = prefs.getString("patient_id", null)
    if (patientId == null) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    // 3) 실시간 위치를 담을 상태
    var currentLatLng by remember { mutableStateOf(LatLng(37.5665, 126.9780)) }

    // 4) Firestore 리스너 등록
    DisposableEffect(patientId) {
        val registration: ListenerRegistration =
            FirebaseFirestore.getInstance()
                .collection("patients")
                .document(patientId)
                .addSnapshotListener { snap, _ ->
                    snap?.get("location")?.let { loc ->
                        (loc as? Map<*, *>)?.let { map ->
                            val lat = map["latitude"] as? Double
                            val lng = map["longitude"] as? Double
                            if (lat != null && lng != null) {
                                currentLatLng = LatLng(lat, lng)
                            }
                        }
                    }
                }
        onDispose { registration.remove() }
    }

    // 5) 카메라 상태
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val (logo, title, map, button) = createRefs()

        // 로고
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            modifier = Modifier
                .size(200.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                }
        )

        // 타이틀
        Text(
            text = "환자 위치 확인",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(logo.bottom, margin = 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        // 지도
        GoogleMap(
            modifier = Modifier
                .constrainAs(map) {
                    top.linkTo(title.bottom, margin = 16.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(300.dp),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = permissionsState.allPermissionsGranted
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        ) {
            Marker(
                state = MarkerState(currentLatLng),
                title = "환자 위치"
            )
        }

        // 버튼 (디자인 유지)
        Button(
            onClick = { /* 필요시 네비게이트하거나 추가 기능 */ },
            modifier = Modifier.constrainAs(button) {
                top.linkTo(map.bottom, margin = 24.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            },
            shape = MaterialTheme.shapes.medium
        ) {
            Text("뒤로 가기")
        }
    }
}







