package com.example.myapplication.data

import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import android.util.Log

class DatasetRepository {
    // 절대 URL로 참조 생성
    private val storageRef = Firebase
        .storage
        .getReferenceFromUrl("gs://carelink-a228a.firebasestorage.app/memory/dataset.json")

    suspend fun fetchAll(): List<DatasetItem> = withContext(Dispatchers.IO) {
        // 내려받기 직전
        Log.d("DatasetRepo", "fetchAll() 호출 — URL: ${storageRef.path}")
        val bytes = storageRef.getBytes(5 * 1024 * 1024).await()
        Log.d("DatasetRepo", "바이트 크기: ${bytes.size}")
        val json = bytes.toString(Charsets.UTF_8)
        val list = Json { ignoreUnknownKeys = true }
            .decodeFromString<List<DatasetItem>>(json)
        Log.d("DatasetRepo", "파싱된 항목 개수: ${list.size}")
        return@withContext list
    }
}

