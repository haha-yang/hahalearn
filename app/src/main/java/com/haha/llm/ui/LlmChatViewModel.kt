package com.haha.llm.ui

import android.app.Application
import android.content.Context
import com.haha.llmsdk.chat.ChatSession
import com.haha.llmsdk.config.LlmConfig
import com.haha.llmsdk.config.LlmProvider
import com.haha.mviFrame.base.BaseMVIViewModel

class LlmChatViewModel(application: Application) :
    BaseMVIViewModel<LlmChatIntent, LlmChatUiState, LlmChatUiEffect>(
        application,
        LlmChatUiState()
    ) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val session = ChatSession(object : ChatSession.Listener {
        override fun onEngineChanged(name: String) {
            setState { copy(engineName = name) }
        }

        override fun onTranscript(transcript: String, partial: String) {
            setState { copy(transcript = transcript, partialText = partial) }
        }

        override fun onRequest(url: String, headerHint: String, bodyJson: String) {
            setState {
                copy(
                    requestUrl = url,
                    requestHeader = headerHint,
                    requestJson = bodyJson
                )
            }
        }

        override fun onSse(raw: String) {
            setState { copy(lastSse = raw) }
        }

        override fun onUsage(promptTokens: Int, completionTokens: Int) {
            val label = if (promptTokens == 0 && completionTokens == 0) {
                ""
            } else {
                "usage：prompt $promptTokens / completion $completionTokens"
            }
            setState { copy(usageLabel = label) }
        }

        override fun onStreamingChanged(streaming: Boolean) {
            setState { copy(isStreaming = streaming) }
        }

        override fun onError(message: String) {
            setState { copy(error = message, isStreaming = false) }
            sendEffect(LlmChatUiEffect.ShowToast(message))
        }
    })

    init {
        val saved = loadConfig()
        session.setConfig(saved.toSdkConfig())
        setState {
            copy(
                provider = if (saved.mock) LlmProvider.MOCK else LlmProvider.OPENAI_COMPATIBLE,
                engineName = session.engineName
            )
        }
        sendEffect(LlmChatUiEffect.FillConfig(saved))
    }

    override suspend fun handleIntent(intent: LlmChatIntent) {
        when (intent) {
            is LlmChatIntent.ApplyConfig -> applyConfig(intent.config)
            is LlmChatIntent.SendMessage -> {
                applyConfig(intent.config)
                session.send(intent.text)
                sendEffect(LlmChatUiEffect.ClearInput)
            }

            LlmChatIntent.Stop -> session.stop()
            LlmChatIntent.Clear -> {
                session.clear()
                setState {
                    copy(
                        transcript = "",
                        partialText = "",
                        requestJson = "",
                        requestUrl = "",
                        requestHeader = "",
                        lastSse = "",
                        usageLabel = "",
                        error = null
                    )
                }
            }
        }
    }

    private fun applyConfig(ui: LlmUiConfig) {
        saveConfig(ui)
        session.setConfig(ui.toSdkConfig())
        setState {
            copy(
                provider = if (ui.mock) LlmProvider.MOCK else LlmProvider.OPENAI_COMPATIBLE,
                engineName = session.engineName,
                error = null
            )
        }
    }

    private fun loadConfig(): LlmUiConfig {
        return LlmUiConfig(
            mock = prefs.getBoolean(KEY_MOCK, true),
            baseUrl = prefs.getString(KEY_BASE_URL, LlmConfig.DEFAULT_BASE_URL)
                ?: LlmConfig.DEFAULT_BASE_URL,
            model = prefs.getString(KEY_MODEL, LlmConfig.DEFAULT_MODEL)
                ?: LlmConfig.DEFAULT_MODEL,
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
            stream = prefs.getBoolean(KEY_STREAM, true)
        )
    }

    private fun saveConfig(ui: LlmUiConfig) {
        prefs.edit()
            .putBoolean(KEY_MOCK, ui.mock)
            .putString(KEY_BASE_URL, ui.baseUrl)
            .putString(KEY_MODEL, ui.model)
            .putString(KEY_API_KEY, ui.apiKey)
            .putBoolean(KEY_STREAM, ui.stream)
            .apply()
    }

    private fun LlmUiConfig.toSdkConfig(): LlmConfig {
        return LlmConfig(
            provider = if (mock) LlmProvider.MOCK else LlmProvider.OPENAI_COMPATIBLE,
            baseUrl = baseUrl.ifBlank { LlmConfig.DEFAULT_BASE_URL },
            apiKey = apiKey.trim(),
            model = model.ifBlank { LlmConfig.DEFAULT_MODEL },
            stream = stream
        )
    }

    override fun onCleared() {
        session.release()
        super.onCleared()
    }

    companion object {
        private const val PREFS_NAME = "llm_learn_demo"
        private const val KEY_MOCK = "mock"
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_MODEL = "model"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_STREAM = "stream"
    }
}
