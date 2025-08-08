package com.example.myapplication.audio

import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.File
import java.io.RandomAccessFile

class AudioRecorder(private val context: Context) {
    private var audioRecord: AudioRecord? = null
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false

    private lateinit var wavFile: File
    private lateinit var raf: RandomAccessFile

    // 기본 파라미터
    private var sampleRate = 44100
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private val channels = 1
    private val bitsPerSample = 16

    /**
     * 녹음 시작
     * - 권한 체크
     * - AudioRecord 초기화 & 상태 확인
     * - 44바이트 WAV 헤더(임시) 기록 후 스트리밍 기록
     */
    fun startRecording(): String {
        if (isRecording) {
            Log.w("AudioRecorder", "startRecording() called while already recording — ignored")
            return if (this::wavFile.isInitialized) wavFile.absolutePath else ""
        }

        if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) throw SecurityException("RECORD_AUDIO 권한이 없습니다.")

        // 일부 기기에서 44.1k가 실패할 수 있어 48k로 폴백 시도
        var minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        if (minBuffer == AudioRecord.ERROR || minBuffer == AudioRecord.ERROR_BAD_VALUE) {
            sampleRate = 48000
            minBuffer = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        }
        require(minBuffer != AudioRecord.ERROR && minBuffer != AudioRecord.ERROR_BAD_VALUE) {
            "지원되지 않는 오디오 설정"
        }

        // 버퍼는 여유 있게 (x2) — 언더런 방지
        val bufferSize = minBuffer * 2

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            audioRecord = null
            throw IllegalStateException("AudioRecord 초기화 실패")
        }

        wavFile = File(context.cacheDir, "recording_${System.currentTimeMillis()}.wav")
        raf = RandomAccessFile(wavFile, "rw")
        // 임시 헤더(사이즈 0으로) 먼저 써두기
        writeWavHeader(raf, 0, sampleRate, channels, bitsPerSample)

        audioRecord?.startRecording()
        if (audioRecord?.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            audioRecord?.release()
            audioRecord = null
            raf.close()
            throw IllegalStateException("녹음 시작 실패")
        }

        isRecording = true

        recordingThread = Thread {
            var totalPcmBytes: Long = 0
            try {
                val buffer = ByteArray(bufferSize)
                while (isRecording) {
                    val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                    if (read > 0) {
                        raf.write(buffer, 0, read)
                        totalPcmBytes += read
                    } else if (read < 0) {
                        // 오류 코드: ERROR_INVALID_OPERATION / ERROR_BAD_VALUE 등
                        Log.w("AudioRecorder", "AudioRecord read() error: $read")
                    }
                }
            } catch (e: Exception) {
                Log.e("AudioRecorder", "Recording loop exception: ${e.message}", e)
            } finally {
                // 헤더 사이즈 갱신 + 파일 동기화/정리
                try {
                    updateWavSizes(raf, totalPcmBytes, sampleRate, channels, bitsPerSample)
                    try {
                        raf.fd.sync()
                    } catch (_: Exception) {}
                    raf.close()
                } catch (e: Exception) {
                    Log.e("AudioRecorder", "Finalizing WAV failed: ${e.message}", e)
                }
            }
        }.apply { start() }

        return wavFile.absolutePath
    }

    /**
     * 녹음 종료
     * - 루프 종료 플래그
     * - AudioRecord stop/release
     * - 스레드 join (헤더 갱신/파일 닫기까지 대기)
     */
    fun stopRecording(): String {
        if (!isRecording) {
            Log.w("AudioRecorder", "stopRecording() called while not recording — returning last path")
            return if (this::wavFile.isInitialized) wavFile.absolutePath else ""
        }

        isRecording = false

        audioRecord?.run {
            try { stop() } catch (_: RuntimeException) {}
            try { release() } catch (_: Exception) {}
        }
        audioRecord = null

        try {
            recordingThread?.join()
        } catch (_: InterruptedException) {}
        recordingThread = null

        return wavFile.absolutePath
    }

    /** WAV 헤더 쓰기 (임시 사이즈 0) */
    private fun writeWavHeader(
        raf: RandomAccessFile,
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val byteRate = sampleRate * channels * (bitsPerSample / 8)
        val totalDataLen = totalAudioLen + 36

        val header = ByteArray(44)
        // RIFF/WAVE
        header[0] = 'R'.code.toByte(); header[1] = 'I'.code.toByte(); header[2] = 'F'.code.toByte(); header[3] = 'F'.code.toByte()
        writeIntLE(header, 4, totalDataLen.toInt())
        header[8] = 'W'.code.toByte(); header[9] = 'A'.code.toByte(); header[10] = 'V'.code.toByte(); header[11] = 'E'.code.toByte()
        // fmt chunk
        header[12] = 'f'.code.toByte(); header[13] = 'm'.code.toByte(); header[14] = 't'.code.toByte(); header[15] = ' '.code.toByte()
        writeIntLE(header, 16, 16) // Subchunk1Size (PCM)
        writeShortLE(header, 20, 1) // AudioFormat (PCM)
        writeShortLE(header, 22, channels.toShort())
        writeIntLE(header, 24, sampleRate)
        writeIntLE(header, 28, byteRate)
        writeShortLE(header, 32, (channels * (bitsPerSample / 8)).toShort()) // BlockAlign
        writeShortLE(header, 34, bitsPerSample.toShort())
        // data chunk
        header[36] = 'd'.code.toByte(); header[37] = 'a'.code.toByte(); header[38] = 't'.code.toByte(); header[39] = 'a'.code.toByte()
        writeIntLE(header, 40, totalAudioLen.toInt())

        raf.seek(0)
        raf.write(header, 0, 44)
        raf.seek(44) // 오디오 데이터 시작
    }

    /** 녹음 종료 후 사이즈 필드 갱신 */
    private fun updateWavSizes(
        raf: RandomAccessFile,
        totalAudioLen: Long,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ) {
        val totalDataLen = totalAudioLen + 36
        // ChunkSize
        raf.seek(4); writeIntLE(raf, totalDataLen.toInt())
        // Subchunk2Size (data)
        raf.seek(40); writeIntLE(raf, totalAudioLen.toInt())
    }

    private fun writeIntLE(arr: ByteArray, offset: Int, value: Int) {
        arr[offset] = (value and 0xff).toByte()
        arr[offset + 1] = ((value shr 8) and 0xff).toByte()
        arr[offset + 2] = ((value shr 16) and 0xff).toByte()
        arr[offset + 3] = ((value shr 24) and 0xff).toByte()
    }

    private fun writeShortLE(arr: ByteArray, offset: Int, value: Short) {
        arr[offset] = (value.toInt() and 0xff).toByte()
        arr[offset + 1] = ((value.toInt() shr 8) and 0xff).toByte()
    }

    private fun writeIntLE(raf: RandomAccessFile, value: Int) {
        raf.write(byteArrayOf(
            (value and 0xff).toByte(),
            ((value shr 8) and 0xff).toByte(),
            ((value shr 16) and 0xff).toByte(),
            ((value shr 24) and 0xff).toByte()
        ))
    }
}



