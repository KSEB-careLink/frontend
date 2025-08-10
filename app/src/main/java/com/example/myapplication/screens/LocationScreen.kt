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

@OptIn(ExperimentalPermissionsApi::class)

@Composable
fun LocationScreen(navController: NavController, patientId: String) {
    val context = LocalContext.current

    // 1) 위치 권한 상태
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    // 최초에 권한 요청
    LaunchedEffect(Unit) {
        permissionsState.launchMultiplePermissionRequest()
    }

    // 권한이 없으면 안내 UI
    if (!permissionsState.allPermissionsGranted) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("위치 권한이 필요합니다", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            Button(onClick = { permissionsState.launchMultiplePermissionRequest() }) {
                Text("권한 요청")
            }
        }
        return
    }

    // 2) SharedPreferences에서 연동된 환자 ID 읽기
    val prefs = context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val patientId = prefs.getString("patient_id", null)
    if (patientId == null) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    // 3) 실시간 위치 상태
    var currentLatLng by remember { mutableStateOf(LatLng(37.5665, 126.9780)) }
    var isLoading by remember { mutableStateOf(true) }

    // 4) Firestore 스냅샷 리스너
    DisposableEffect(patientId) {
        val registration: ListenerRegistration =
            FirebaseFirestore.getInstance()
                .collection("patients")
                .document(patientId)
                .addSnapshotListener { snap, _ ->
                    snap?.get("location")?.let { loc ->
                        (loc as? Map<*, *>)?.let { map ->
                            val lat = (map["latitude"] as? Number)?.toDouble()
                            val lng = (map["longitude"] as? Number)?.toDouble()
                            if (lat != null && lng != null) {
                                currentLatLng = LatLng(lat, lng)
                                isLoading = false
                            }
                        }
                    }
                }
        onDispose { registration.remove() }
    }

    // 5) 카메라 상태
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
    }

    // 위치 변경 시 카메라 업데이트
    LaunchedEffect(currentLatLng) {
        cameraState.position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
    }

    // 6) UI 레이아웃
    ConstraintLayout(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val (logo, title, map, backBtn) = createRefs()

        // 로고
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            modifier = Modifier
                .size(200.dp)
                .constrainAs(logo) {
                    top.linkTo(parent.top, margin = 16.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                }
        )

        // 제목
        Text(
            text = "환자 위치 확인",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(logo.bottom, margin = 16.dp)
                start.linkTo(parent.start); end.linkTo(parent.end)
            }
        )

        // 지도 박스
        Box(
            Modifier
                .constrainAs(map) {
                    top.linkTo(title.bottom, margin = 16.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(300.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                GoogleMap(
                    cameraPositionState = cameraState,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                    modifier = Modifier.matchParentSize()
                ) {
                    Marker(state = MarkerState(currentLatLng), title = "환자 위치")
                }
            }
        }

        // 뒤로 가기 버튼
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.constrainAs(backBtn) {
                top.linkTo(map.bottom, margin = 24.dp)
                start.linkTo(parent.start); end.linkTo(parent.end)
            },
            shape = MaterialTheme.shapes.medium
        ) {
            Text("뒤로 가기")
        }
    }
}








