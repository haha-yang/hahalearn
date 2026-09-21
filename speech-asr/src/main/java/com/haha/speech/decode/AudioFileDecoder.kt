package com.haha.speech.decode

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.haha.speech.pcm.PcmFormat
import com.haha.speech.pcm.PcmUtils
import com.haha.speech.pcm.WavPcmReader
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer

/**
 * 压缩音频（MP3/AAC/M4A）→ PCM float。WAV PCM 走 [WavPcmReader]，不必进解码器。
 */
object AudioFileDecoder {

    data class Result(
        val format: PcmFormat,
        val samples: FloatArray
    )

    fun decodeToAsrPcm(file: File, target: PcmFormat = PcmFormat.ASR_DEFAULT): Result {
        if (looksLikeWav(file)) {
            runCatching {
                val (wavFormat, bytes) = WavPcmReader.readAllPcm(file)
                val floats = PcmUtils.bytesToFloatMono(bytes, wavFormat)
                val resampled =
                    LinearResampler.resample(floats, wavFormat.sampleRate, target.sampleRate)
                return Result(target, resampled)
            }
        }
        return decodeCompressed(file, target)
    }

    private fun looksLikeWav(file: File): Boolean {
        return file.inputStream().use { input ->
            val head = ByteArray(12)
            val n = input.read(head)
            n == 12 &&
                    String(head, 0, 4, Charsets.US_ASCII) == "RIFF" &&
                    String(head, 8, 4, Charsets.US_ASCII) == "WAVE"
        }
    }

    private fun decodeCompressed(file: File, target: PcmFormat): Result {
        val extractor = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            extractor.setDataSource(file.absolutePath)
            val track = (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("audio/") == true
            } ?: error("文件里没有音轨: ${file.name}")
            val format = extractor.getTrackFormat(track)
            extractor.selectTrack(track)
            val mime = format.getString(MediaFormat.KEY_MIME)
                ?: error("音轨缺少 mime")
            val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            val channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            codec = MediaCodec.createDecoderByType(mime)
            codec.configure(format, null, null, 0)
            codec.start()

            val pcmBytes = ByteArrayOutputStream()
            val bufferInfo = MediaCodec.BufferInfo()
            var inputEos = false
            var outputEos = false
            while (!outputEos) {
                if (!inputEos) {
                    val inIndex = codec.dequeueInputBuffer(10_000)
                    if (inIndex >= 0) {
                        val inputBuffer =
                            codec.getInputBuffer(inIndex) ?: error("decoder input buffer 为空")
                        inputBuffer.clear()
                        val sampleSize = extractor.readSampleData(inputBuffer, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(
                                inIndex,
                                0,
                                0,
                                0,
                                MediaCodec.BUFFER_FLAG_END_OF_STREAM
                            )
                            inputEos = true
                        } else {
                            codec.queueInputBuffer(inIndex, 0, sampleSize, extractor.sampleTime, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIndex = codec.dequeueOutputBuffer(bufferInfo, 10_000)
                when {
                    outIndex == MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                    outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Unit
                    outIndex >= 0 -> {
                        val outBuf = codec.getOutputBuffer(outIndex)
                        if (outBuf != null && bufferInfo.size > 0) {
                            appendPcm(pcmBytes, outBuf, bufferInfo)
                        }
                        codec.releaseOutputBuffer(outIndex, false)
                        if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                            outputEos = true
                        }
                    }
                }
            }
            val srcFormat = PcmFormat(sampleRate = sampleRate, channelCount = channelCount)
            val bytes = pcmBytes.toByteArray()
            val floats = PcmUtils.bytesToFloatMono(bytes, srcFormat)
            val resampled =
                LinearResampler.resample(floats, srcFormat.sampleRate, target.sampleRate)
            return Result(target, resampled)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            extractor.release()
        }
    }

    private fun appendPcm(
        dest: ByteArrayOutputStream,
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo
    ) {
        val bytes = ByteArray(info.size)
        buffer.position(info.offset)
        buffer.limit(info.offset + info.size)
        buffer.get(bytes)
        dest.write(bytes)
    }
}
