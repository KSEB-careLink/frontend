package com.example.myapplication.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EmotionRepository(private val dataStore: DataStore<Preferences>) {

    // 저장 키 - 날짜별 감정을 구분하려면 키를 동적으로 생성해야 함
    private fun emotionKey(date: String) = stringPreferencesKey("emotion_$date")

    // 감정 저장
    suspend fun saveEmotion(date: String, emotion: String) {
        dataStore.edit { prefs ->
            prefs[emotionKey(date)] = emotion
        }
    }

    // 모든 저장된 감정 기록 읽기 (Map<날짜, 감정>)
    val emotionRecordsFlow: Flow<Map<String, String>> = dataStore.data
        .map { prefs ->
            prefs.asMap().mapNotNull { entry ->
                val key = entry.key.name
                val value = entry.value as? String
                if (key.startsWith("emotion_") && value != null) {
                    key.removePrefix("emotion_") to value
                } else {
                    null
                }
            }.toMap()
        }

    // ✅ 감정 기록 전체 삭제
    suspend fun clearAllEmotions() {
        dataStore.edit { prefs ->
            // emotion_으로 시작하는 키만 삭제
            val keysToRemove = prefs.asMap().keys.filter { it.name.startsWith("emotion_") }
            for (key in keysToRemove) {
                prefs.remove(key)
            }
        }
    }
}





