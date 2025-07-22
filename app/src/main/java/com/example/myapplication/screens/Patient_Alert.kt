package com.example.myapplication.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.R

@Composable
fun Patient_Alert(navController: NavController) {
    // 조절용 값

    val textTopGap    = 24.dp
    val circleSize    = 260.dp
    val titleGap      = 16.dp

    // 현재 route
    val navBackStack by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStack?.destination?.route

    Scaffold(
        bottomBar = {
            val navColors = NavigationBarItemDefaults.colors(
                indicatorColor      = Color.Transparent,
                selectedIconColor   = Color(0xFF00C4B4),
                unselectedIconColor = Color(0xFF888888),
                selectedTextColor   = Color(0xFF00C4B4),
                unselectedTextColor = Color(0xFF888888)
            )
            NavigationBar {
                listOf(
                    "sentence" to "회상문장",
                    "quiz"     to "회상퀴즈",
                    "alert"    to "긴급 알림"
                ).forEach { (route, label) ->
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Star, contentDescription = label) },
                        label = { Text(label) },
                        selected = currentRoute == route,
                        onClick = {
                            if (currentRoute != route) {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.startDestinationId)
                                    launchSingleTop = true
                                }
                            }
                        },
                        colors = navColors
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(modifier = Modifier.height(210.dp))

            // 2) 안내 문구
            Spacer(Modifier.height(textTopGap))
            Text(
                buildAnnotatedString {
                    append("불편함이나 ")
                    withStyle(SpanStyle(color = Color(0xFFE2101A))) {
                        append("위험")
                    }
                    append("을 느끼시나요?")
                },
                fontSize = 25.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color(0xFF00C4B4))) {
                        append("보호자")
                    }
                    append("에게 연락할게요")
                },
                fontSize = 25.sp
            )

            // 3) 타이틀
            Spacer(Modifier.height(titleGap))
            Text(
                text = "긴급 알림",
                fontSize = 39.sp,
                color = Color(0xFFE2101A),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            // 4) HELP 아이콘 원형으로
            Spacer(Modifier.height(titleGap))
            Box(
                modifier = Modifier
                    .size(circleSize)
                    .background(color = Color(0xFFBF0310), shape = CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.help_icon),
                    contentDescription = "HELP",
                    modifier = Modifier.size(140.dp),
                    contentScale = ContentScale.Fit
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewPatient_Alert() {
    Patient_Alert(navController = rememberNavController())
}

