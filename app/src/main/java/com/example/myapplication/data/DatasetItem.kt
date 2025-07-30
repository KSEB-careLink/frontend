package com.example.myapplication.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DatasetItem(
    @SerialName("id")          val questionId: Int,
    @SerialName("type")        val type:       String,
    @SerialName("question")    val question:   String,
    @SerialName("answer")      val answer:     String,
    @SerialName("memory_id")   val memoryId:   Int?    = null,
    @SerialName("photo_id")    val photoId:    Int?    = null,
    @SerialName("category")    val category:   String,
    @SerialName("created_at")  val createdAt:  String,
    @SerialName("options")     val options:    List<QuizOption>
)

@Serializable
data class QuizOption(
    @SerialName("id")          val id:         Int,
    @SerialName("quiz_id")     val quizId:     Int,
    @SerialName("option_text") val optionText: String,
    @SerialName("is_correct")  val isCorrect:  Boolean
)

