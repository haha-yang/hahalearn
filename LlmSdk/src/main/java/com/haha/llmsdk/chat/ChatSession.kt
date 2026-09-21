package com.haha.llmsdk.chat

import com.haha.llmsdk.config.LlmConfig
import com.haha.llmsdk.config.LlmProvider
import com.haha.llmsdk.engine.LlmEngine
import com.haha.llmsdk.engine.LlmEngineFactory

/**
 * 会话层：维护 messages 历史，把引擎事件聚合成 transcript / partial。
 * 对应 speech-asr 的 CaptionSession，页面不要直接碰 Engine。
 */
class ChatSession(
    private val listener: Listener
) {
    interface Listener {
        fun onEngineChanged(name: String)
        fun onTranscript(transcript: String, partial: String)
        fun onRequest(url: String, headerHint: String, bodyJson: String)
        fun onSse(raw: String)
        fun onUsage(promptTokens: Int, completionTokens: Int)
        fun onStreamingChanged(streaming: Boolean)
        fun onError(message: String)
    }

    private val lock = Any()
    private val history = mutableListOf<ChatMessage>()
    private val assistantBuffer = StringBuilder()

    @Volatile
    private var config: LlmConfig = LlmConfig()

    @Volatile
    private var engine: LlmEngine = LlmEngineFactory.create(config)

    @Volatile
    private var activeCall: LlmCall? = null

    @Volatile
    private var generation: Int = 0

    val engineName: String
        get() = engine.name

    val streaming: Boolean
        get() = activeCall != null

    init {
        listener.onEngineChanged(engine.name)
    }

    fun setConfig(newConfig: LlmConfig) {
        if (newConfig == config) return
        stop()
        config = newConfig
        engine.release()
        engine = LlmEngineFactory.create(newConfig)
        listener.onEngineChanged(engine.name)
    }

    fun send(userText: String) {
        val text = userText.trim()
        if (text.isEmpty()) {
            listener.onError("请先输入一句 user 消息")
            return
        }
        if (config.provider == LlmProvider.OPENAI_COMPATIBLE && config.model.isBlank()) {
            listener.onError("HTTP 模式需要填写 model")
            return
        }
        stop()
        val gen = generation
        val request: ChatRequest
        synchronized(lock) {
            ensureSystemPromptLocked()
            history.add(ChatMessage(ChatRole.USER, text))
            request = ChatRequest(
                model = config.model.ifBlank { LlmConfig.DEFAULT_MODEL },
                messages = history.toList(),
                stream = config.stream
            )
        }
        assistantBuffer.setLength(0)
        listener.onTranscript(formatTranscript(), "")
        listener.onStreamingChanged(true)
        activeCall = engine.chat(request) { event ->
            if (gen != generation) return@chat
            handleEvent(event)
        }
    }

    fun stop() {
        generation++
        activeCall?.cancel()
        activeCall = null
        listener.onStreamingChanged(false)
        val leftover = synchronized(lock) {
            val text = assistantBuffer.toString()
            assistantBuffer.setLength(0)
            if (text.isNotEmpty()) {
                history.add(ChatMessage(ChatRole.ASSISTANT, text))
            }
            formatTranscriptLocked()
        }
        listener.onTranscript(leftover, "")
    }

    fun clear() {
        stop()
        synchronized(lock) {
            history.clear()
            assistantBuffer.setLength(0)
        }
        listener.onTranscript("", "")
        listener.onSse("")
        listener.onRequest("", "", "")
        listener.onUsage(0, 0)
    }

    fun release() {
        stop()
        engine.release()
    }

    fun snapshotMessages(): List<ChatMessage> {
        synchronized(lock) {
            return history.toList()
        }
    }

    private fun handleEvent(event: LlmEvent) {
        when (event) {
            is LlmEvent.RequestReady -> {
                listener.onRequest(event.url, event.headerHint, event.bodyJson)
            }

            is LlmEvent.SseLine -> listener.onSse(event.raw)

            is LlmEvent.Token -> {
                val partial: String
                val transcript: String
                synchronized(lock) {
                    assistantBuffer.append(event.text)
                    partial = assistantBuffer.toString()
                    transcript = formatTranscriptLocked()
                }
                listener.onTranscript(transcript, partial)
            }

            is LlmEvent.Usage -> listener.onUsage(event.promptTokens, event.completionTokens)

            is LlmEvent.Done -> {
                activeCall = null
                val full = event.fullText.ifEmpty { assistantBuffer.toString() }
                synchronized(lock) {
                    assistantBuffer.setLength(0)
                    if (full.isNotEmpty()) {
                        val last = history.lastOrNull()
                        if (last?.role != ChatRole.ASSISTANT || last.content != full) {
                            history.add(ChatMessage(ChatRole.ASSISTANT, full))
                        }
                    }
                }
                listener.onTranscript(formatTranscript(), "")
                listener.onStreamingChanged(false)
            }

            is LlmEvent.Error -> {
                activeCall = null
                listener.onStreamingChanged(false)
                listener.onError(event.message)
            }
        }
    }

    private fun ensureSystemPromptLocked() {
        if (config.systemPrompt.isBlank()) return
        if (history.none { it.role == ChatRole.SYSTEM }) {
            history.add(0, ChatMessage(ChatRole.SYSTEM, config.systemPrompt))
        }
    }

    private fun formatTranscript(): String {
        synchronized(lock) {
            return formatTranscriptLocked()
        }
    }

    private fun formatTranscriptLocked(): String {
        if (history.isEmpty()) return ""
        return history.joinToString("\n\n") { message ->
            "[${message.role.wire}]\n${message.content}"
        }
    }
}
