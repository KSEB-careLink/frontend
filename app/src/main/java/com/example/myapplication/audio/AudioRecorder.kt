package com.example.myapplication.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class AudioRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false
    private lateinit var pcmFile: File

    private val sampleRate = 44100 // CD 품질
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    fun startRecording(): String {
        // 1. 권한 확인
        val permission = android.Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(context, permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("RECORD_AUDIO 권한이 없습니다.")
        }

        // 2. AudioRecord 초기화
        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        // 3. 저장할 PCM 파일 경로 생성
        pcmFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.pcm")
        val outputStream = FileOutputStream(pcmFile)

        // 4. 녹음 시작
        audioRecord?.startRecording()
        isRecording = true

        recordingThread = Thread {
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    outputStream.write(buffer, 0, read)
                }
            }
            try {
                outputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        recordingThread?.start()

        return pcmFile.absolutePath
    }

    fun stopRecording(): String? {
        isRecording = false

        audioRecord?.apply {
            try {
                stop()
            } catch (e: RuntimeException) {
                e.printStackTrace()
            }
            release()
        }
        audioRecord = null
        recordingThread = null

        return pcmFile.absolutePath
    }
}

