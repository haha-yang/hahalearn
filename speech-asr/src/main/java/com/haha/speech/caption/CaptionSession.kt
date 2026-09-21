package com.haha.speech.caption

import android.content.Context
import android.net.Uri
import android.util.Log
import com.haha.speech.asr.AsrEngineFactory
import com.haha.speech.asr.AsrEvent
import com.haha.speech.asr.StreamingAsrEngine
import com.haha.speech.decode.AudioFileDecoder
import com.haha.speech.feature.EnergyVad
import com.haha.speech.model.ModelStore
import com.haha.speech.pcm.MicPcmSource
import com.haha.speech.pcm.PcmFormat
import com.haha.speech.pcm.PcmUtils
import java.io.File
import java.io.FileOutputStream

/**
 * 把麦克风 / 文件 PCM 接到识别引擎，再聚合成字幕。
 */
class CaptionSession(
    context: Context,
    private val listener: Listener
) {
    interface Listener {
        fun onLevel(rms: Float, speaking: Boolean)
        fun onCaption(committed: String, partial: String, display: String)
        fun onEngineChanged(name: String)
        fun onError(message: String)
    }

    private val appContext = context.applicationContext
    val modelStore = ModelStore(appContext)
    private val captions = CaptionAggregator()
    private val vad = EnergyVad()
    private val mic = MicPcmSource()
    private var engine: StreamingAsrEngine = AsrEngineFactory.create(modelStore)

    @Volatile
    var capturing: Boolean = false
        private set

    val engineName: String get() = engine.name

    init {
        bindEngine(engine)
        listener.onEngineChanged(engine.name)
    }

    fun reloadEngine() {
        stopMic()
        engine.release()
        engine = AsrEngineFactory.create(modelStore)
        bindEngine(engine)
        listener.onEngineChanged(engine.name)
    }

    fun startMic() {
        if (capturing) return
        try {
            engine.prepare()
            vad.reset()
            capturing = true
            mic.start { bytes ->
                val samples = PcmUtils.bytesToFloatMono(bytes, mic.pcmFormat)
                val rms = PcmUtils.rms(samples)
                val speaking = vad.accept(rms)
                listener.onLevel(rms, speaking)
                engine.acceptWaveform(samples, mic.pcmFormat.sampleRate)
            }
        } catch (t: Throwable) {
            capturing = false
            Log.e(TAG, "startMic failed", t)
            listener.onError(t.message ?: "启动录音失败")
        }
    }

    fun stopMic() {
        if (!capturing) {
            mic.stop()
            return
        }
        capturing = false
        mic.stop()
        runCatching { engine.inputFinished() }
    }

    fun recognizeFile(uri: Uri) {
        stopMic()
        try {
            val cache = copyUriToCache(uri)
            val decoded = AudioFileDecoder.decodeToAsrPcm(cache)
            engine.prepare()
            val chunk = 1600
            var index = 0
            val samples = decoded.samples
            while (index < samples.size) {
                val end = (index + chunk).coerceAtMost(samples.size)
                val piece = samples.copyOfRange(index, end)
                val rms = PcmUtils.rms(piece)
                listener.onLevel(rms, vad.accept(rms))
                engine.acceptWaveform(piece, decoded.format.sampleRate)
                index = end
            }
            engine.inputFinished()
        } catch (t: Throwable) {
            Log.e(TAG, "recognizeFile failed", t)
            listener.onError(t.message ?: "解析音频失败")
        }
    }

    fun clearCaptions() {
        captions.clear()
        listener.onCaption("", "", "")
    }

    fun release() {
        stopMic()
        engine.release()
    }

    private fun bindEngine(target: StreamingAsrEngine) {
        target.setListener { event ->
            captions.onEvent(event)
            listener.onCaption(
                captions.committedText(),
                captions.partialText(),
                captions.displayText()
            )
            if (event is AsrEvent.Final) {
                Log.i(TAG, "final=${event.text}")
            }
        }
    }

    private fun copyUriToCache(uri: Uri): File {
        val dest = File(appContext.cacheDir, "speech-asr-input-${System.currentTimeMillis()}")
        appContext.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        } ?: error("无法读取所选文件")
        return dest
    }

    companion object {
        private const val TAG = "CaptionSession"
        val pcmHint: String
            get() {
                val f = PcmFormat.ASR_DEFAULT
                return "${f.sampleRate}Hz / ${f.channelCount}ch / ${f.bitsPerSample}bit PCM"
            }
    }
}
