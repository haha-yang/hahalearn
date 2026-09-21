package com.haha.llmsdk.chat

enum class ChatRole(val wire: String) {
    SYSTEM("system"),
    USER("user"),
    ASSISTANT("assistant");

    companion object {
        fun fromWire(value: String): ChatRole {
            return entries.firstOrNull { it.wire == value } ?: USER
        }
    }
}

data class ChatMessage(
    val role: ChatRole,
    val content: String
)

data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val stream: Boolean
)

/**
 * 一次 chat 过程中的学习事件：请求体、SSE 原文、增量 token、结束或错误。
 */
sealed interface LlmEvent {
    data class RequestReady(
        val url: String,
        val headerHint: String,
        val bodyJson: String
    ) : LlmEvent

    data class SseLine(val raw: String) : LlmEvent

    data class Token(val text: String) : LlmEvent

    data class Usage(val promptTokens: Int, val completionTokens: Int) : LlmEvent

    data class Done(val fullText: String) : LlmEvent

    data class Error(val message: String) : LlmEvent
}

fun interface LlmListener {
    fun onEvent(event: LlmEvent)
}

fun interface LlmCall {
    fun cancel()
}
