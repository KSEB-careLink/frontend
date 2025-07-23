package com.example.myapplication.components

import android.content.Context
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

suspend fun FirebaseUser.refreshAndSaveToken(context: Context) : String {
    // 강제 재발급
    val newToken = this.getIdToken(true).await().token
        ?: throw Exception("ID 토큰이 null입니다")
    // SharedPreferences에 저장
    context.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        .edit()
        .putString("jwt_token", newToken)
        .apply()
    return newToken
}

fun Context.savePatientId(patientId: String) {
    this.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        .edit()
        .putString("patient_id", patientId)
        .apply()
}