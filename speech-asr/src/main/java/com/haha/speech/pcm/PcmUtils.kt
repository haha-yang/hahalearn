package com.haha.speech.pcm

/**
 * 16bit 小端 PCM ↔ float[-1,1]。识别引擎吃 float，AudioRecord 吐 byte。
 */
object PcmUtils {

    fun bytesToFloatMono(
        bytes: ByteArray,
        format: PcmFormat,
        length: Int = bytes.size
    ): FloatArray {
        require(format.bitsPerSample == 16) { "仅支持 16bit PCM，当前 bits=${format.bitsPerSample}" }
        val frameCount = length / format.bytesPerFrame
        val out = FloatArray(frameCount)
        var offset = 0
        for (i in 0 until frameCount) {
            if (format.channelCount == 1) {
                out[i] = le16ToFloat(bytes, offset)
                offset += 2
            } else {
                var acc = 0f
                for (c in 0 until format.channelCount) {
                    acc += le16ToFloat(bytes, offset)
                    offset += 2
                }
                out[i] = acc / format.channelCount
            }
        }
        return out
    }

    fun le16ToFloat(bytes: ByteArray, offset: Int): Float {
        val sample =
            ((bytes[offset].toInt() and 0xFF) or (bytes[offset + 1].toInt() shl 8)).toShort()
        return sample / 32768f
    }

    fun rms(samples: FloatArray): Float {
        if (samples.isEmpty()) return 0f
        var sum = 0.0
        for (s in samples) {
            sum += s * s
        }
        return kotlin.math.sqrt(sum / samples.size).toFloat()
    }
}
