package com.example.myapplication.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DatasetItem(
    @SerialName("question_id") val questionId: Int,
    val topic: String,
    val reminder: String,
    val question: String,
    val options: List<String>,
    val answer: Int
)

