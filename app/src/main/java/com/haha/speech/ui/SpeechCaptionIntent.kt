package com.haha.speech.ui

import com.haha.mviFrame.base.IMviIntent

sealed interface SpeechCaptionIntent : IMviIntent {
    data object PrepareModel : SpeechCaptionIntent
    data object ToggleCapture : SpeechCaptionIntent
    data object PickAudio : SpeechCaptionIntent
    data class RecognizeFile(val uriString: String) : SpeechCaptionIntent
    data object ClearCaption : SpeechCaptionIntent
    data object StopCapture : SpeechCaptionIntent
}
