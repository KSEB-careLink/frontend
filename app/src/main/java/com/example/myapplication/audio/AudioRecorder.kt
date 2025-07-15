// AudioRecorder.kt
package com.example.myapplication.audio

import android.content.Context
import android.media.MediaRecorder
import java.io.File
import java.io.IOException

class AudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var outputFile: String? = null

    fun startRecording(): String {
        // 출력 파일 경로: cacheDir/record_{timestamp}.3gp
        val fileName = "record_${System.currentTimeMillis()}.3gp"
        val file = File(context.cacheDir, fileName)
        outputFile = file.absolutePath

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(outputFile)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
            try {
                prepare()
                start()
            } catch (e: IOException) {
                throw RuntimeException("MediaRecorder prepare/start failed", e)
            }
        }
        return outputFile!!
    }

    fun stopRecording(): String? {
        recorder?.apply {
            try {
                stop()
            } catch (e: RuntimeException) {
                // stop 호출 시 에러 핸들링
            }
            release()
        }
        recorder = null
        return outputFile
    }
}
