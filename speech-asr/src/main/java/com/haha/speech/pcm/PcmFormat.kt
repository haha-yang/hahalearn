package com.haha.speech.pcm

/**
 * PCM 布局约定。离线识别模型几乎都要求 16kHz / 单声道 / 16bit。
 */
data class PcmFormat(
    val sampleRate: Int,
    val channelCount: Int,
    val bitsPerSample: Int = 16
) {
    val bytesPerSample: Int get() = bitsPerSample / 8
    val bytesPerFrame: Int get() = bytesPerSample * channelCount
    val bytesPerSecond: Int get() = sampleRate * bytesPerFrame

    fun durationMs(byteCount: Int): Long {
        if (bytesPerSecond <= 0) return 0L
        return byteCount * 1000L / bytesPerSecond
    }

    companion object {
        val ASR_DEFAULT = PcmFormat(sampleRate = 16_000, channelCount = 1, bitsPerSample = 16)
    }
}
