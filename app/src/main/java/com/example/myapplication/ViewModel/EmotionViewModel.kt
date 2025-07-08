package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.datastore.EmotionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EmotionViewModel(private val repository: EmotionRepository) : ViewModel() {

    val emotionRecords: StateFlow<Map<String, String>> = repository.emotionRecordsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyMap()
        )

    fun saveEmotion(date: String, emotion: String) {
        viewModelScope.launch {
            repository.saveEmotion(date, emotion)
        }
    }

    // ✅ 감정 기록 전체 삭제 함수
    fun clearAllEmotions() {
        viewModelScope.launch {
            repository.clearAllEmotions()
        }
    }
}

