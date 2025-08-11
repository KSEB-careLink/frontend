// safeZone 입력받는 로직 넣은 버전
@file:OptIn(ExperimentalPermissionsApi::class)
@file:SuppressLint("MissingPermission")

package com.example.myapplication.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
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
import com.example.myapplication.network.LatLngDto
import com.example.myapplication.network.RetrofitInstance
import com.example.myapplication.network.SafeZoneRequest
import com.google.accompanist.permissions.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocationScreen(navController: NavController, patientId: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 1) 위치 권한
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )
    LaunchedEffect(Unit) { permissionsState.launchMultiplePermissionRequest() }

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

    // 2) 연결된 환자 ID (넘겨받은 파라미터 우선, 없으면 prefs 사용)
    val realPatientId = remember(patientId) {
        if (patientId.isNotBlank()) patientId
        else context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
            .getString("patient_id", null) ?: ""
    }
    if (realPatientId.isBlank()) {
        LaunchedEffect(Unit) { navController.popBackStack() }
        return
    }

    // 3) 실시간 환자 위치
    var currentLatLng by remember { mutableStateOf(LatLng(37.5665, 126.9780)) }
    var isLoading by remember { mutableStateOf(true) }

    DisposableEffect(realPatientId) {
        val reg: ListenerRegistration =
            FirebaseFirestore.getInstance()
                .collection("patients")
                .document(realPatientId)
                .addSnapshotListener { snap, _ ->
                    snap?.get("location")?.let { loc ->
                        (loc as? Map<*, *>)?.let { m ->
                            val lat = (m["latitude"] as? Number)?.toDouble()
                            val lng = (m["longitude"] as? Number)?.toDouble()
                            if (lat != null && lng != null) {
                                currentLatLng = LatLng(lat, lng)
                                isLoading = false
                            }
                        }
                    }
                }
        onDispose { reg.remove() }
    }

    // 4) 카메라
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
    }
    LaunchedEffect(currentLatLng) {
        cameraState.position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
    }

    // 5) 안전구역 편집 상태
    var center by remember { mutableStateOf<LatLng?>(null) }      // 중심(롱탭으로 설정)
    var radius by remember { mutableStateOf(200f) }                // 기본 200m
    var saving by remember { mutableStateOf(false) }

    ConstraintLayout(
        Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val (logo, title, map, controls, backBtn) = createRefs()

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

        Text(
            text = "환자 위치 / 안전구역 설정",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(logo.bottom, margin = 16.dp)
                start.linkTo(parent.start); end.linkTo(parent.end)
            }
        )

        // 지도
        Box(
            Modifier
                .constrainAs(map) {
                    top.linkTo(title.bottom, margin = 16.dp)
                    start.linkTo(parent.start); end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }
                .height(360.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                GoogleMap(
                    cameraPositionState = cameraState,
                    properties = MapProperties(isMyLocationEnabled = true),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false),
                    modifier = Modifier.matchParentSize(),
                    onMapLongClick = { latLng -> center = latLng } // 🔹 롱탭으로 중심 설정
                ) {
                    // 환자 현재 위치
                    Marker(state = MarkerState(currentLatLng), title = "환자 위치")

                    // 선택한 안전구역 미리보기
                    center?.let { c ->
                        Marker(
                            state = MarkerState(c),
                            title = "안전구역 중심",
                            //draggable = true,
                            //onDragEnd = { center = it }
                        )
                        Circle(
                            center = c,
                            radius = radius.toDouble(),           // meters
                            strokeColor = androidx.compose.ui.graphics.Color(0x9900C4B4),
                            strokeWidth = 2f,
                            fillColor = androidx.compose.ui.graphics.Color(0x3300C4B4)
                        )
                    }
                }
            }
        }

        // 반경 + 저장
        Column(
            Modifier.constrainAs(controls) {
                top.linkTo(map.bottom, margin = 16.dp)
                start.linkTo(parent.start); end.linkTo(parent.end)
                width = Dimension.fillToConstraints
            }
        ) {
            Text("반경: ${radius.toInt()} m")
            Slider(
                value = radius,
                onValueChange = { radius = it },
                valueRange = 50f..1000f,
                steps = 19
            )
            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally // 가로 중앙
            ) {
                Button(
                    enabled = (center != null && !saving),
                    onClick = {
                        if (center == null) {
                            Toast.makeText(context, "지도를 길게 눌러 중심을 선택하세요.", Toast.LENGTH_SHORT)
                                .show()
                            return@Button
                        }
                        saving = true
                        scope.launch(Dispatchers.IO) {
                            try {
                                val res = RetrofitInstance.api.setSafeZone(
                                    realPatientId,
                                    SafeZoneRequest(
                                        center = LatLngDto(center!!.latitude, center!!.longitude),
                                        radiusMeters = radius.toInt()
                                    )
                                )
                                if (res.isSuccessful) {
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "안전구역이 저장되었습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    val body = res.errorBody()?.string()
                                    launch(Dispatchers.Main) {
                                        Toast.makeText(
                                            context,
                                            "저장 실패(${res.code()})",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    android.util.Log.e(
                                        "SafeZone",
                                        "fail code=${res.code()} body=$body"
                                    )
                                }
                            } catch (e: Exception) {
                                launch(Dispatchers.Main) {
                                    Toast.makeText(context, "오류: ${e.message}", Toast.LENGTH_SHORT)
                                        .show()
                                }
                                android.util.Log.e("SafeZone", "exception", e)
                            } finally {
                                saving = false
                            }
                        }
                    }
                ) {
                    Text(if (saving) "저장 중..." else "이 위치를 안전구역으로 저장")
                }
            }
        }
        Button(
            onClick = { navController.popBackStack() },
            modifier = Modifier.constrainAs(backBtn) {
                top.linkTo(controls.bottom, margin = 16.dp)
                start.linkTo(parent.start); end.linkTo(parent.end)
            }
        ) { Text("뒤로 가기") }
    }
}
