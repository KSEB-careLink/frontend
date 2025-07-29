package com.example.myapplication.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.io.*

class AudioRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    private var isRecording = false
    private lateinit var wavFile: File
    private lateinit var pcmBuffer: ByteArrayOutputStream

    private val sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val channels = 1
    private val byteRate = sampleRate * 2  // 16bit mono = 2bytes per sample

    fun startRecording(): String {
        val permission = android.Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(context, permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            throw SecurityException("RECORD_AUDIO 권한이 없습니다.")
        }

        val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        wavFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.wav")
        pcmBuffer = ByteArrayOutputStream()

        audioRecord?.startRecording()
        isRecording = true

        recordingThread = Thread {
            val buffer = ByteArray(bufferSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                if (read > 0) {
                    pcmBuffer.write(buffer, 0, read)
                }
            }

            // 녹음 완료 후 WAV 파일 저장
            val audioData = pcmBuffer.toByteArray()
            FileOutputStream(wavFile).use { out ->
                writeWavHeader(out, audioData.size.toLong(), sampleRate, channels, byteRate)
                out.write(audioData)
            }

            pcmBuffer.close()
        }
        recordingThread?.start()

        return wavFile.absolutePath
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
        recordingThread?.join()
        recordingThread = null

        return wavFile.absolutePath
    }

    // WAV 헤더 생성 함수
    private fun writeWavHeader(
        out: FileOutputStream,
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        byteRate: Int
    ) {
        val totalDataLen = totalAudioLen + 36
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        header[4] = (totalDataLen and 0xff).toByte()
        header[5] = ((totalDataLen shr 8) and 0xff).toByte()
        header[6] = ((totalDataLen shr 16) and 0xff).toByte()
        header[7] = ((totalDataLen shr 24) and 0xff).toByte()
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        header[16] = 16
        header[17] = 0
        header[18] = 0
        header[19] = 0
        header[20] = 1
        header[21] = 0
        header[22] = channels.toByte()
        header[23] = 0
        header[24] = (sampleRate and 0xff).toByte()
        header[25] = ((sampleRate shr 8) and 0xff).toByte()
        header[26] = ((sampleRate shr 16) and 0xff).toByte()
        header[27] = ((sampleRate shr 24) and 0xff).toByte()
        header[28] = (byteRate and 0xff).toByte()
        header[29] = ((byteRate shr 8) and 0xff).toByte()
        header[30] = ((byteRate shr 16) and 0xff).toByte()
        header[31] = ((byteRate shr 24) and 0xff).toByte()
        header[32] = (2).toByte()  // block align
        header[33] = 0
        header[34] = 16  // bits per sample
        header[35] = 0
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        header[40] = (totalAudioLen and 0xff).toByte()
        header[41] = ((totalAudioLen shr 8) and 0xff).toByte()
        header[42] = ((totalAudioLen shr 16) and 0xff).toByte()
        header[43] = ((totalAudioLen shr 24) and 0xff).toByte()

        out.write(header, 0, 44)
    }
}

