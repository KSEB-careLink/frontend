// app/src/main/java/com/example/myapplication/viewmodel/QuizViewModel.kt
package com.example.myapplication.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DatasetItem
import com.example.myapplication.data.DatasetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class QuizViewModel(
    private val repo: DatasetRepository = DatasetRepository()
) : ViewModel() {
    companion object { private const val TAG = "QuizViewModel" }

    private val _items = MutableStateFlow<List<DatasetItem>>(emptyList())
    val items = _items.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    // 기존 코드와 호환되게 기본값 제공
    fun loadQuizzes(
        patientId: String,
        photoId: String? = null,
        imageUrl: String? = null,
        imagePath: String? = null,
        description: String? = null
    ) {
        viewModelScope.launch {
            try {
                val list = repo.fetchAll(
                    patientId = patientId,
                    photoId = photoId,
                    imageUrl = imageUrl,
                    imagePath = imagePath,
                    description = description
                )
                _items.value = list
            } catch (e: Exception) {
                Log.e(TAG, "퀴즈 로드 실패", e)
                _error.value = e.message
            }
        }
    }
}




