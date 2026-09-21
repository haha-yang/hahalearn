package com.haha.speech.asr

import com.haha.speech.model.ModelStore

object AsrEngineFactory {

    fun create(modelStore: ModelStore): StreamingAsrEngine {
        return if (modelStore.isReady()) {
            SherpaStreamingAsrEngine(modelStore.modelDir)
        } else {
            ProbeAsrEngine()
        }
    }
}
