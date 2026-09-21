package com.haha.speech.ui

import android.app.Application
import android.net.Uri
import com.haha.mviFrame.base.BaseMVIViewModel
import com.haha.speech.caption.CaptionSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpeechCaptionViewModel(application: Application) :
    BaseMVIViewModel<SpeechCaptionIntent, SpeechCaptionUiState, SpeechCaptionUiEffect>(
        application,
        SpeechCaptionUiState()
    ) {

    private val session = CaptionSession(application, object : CaptionSession.Listener {
        override fun onLevel(rms: Float, speaking: Boolean) {
            setState { copy(rms = rms, isSpeech = speaking) }
        }

        override fun onCaption(committed: String, partial: String, display: String) {
            setState {
                copy(
                    committedText = committed,
                    partialText = partial,
                    captionText = display
                )
            }
        }

        override fun onEngineChanged(name: String) {
            setState { copy(engineName = name) }
        }

        override fun onError(message: String) {
            setState { copy(error = message, isCapturing = false) }
            sendEffect(SpeechCaptionUiEffect.ShowToast(message))
        }
    })

    init {
        val ready = session.modelStore.isReady() || session.modelStore.copyFromAssetsIfPresent()
        if (ready) {
            session.reloadEngine()
        }
        setState {
            copy(
                modelState = if (session.modelStore.isReady()) {
                    SpeechModelState.Ready
                } else {
                    SpeechModelState.Missing
                },
                engineName = session.engineName,
                pcmHint = CaptionSession.pcmHint
            )
        }
    }

    override suspend fun handleIntent(intent: SpeechCaptionIntent) {
        when (intent) {
            SpeechCaptionIntent.PrepareModel -> prepareModel()
            SpeechCaptionIntent.ToggleCapture -> toggleCapture()
            SpeechCaptionIntent.PickAudio -> sendEffect(SpeechCaptionUiEffect.OpenAudioPicker)
            is SpeechCaptionIntent.RecognizeFile -> recognizeFile(intent.uriString)
            SpeechCaptionIntent.ClearCaption -> session.clearCaptions()
            SpeechCaptionIntent.StopCapture -> stopCapture()
        }
    }

    private suspend fun prepareModel() {
        if (session.modelStore.isReady()) {
            session.reloadEngine()
            setState {
                copy(
                    modelState = SpeechModelState.Ready,
                    engineName = session.engineName,
                    error = null
                )
            }
            sendEffect(SpeechCaptionUiEffect.ShowToast("模型已就绪"))
            return
        }
        val localArchive = session.modelStore.hasLocalArchive()
        setState {
            copy(
                modelState = SpeechModelState.Downloading,
                downloadProgress = 0f,
                downloadLabel = if (localArchive) {
                    "安装包已存在，正在解压…"
                } else {
                    "安装包不存在，开始下载…"
                },
                error = null
            )
        }
        try {
            val downloaded = withContext(Dispatchers.IO) {
                session.modelStore.prepare { received, total ->
                    val progress = if (total > 0) received.toFloat() / total else 0f
                    val label = if (total > 0 && received < total) {
                        "下载 ${received / 1024 / 1024}MB / ${total / 1024 / 1024}MB"
                    } else if (localArchive || received == total) {
                        "正在解压安装包…"
                    } else {
                        "已下载 ${received / 1024 / 1024}MB"
                    }
                    setState {
                        copy(
                            modelState = SpeechModelState.Downloading,
                            downloadProgress = progress.coerceIn(0f, 1f),
                            downloadLabel = label
                        )
                    }
                }
            }
            session.reloadEngine()
            val doneLabel = if (downloaded) "下载并解压完成" else "安装包已存在，已解压"
            setState {
                copy(
                    modelState = SpeechModelState.Ready,
                    downloadProgress = 1f,
                    downloadLabel = doneLabel,
                    engineName = session.engineName
                )
            }
            sendEffect(SpeechCaptionUiEffect.ShowToast(doneLabel))
        } catch (t: Throwable) {
            setState {
                copy(
                    modelState = SpeechModelState.Failed,
                    error = t.message,
                    downloadLabel = t.message ?: "下载失败"
                )
            }
            sendEffect(
                SpeechCaptionUiEffect.ShowToast(
                    t.message ?: "下载失败，可把模型放到 assets/speech-asr/zh-14m/"
                )
            )
        }
    }

    private fun toggleCapture() {
        if (session.capturing) {
            stopCapture()
            return
        }
        session.startMic()
        setState { copy(isCapturing = session.capturing, error = null) }
    }

    private fun stopCapture() {
        session.stopMic()
        setState { copy(isCapturing = false, rms = 0f, isSpeech = false) }
    }

    private suspend fun recognizeFile(uriString: String) {
        stopCapture()
        setState { copy(error = null) }
        try {
            withContext(Dispatchers.IO) {
                session.recognizeFile(Uri.parse(uriString))
            }
        } catch (t: Throwable) {
            setState { copy(error = t.message) }
            sendEffect(SpeechCaptionUiEffect.ShowToast(t.message ?: "识别文件失败"))
        }
    }

    override fun onCleared() {
        session.release()
        super.onCleared()
    }
}
