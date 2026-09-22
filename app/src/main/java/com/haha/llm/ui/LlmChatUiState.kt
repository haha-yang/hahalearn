package com.haha.llm.ui

import com.haha.baseui.mvi.IMviIntent
import com.haha.baseui.mvi.IMviUiEffect
import com.haha.baseui.mvi.IMviUiState
import com.haha.llmsdk.config.LlmProvider

data class LlmUiConfig(
    val mock: Boolean = true,
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = "",
    val stream: Boolean = true
)

sealed interface LlmChatIntent : IMviIntent {
    data class ApplyConfig(val config: LlmUiConfig) : LlmChatIntent
    data class SendMessage(val text: String, val config: LlmUiConfig) : LlmChatIntent
    data object Stop : LlmChatIntent
    data object Clear : LlmChatIntent
}

data class LlmChatUiState(
    val provider: LlmProvider = LlmProvider.MOCK,
    val engineName: String = "",
    val isStreaming: Boolean = false,
    val transcript: String = "",
    val partialText: String = "",
    val requestUrl: String = "",
    val requestHeader: String = "",
    val requestJson: String = "",
    val lastSse: String = "",
    val usageLabel: String = "",
    val error: String? = null
) : IMviUiState {
    val httpConfigVisible: Boolean
        get() = provider == LlmProvider.OPENAI_COMPATIBLE
}

sealed interface LlmChatUiEffect : IMviUiEffect {
    data class ShowToast(val message: String) : LlmChatUiEffect
    data class FillConfig(val config: LlmUiConfig) : LlmChatUiEffect
    data object ClearInput : LlmChatUiEffect
}
