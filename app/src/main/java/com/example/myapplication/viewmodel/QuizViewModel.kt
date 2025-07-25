package com.example.myapplication.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DatasetItem
import com.example.myapplication.data.DatasetRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import android.util.Log

class QuizViewModel(
    private val repo: DatasetRepository = DatasetRepository()
) : ViewModel() {

    private val _items = MutableStateFlow<List<DatasetItem>>(emptyList())
    val items: StateFlow<List<DatasetItem>> = _items

    init {
        viewModelScope.launch {
            try {
                val fetched = repo.fetchAll()
                _items.value = fetched
                Log.d("QuizVM", "items 업데이트: size = ${fetched.size}")
            } catch (e: Exception) {
                Log.e("QuizVM", "fetchAll 오류", e)
            }
        }
    }
}
