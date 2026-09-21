package com.haha.speech.model

import java.io.File

/**
 * 小型流式中文 Zipformer（约 14M 参数，int8）。
 * 官方包：https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23.tar.bz2
 */
object SherpaZh14mModel {
    const val DIR_NAME = "sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23"
    const val ENCODER = "encoder-epoch-99-avg-1.int8.onnx"
    const val DECODER = "decoder-epoch-99-avg-1.onnx"
    const val JOINER = "joiner-epoch-99-avg-1.int8.onnx"
    const val TOKENS = "tokens.txt"
    const val MODEL_TYPE = "zipformer"

    const val ARCHIVE_NAME = "$DIR_NAME.tar.bz2"
    const val ARCHIVE_URL =
        "https://github.com/k2-fsa/sherpa-onnx/releases/download/asr-models/$ARCHIVE_NAME"

    fun isComplete(dir: File): Boolean {
        return File(dir, ENCODER).isFile &&
                File(dir, DECODER).isFile &&
                File(dir, JOINER).isFile &&
                File(dir, TOKENS).isFile
    }
}
