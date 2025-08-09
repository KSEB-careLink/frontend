// app/src/main/java/com/example/myapplication/data/DatasetItem.kt
package com.example.myapplication.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DatasetItem(
    // recommend 응답: quiz_id
    @SerialName("quiz_id") val id: Int,
    @SerialName("question_text") val questionText: String,
    // 표준: options 배열, 백엔드가 option_1~4로 주더라도 리포지토리에서 배열로 변환
    val options: List<String>,
    @SerialName("tts_audio_url") val ttsAudioUrl: String = "",
    // 아래 필드는 있으면 사용, 없으면 기본값
    @SerialName("patient_id") val patientId: String? = null,
    @SerialName("memory_id") val memoryId: Int? = null,
    val category: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)




