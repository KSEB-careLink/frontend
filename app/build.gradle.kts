plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    // 직렬화 플러그인
    kotlin("plugin.serialization") version "1.9.10"
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"https://backend-f61l.onrender.com\"")
    }
    
    buildFeatures {
        compose = true
        buildConfig = true  // ✅ 이 줄을 꼭 추가하세요!
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.6.7"
    }
}

dependencies {
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM 및 UI
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-extended:1.6.7")

    // Coil
    implementation(libs.coil.compose)

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // Compose Tooling
    implementation(libs.androidx.ui.tooling)

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")


    // Retrofit + OkHttp + Logging Interceptor
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation(libs.okhttp.logging.interceptor) // 정확한 이름으로 변경


    // AndroidView 유틸
    implementation("androidx.compose.ui:ui-util:1.6.7")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("com.patrykandpatrick.vico:compose:1.13.0")

    // Test Dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Firebase BoM: 공식 가이드 최신 버전 사용
    implementation(platform("com.google.firebase:firebase-bom:33.16.0"))

// Firebase Auth & 기타 제품은 BoM 덕분에 버전 없이 추가
    implementation("com.google.firebase:firebase-auth-ktx")

// (필요하다면) Analytics, Firestore 등 다른 제품도 버전 없이 추가
    implementation("com.google.firebase:firebase-analytics-ktx")

    // Firebase 알림
    implementation ("com.google.firebase:firebase-messaging:23.1.2")

    // Accompanist Permissions (Compose 런타임 권한 요청)
    implementation("com.google.accompanist:accompanist-permissions:0.37.3")

    // 위치 권한을 얻기 위한 FusedLocationProvider
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Compose Google Maps
    implementation("com.google.maps.android:maps-compose:2.11.3")
    implementation("com.google.android.gms:play-services-maps:18.1.0")

    // 백엔드 로그인 기능 연동을 위한 코드
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.1")

    //Compose용 ConstraintLayout을 사용
    implementation("androidx.constraintlayout:constraintlayout-compose:1.0.1")

    // WorkManager 코어 + 코틀린 확장
    implementation ("androidx.work:work-runtime-ktx:2.8.1")

    // LocalActivity 컴포지션
    implementation ("androidx.activity:activity-compose:1.10.0")


    // java.time API desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    // core 모듈 (버튼 API 포함)
    implementation("io.github.vanpra.compose-material-dialogs:core:0.9.0")
    // datetime 모듈 (timepicker)
    implementation("io.github.vanpra.compose-material-dialogs:datetime:0.9.0")

     // 파이어베이스 db 연동
    implementation("io.coil-kt:coil-compose:2.2.2")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")

    // Firebase Storage
    implementation("com.google.firebase:firebase-storage-ktx:20.4.2")
    // Coroutines + Play Services
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    //7월 28일 통계 보는데 추가
    
}



