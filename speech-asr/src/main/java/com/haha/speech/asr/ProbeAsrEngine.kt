package com.haha.speech.asr

import com.haha.speech.feature.EnergyVad
import com.haha.speech.pcm.PcmUtils

/**
 * 无模型时的学习引擎：用 VAD 演示 Partial / Final 分句，方便先跑通字幕流水线。
 */
class ProbeAsrEngine : StreamingAsrEngine {

    override val name: String = "ProbeVAD（未加载中文模型）"

    private val vad = EnergyVad()
    private var listener: AsrListener? = null
    private var inSpeech = false
    private var speechSamples = 0

    override fun prepare() {
        vad.reset()
        inSpeech = false
        speechSamples = 0
    }

    override fun acceptWaveform(samples: FloatArray, sampleRate: Int) {
        val rms = PcmUtils.rms(samples)
        val speaking = vad.accept(rms)
        if (speaking) {
            speechSamples += samples.size
            if (!inSpeech) {
                inSpeech = true
            }
            val ms = speechSamples * 1000 / sampleRate.coerceAtLeast(1)
            listener?.onEvent(
                AsrEvent.Partial("检测到语音 ${ms}ms  RMS=${"%.3f".format(rms)}（请先准备模型）")
            )
        } else if (inSpeech) {
            inSpeech = false
            val ms = speechSamples * 1000 / sampleRate.coerceAtLeast(1)
            speechSamples = 0
            listener?.onEvent(
                AsrEvent.Final("（VAD 分句 ${ms}ms，尚未离线识别。点「准备模型」下载中文 Zipformer）")
            )
        }
    }

    override fun inputFinished() {
        if (inSpeech) {
            inSpeech = false
            listener?.onEvent(AsrEvent.Final("（文件结束，VAD 收尾）"))
        }
        speechSamples = 0
        vad.reset()
    }

    override fun release() {
        listener = null
        vad.reset()
    }

    override fun setListener(listener: AsrListener?) {
        this.listener = listener
    }
}
