package com.haha.speech.feature

/**
 * 能量 VAD：RMS 过阈值认为在说话，静音后保留若干帧（hangover）避免把字切碎。
 * 真正的识别端点由 Sherpa 的 endpoint 规则负责，这里只给 UI 电平条和 Probe 引擎用。
 */
class EnergyVad(
    private val speechThreshold: Float = 0.018f,
    private val hangoverFrames: Int = 8
) {
    private var hangover = 0
    var speaking: Boolean = false
        private set

    fun accept(rms: Float): Boolean {
        if (rms >= speechThreshold) {
            hangover = hangoverFrames
            speaking = true
        } else if (hangover > 0) {
            hangover--
            speaking = true
        } else {
            speaking = false
        }
        return speaking
    }

    fun reset() {
        hangover = 0
        speaking = false
    }
}
