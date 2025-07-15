// LocationScreen.kt
@file:OptIn(ExperimentalPermissionsApi::class)
@file:SuppressLint("MissingPermission")

package com.example.myapplication.screens

import android.Manifest
import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.myapplication.R
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.compose.ui.Alignment
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun LocationScreen(navController: NavController) {
    val context = LocalContext.current

    // 권한 상태
    val permissionsState = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    // 기본 위치 (서울시청)
    var currentLatLng by remember { mutableStateOf(LatLng(37.5665, 126.9780)) }

    // FusedLocationProviderClient
    val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    // 위치 업데이트
    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted) {
            fusedClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    currentLatLng = LatLng(it.latitude, it.longitude)
                }
            }
        } else {
            permissionsState.launchMultiplePermissionRequest()
        }
    }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
    }

    ConstraintLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        val (logo, title, map, button) = createRefs()

        // 1) 로고
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

        // 2) 텍스트
        Text(
            text = "위치 확인",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.constrainAs(title) {
                top.linkTo(logo.bottom, margin = 16.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        )

        // 3) 지도
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
                title = "내 위치"
            )
        }

        // 4) 버튼
        Button(
            onClick = {
                // 예: navController.navigate("NextScreen")
            },
            modifier = Modifier.constrainAs(button) {
                top.linkTo(map.bottom, margin = 24.dp)
                start.linkTo(parent.start)
                end.linkTo(parent.end)
            }
        ) {
            Text("범위 설정")
        }
    }
}





