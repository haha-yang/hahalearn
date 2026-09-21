package com.haha.speech.asr

/**
 * 流式识别事件。Partial 会不断被覆盖，Final 对应一次 endpoint（一句话）。
 */
sealed interface AsrEvent {
    data class Partial(val text: String) : AsrEvent
    data class Final(val text: String) : AsrEvent
}

fun interface AsrListener {
    fun onEvent(event: AsrEvent)
}

interface StreamingAsrEngine {
    val name: String
    fun prepare()
    fun acceptWaveform(samples: FloatArray, sampleRate: Int)
    fun inputFinished()
    fun release()
    fun setListener(listener: AsrListener?)
}
