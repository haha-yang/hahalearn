package com.haha.speech.decode

/**
 * 线性重采样。文件解码后若不是 16kHz，先拉到识别采样率再送引擎。
 */
object LinearResampler {

    fun resample(input: FloatArray, srcRate: Int, dstRate: Int): FloatArray {
        if (srcRate <= 0 || dstRate <= 0) return input
        if (srcRate == dstRate || input.isEmpty()) return input
        val outLen = (input.size.toLong() * dstRate / srcRate).toInt().coerceAtLeast(1)
        val out = FloatArray(outLen)
        val ratio = srcRate.toDouble() / dstRate
        val last = input.lastIndex
        for (i in 0 until outLen) {
            val srcPos = i * ratio
            val idx = srcPos.toInt().coerceAtMost(last)
            val frac = (srcPos - idx).toFloat()
            val a = input[idx]
            val b = input[(idx + 1).coerceAtMost(last)]
            out[i] = a + (b - a) * frac
        }
        return out
    }
}
