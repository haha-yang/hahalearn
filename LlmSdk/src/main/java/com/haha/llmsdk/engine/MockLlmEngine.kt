package com.haha.llmsdk.engine

import com.haha.llmsdk.chat.ChatRequest
import com.haha.llmsdk.chat.ChatRole
import com.haha.llmsdk.chat.LlmCall
import com.haha.llmsdk.chat.LlmEvent
import com.haha.llmsdk.chat.LlmListener
import com.haha.llmsdk.json.ChatCompletionsCodec
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 本地 Mock：不发网，但按真实协议吐 Request JSON + SSE data 行 + token。
 * 用来先把 messages / stream 学明白，再换成 HTTP。
 */
class MockLlmEngine : LlmEngine {

    override val name: String = "Mock（本地协议演示）"

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "llmsdk-mock").apply { isDaemon = true }
    }

    override fun chat(request: ChatRequest, listener: LlmListener): LlmCall {
        val canceled = AtomicBoolean(false)
        val bodyJson = ChatCompletionsCodec.encodeRequest(request)
        listener.onEvent(
            LlmEvent.RequestReady(
                url = "mock://chat/completions",
                headerHint = "Authorization: Bearer ***（Mock 不发网）",
                bodyJson = bodyJson
            )
        )
        val future: Future<*> = executor.submit {
            try {
                val reply = replyFor(request)
                val tokens = tokenize(reply)
                if (request.stream) {
                    streamTokens(tokens, canceled, listener)
                } else {
                    if (canceled.get()) return@submit
                    listener.onEvent(
                        LlmEvent.SseLine("非流式：choices[0].message.content 一次返回")
                    )
                    listener.onEvent(LlmEvent.Token(reply))
                }
                if (canceled.get()) return@submit
                listener.onEvent(LlmEvent.Usage(estimateTokens(request), tokens.size))
                listener.onEvent(LlmEvent.Done(reply))
            } catch (t: Throwable) {
                if (!canceled.get()) {
                    listener.onEvent(LlmEvent.Error(t.message ?: "Mock 失败"))
                }
            }
        }
        return LlmCall {
            canceled.set(true)
            future.cancel(true)
        }
    }

    override fun release() {
        executor.shutdownNow()
    }

    private fun streamTokens(
        tokens: List<String>,
        canceled: AtomicBoolean,
        listener: LlmListener
    ) {
        tokens.forEach { token ->
            if (canceled.get() || Thread.currentThread().isInterrupted) {
                return
            }
            val sse = """{"choices":[{"delta":{"content":${jsonString(token)}}}]}"""
            listener.onEvent(LlmEvent.SseLine(sse))
            listener.onEvent(LlmEvent.Token(token))
            try {
                Thread.sleep(TOKEN_DELAY_MS)
            } catch (_: InterruptedException) {
                return
            }
        }
        if (!canceled.get()) {
            listener.onEvent(LlmEvent.SseLine("[DONE]"))
        }
    }

    private fun replyFor(request: ChatRequest): String {
        val userText = request.messages.lastOrNull { it.role == ChatRole.USER }?.content.orEmpty()
        val q = userText.trim()
        return when {
            q.contains("token", ignoreCase = true) || q.contains("令牌") ->
                "Token 是模型一次读写的最小单位。流式接口里每条 SSE 的 delta.content 通常就是一小段 token 文本，客户端要自己拼成完整 assistant 消息。"

            q.contains("SSE", ignoreCase = true) || q.contains("流式") ||
                    q.contains("stream", ignoreCase = true) ->
                "stream=true 时服务端用 text/event-stream。每一行 data: {json}，json 里 choices[0].delta.content 是增量；最后一行 data: [DONE]。"

            q.contains("role", ignoreCase = true) || q.contains("角色") ||
                    q.contains("messages", ignoreCase = true) ->
                "messages 就是对话状态。system 定人设，user 是你说的话，assistant 是模型回复。下次请求要把历史再带上，服务端默认不记会话。"

            q.isBlank() ->
                "请输入一句 user 消息。Mock 会按 Chat Completions 协议把 messages 编成 JSON，再假装流式吐 token。"

            else ->
                "【Mock 引擎】不会真正推理。你刚才的 user 内容是：「$q」。切到 OpenAI Compatible 后，这段会原样放进 POST /v1/chat/completions 的 messages。"
        }
    }

    private fun tokenize(text: String): List<String> {
        val tokens = ArrayList<String>()
        val latin = StringBuilder()
        fun flushLatin() {
            if (latin.isNotEmpty()) {
                tokens.add(latin.toString())
                latin.setLength(0)
            }
        }
        text.forEach { ch ->
            val isLatin = ch.code < 128 && (ch.isLetterOrDigit() || ch == '\'' || ch == '_')
            if (isLatin) {
                latin.append(ch)
            } else {
                flushLatin()
                tokens.add(ch.toString())
            }
        }
        flushLatin()
        return tokens
    }

    private fun estimateTokens(request: ChatRequest): Int {
        return request.messages.sumOf { tokenize(it.content).size }
    }

    private fun jsonString(value: String): String {
        val escaped = value
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }

    companion object {
        private const val TOKEN_DELAY_MS = 28L
    }
}
