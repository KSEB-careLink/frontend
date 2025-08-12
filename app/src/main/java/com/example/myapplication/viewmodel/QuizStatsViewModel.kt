// app/src/main/java/com/example/myapplication/viewmodel/QuizStatsViewModel.kt
package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class QuizStatsViewModel : ViewModel() {
    private val _patientId = MutableStateFlow<String?>(null)
    val patientId: StateFlow<String?> = _patientId

    fun setPatientId(id: String) {
        _patientId.value = id
    }
}