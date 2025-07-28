package com.example.myapplication.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.DatasetItem
import com.example.myapplication.data.DatasetRepository
import com.example.myapplication.BuildConfig
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class QuizViewModel(
    private val repo: DatasetRepository = DatasetRepository()
) : ViewModel() {

    private val _items = MutableStateFlow<List<DatasetItem>>(emptyList())
    val items: StateFlow<List<DatasetItem>> = _items

    private val client = OkHttpClient()
    private val baseUrl = BuildConfig.BASE_URL.trimEnd('/')

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

    /** Firebase Auth ID 토큰을 가져옵니다. */
    private suspend fun getIdToken(): String? {
        return Firebase.auth.currentUser
            ?.getIdToken(false)
            ?.await()
            ?.token
    }

    /** 1. 퀴즈 통계 조회 */
    fun fetchStats(patientId: String, onResult: (JSONObject?) -> Unit) {
        viewModelScope.launch {
            val url = "$baseUrl/quiz-stats?patientId=$patientId"
            val idToken = try { getIdToken() } catch (_: Exception) { null }
            Log.d("QuizVM", "GET $url token=$idToken")

            val builder = Request.Builder()
                .url(url)
                .get()
            idToken?.let { builder.addHeader("Authorization", "Bearer $it") }

            val request = builder.build()
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            Log.d("QuizVM", "Stats code=${response.code}")
            val body = response.body?.string()
            Log.d("QuizVM", "Stats body=$body")
            onResult(body?.let { JSONObject(it) })
        }
    }

    /** 2. 퀴즈 추천 조회 */
    fun fetchRecommend(patientId: String, onResult: (JSONObject?) -> Unit) {
        viewModelScope.launch {
            val url = "$baseUrl/quiz-stats/recommend?patientId=$patientId"
            val idToken = try { getIdToken() } catch (_: Exception) { null }
            Log.d("QuizVM", "GET $url token=$idToken")

            val builder = Request.Builder()
                .url(url)
                .get()
            idToken?.let { builder.addHeader("Authorization", "Bearer $it") }

            val request = builder.build()
            val response = withContext(Dispatchers.IO) {
                client.newCall(request).execute()
            }

            Log.d("QuizVM", "Recommend code=${response.code}")
            val body = response.body?.string()
            Log.d("QuizVM", "Recommend body=$body")
            onResult(body?.let { JSONObject(it) })
        }
    }
}

