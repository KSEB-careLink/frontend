package com.example.myapplication.data

data class DatasetItem(
    val id: Int,
    val questionText: String,
    val options: List<String>,
    val ttsAudioUrl: String,
    val imageUrl: String? = null
)





