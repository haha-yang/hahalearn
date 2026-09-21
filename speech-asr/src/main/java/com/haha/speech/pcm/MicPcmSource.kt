package com.haha.speech.pcm

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

/**
 * 麦克风 PCM 源。默认 16kHz 单声道 16bit，每 100ms 回调一块。
 */
class MicPcmSource(
    private val format: PcmFormat = PcmFormat.ASR_DEFAULT,
    private val chunkMs: Int = 100
) {
    @Volatile
    private var running = false
    private var recordThread: Thread? = null
    private var recorder: AudioRecord? = null

    val pcmFormat: PcmFormat get() = format

    @SuppressLint("MissingPermission")
    fun start(onPcm: (ByteArray) -> Unit) {
        stop()
        val channelConfig = if (format.channelCount == 1) {
            AudioFormat.CHANNEL_IN_MONO
        } else {
            AudioFormat.CHANNEL_IN_STEREO
        }
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(format.sampleRate, channelConfig, encoding)
        val chunkBytes =
            (format.bytesPerSecond * chunkMs / 1000).coerceAtLeast(format.bytesPerFrame)
        val bufferSize = minBuf.coerceAtLeast(chunkBytes * 2)
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            format.sampleRate,
            channelConfig,
            encoding,
            bufferSize
        )
        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord.release()
            error("AudioRecord 初始化失败，检查 RECORD_AUDIO 与采样参数")
        }
        recorder = audioRecord
        running = true
        recordThread = Thread({
            val buf = ByteArray(chunkBytes)
            try {
                audioRecord.startRecording()
                while (running) {
                    val n = audioRecord.read(buf, 0, buf.size)
                    if (n > 0) {
                        onPcm(if (n == buf.size) buf.copyOf() else buf.copyOf(n))
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "mic read failed", t)
            } finally {
                try {
                    audioRecord.stop()
                } catch (_: Exception) {
                }
                audioRecord.release()
                if (recorder === audioRecord) {
                    recorder = null
                }
            }
        }, "speech-asr-mic").also { it.start() }
    }

    fun stop() {
        running = false
        recordThread?.join(800)
        recordThread = null
        recorder?.let {
            try {
                it.stop()
            } catch (_: Exception) {
            }
            it.release()
        }
        recorder = null
    }

    companion object {
        private const val TAG = "MicPcmSource"
    }
}
