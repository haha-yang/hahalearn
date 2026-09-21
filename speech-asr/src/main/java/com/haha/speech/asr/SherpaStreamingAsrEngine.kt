package com.haha.speech.asr

import com.haha.speech.decode.LinearResampler
import com.haha.speech.model.SherpaZh14mModel
import com.k2fsa.sherpa.onnx.EndpointConfig
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OnlineModelConfig
import com.k2fsa.sherpa.onnx.OnlineRecognizer
import com.k2fsa.sherpa.onnx.OnlineRecognizerConfig
import com.k2fsa.sherpa.onnx.OnlineStream
import com.k2fsa.sherpa.onnx.OnlineTransducerModelConfig
import java.io.File

/**
 * sherpa-onnx 流式 transducer。PCM 进 [OnlineStream.acceptWaveform]，endpoint 出一句 Final。
 * v1.13.5 AAR 是 Kotlin data class，不是 Java builder。
 */
class SherpaStreamingAsrEngine(
    private val modelDir: File
) : StreamingAsrEngine {

    override val name: String = "Sherpa Zipformer 中文 14M"

    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null
    private var listener: AsrListener? = null
    private var lastPartial = ""

    override fun prepare() {
        release()
        val transducer = OnlineTransducerModelConfig().apply {
            encoder = File(modelDir, SherpaZh14mModel.ENCODER).absolutePath
            decoder = File(modelDir, SherpaZh14mModel.DECODER).absolutePath
            joiner = File(modelDir, SherpaZh14mModel.JOINER).absolutePath
        }
        val modelConfig = OnlineModelConfig().apply {
            this.transducer = transducer
            tokens = File(modelDir, SherpaZh14mModel.TOKENS).absolutePath
            numThreads = 2
            debug = false
            modelType = SherpaZh14mModel.MODEL_TYPE
        }
        val featConfig = FeatureConfig().apply {
            sampleRate = 16_000
            featureDim = 80
        }
        val config = OnlineRecognizerConfig().apply {
            this.featConfig = featConfig
            this.modelConfig = modelConfig
            decodingMethod = "greedy_search"
            enableEndpoint = true
            endpointConfig = EndpointConfig()
        }
        val onlineRecognizer = OnlineRecognizer(assetManager = null, config = config)
        recognizer = onlineRecognizer
        stream = onlineRecognizer.createStream()
        lastPartial = ""
    }

    override fun acceptWaveform(samples: FloatArray, sampleRate: Int) {
        val onlineRecognizer = recognizer ?: return
        val onlineStream = stream ?: return
        val wave = LinearResampler.resample(samples, sampleRate, 16_000)
        onlineStream.acceptWaveform(wave, 16_000)
        decodeAndEmit(onlineRecognizer, onlineStream, isEnd = false)
    }

    override fun inputFinished() {
        val onlineRecognizer = recognizer ?: return
        val onlineStream = stream ?: return
        val tail = FloatArray((0.8 * 16_000).toInt())
        onlineStream.acceptWaveform(tail, 16_000)
        onlineStream.inputFinished()
        decodeAndEmit(onlineRecognizer, onlineStream, isEnd = true)
        onlineRecognizer.reset(onlineStream)
        lastPartial = ""
    }

    override fun release() {
        runCatching { stream?.release() }
        runCatching { recognizer?.release() }
        stream = null
        recognizer = null
        lastPartial = ""
    }

    override fun setListener(listener: AsrListener?) {
        this.listener = listener
    }

    private fun decodeAndEmit(
        onlineRecognizer: OnlineRecognizer,
        onlineStream: OnlineStream,
        isEnd: Boolean
    ) {
        while (onlineRecognizer.isReady(onlineStream)) {
            onlineRecognizer.decode(onlineStream)
        }
        val text = onlineRecognizer.getResult(onlineStream).text.trim()
        val endpoint = isEnd || onlineRecognizer.isEndpoint(onlineStream)
        if (text.isNotEmpty() && text != lastPartial) {
            lastPartial = text
            listener?.onEvent(AsrEvent.Partial(text))
        }
        if (endpoint) {
            if (text.isNotEmpty()) {
                listener?.onEvent(AsrEvent.Final(text))
            }
            lastPartial = ""
            if (!isEnd) {
                onlineRecognizer.reset(onlineStream)
            }
        }
    }
}
