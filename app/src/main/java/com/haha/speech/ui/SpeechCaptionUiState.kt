package com.haha.speech.ui

import com.haha.baseui.mvi.IMviUiEffect
import com.haha.baseui.mvi.IMviUiState

enum class SpeechModelState {
    Missing,
    Downloading,
    Ready,
    Failed
}

data class SpeechCaptionUiState(
    val modelState: SpeechModelState = SpeechModelState.Missing,
    val downloadProgress: Float = 0f,
    val downloadLabel: String = "",
    val engineName: String = "",
    val pcmHint: String = "",
    val isCapturing: Boolean = false,
    val rms: Float = 0f,
    val isSpeech: Boolean = false,
    val committedText: String = "",
    val partialText: String = "",
    val captionText: String = "",
    val error: String? = null
) : IMviUiState {
    val rmsPercent: Int
        get() = (rms.coerceIn(0f, 0.2f) / 0.2f * 100).toInt()
}

sealed interface SpeechCaptionUiEffect : IMviUiEffect {
    data class ShowToast(val message: String) : SpeechCaptionUiEffect
    data object OpenAudioPicker : SpeechCaptionUiEffect
}
