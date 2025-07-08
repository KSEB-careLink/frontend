package com.example.myapplication.datastore

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.emotionDataStore by preferencesDataStore(name = "emotion_store")
