// LocationScreen.kt
@file:OptIn(ExperimentalPermissionsApi::class)
@file:SuppressLint("MissingPermission")    // ← 파일 단위로도 가능
package com.example.myapplication.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.R

// Accompanist Permissions
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

// Google Play Location & Maps
import com.google.android.gms.location.LocationServices
import com.google.maps.android.compose.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition

@SuppressLint("MissingPermission")  // ← 함수 선언부에 붙여 주세요
@Composable
fun LocationScreen(navController: NavController) {
    val context = LocalContext.current

    // 1) 권한 상태
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    // 2) 위치 초기값(서울시청)
    var currentLatLng by remember { mutableStateOf(LatLng(37.5665, 126.9780)) }

    // 3) FusedLocationProviderClient
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    // 4) 권한 승인 시 마지막 위치 가져오기, 미승인 시 권한 요청
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            // 인라인 @SuppressLint 제거!
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    currentLatLng = LatLng(it.latitude, it.longitude)
                }
            }
        } else {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    // 5) 카메라 상태
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        // 로고
        Image(
            painter = painterResource(id = R.drawable.rogo),
            contentDescription = "로고",
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = "위치 확인",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(Modifier.height(16.dp))

        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = permissionsState.allPermissionsGranted
            ),
            uiSettings = MapUiSettings(zoomControlsEnabled = false)
        ) {
            Marker(
                state = MarkerState(currentLatLng),
                title = "내 위치"
            )
        }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                // 예: navController.navigate("NextScreen")
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("범위 설정")
        }
    }
}




