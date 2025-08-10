package com.example.myapplication.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DatasetItem(
    val id: Int,
    @SerialName("patient_id") val patientId: String,
    @SerialName("memory_id") val memoryId: Int,
    @SerialName("question_text") val questionText: String,
    @SerialName("option_1") val option1: String,
    @SerialName("option_2") val option2: String,
    @SerialName("option_3") val option3: String,
    @SerialName("option_4") val option4: String,
    @SerialName("answer_index") val answerIndex: Int,
    @SerialName("tts_audio_url") val ttsAudioUrl: String,
    val category: String,
    @SerialName("created_at") val createdAt: String
) {
    val options get() = listOf(option1, option2, option3, option4)
}



