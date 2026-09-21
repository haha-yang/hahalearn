package com.haha.speech.pcm

import java.io.EOFException
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 解析 PCM WAV 头（RIFF/WAVE）。压缩格式（fmt ≠ 1）直接拒绝，走 [com.haha.speech.decode.AudioFileDecoder]。
 */
data class WavHeader(
    val format: PcmFormat,
    val dataOffset: Int,
    val dataSize: Int
)

object WavPcmReader {

    fun readHeader(file: File): WavHeader = file.inputStream().use { readHeader(it) }

    fun readHeader(input: InputStream): WavHeader {
        val riff = readFully(input, 12)
        require(ascii(riff, 0, 4) == "RIFF") { "不是 RIFF 文件" }
        require(ascii(riff, 8, 4) == "WAVE") { "不是 WAVE 文件" }

        var format: PcmFormat? = null
        var dataSize = -1
        var consumed = 12
        while (true) {
            val chunkHead = readFully(input, 8)
            consumed += 8
            val id = ascii(chunkHead, 0, 4)
            val size = le32(chunkHead, 4)
            when (id) {
                "fmt " -> {
                    val fmt = readFully(input, size)
                    consumed += size
                    val audioFormat = le16(fmt, 0)
                    require(audioFormat == 1) { "WAV 压缩类型=$audioFormat，请用 MediaCodec 解码" }
                    format = PcmFormat(
                        sampleRate = le32(fmt, 4),
                        channelCount = le16(fmt, 2),
                        bitsPerSample = le16(fmt, 14)
                    )
                    if (size % 2 == 1) {
                        input.read()
                        consumed += 1
                    }
                }

                "data" -> {
                    dataSize = size
                    return WavHeader(
                        format = format ?: error("data 块出现在 fmt 之前"),
                        dataOffset = consumed,
                        dataSize = dataSize
                    )
                }

                else -> {
                    skipFully(input, size.toLong())
                    consumed += size
                    if (size % 2 == 1) {
                        input.read()
                        consumed += 1
                    }
                }
            }
        }
    }

    fun readAllPcm(file: File): Pair<PcmFormat, ByteArray> {
        file.inputStream().use { input ->
            val header = readHeader(input)
            val pcm = readFully(input, header.dataSize)
            return header.format to pcm
        }
    }

    private fun ascii(bytes: ByteArray, offset: Int, length: Int): String =
        String(bytes, offset, length, Charsets.US_ASCII)

    private fun le16(bytes: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(bytes, offset, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt() and 0xFFFF

    private fun le32(bytes: ByteArray, offset: Int): Int =
        ByteBuffer.wrap(bytes, offset, 4).order(ByteOrder.LITTLE_ENDIAN).int

    private fun readFully(input: InputStream, size: Int): ByteArray {
        val buf = ByteArray(size)
        var offset = 0
        while (offset < size) {
            val n = input.read(buf, offset, size - offset)
            if (n < 0) throw EOFException("WAV 过早结束，期望 $size 已读 $offset")
            offset += n
        }
        return buf
    }

    private fun skipFully(input: InputStream, size: Long) {
        var left = size
        while (left > 0) {
            val skipped = input.skip(left)
            if (skipped <= 0) {
                if (input.read() < 0) throw EOFException("跳过 chunk 失败")
                left -= 1
            } else {
                left -= skipped
            }
        }
    }
}
