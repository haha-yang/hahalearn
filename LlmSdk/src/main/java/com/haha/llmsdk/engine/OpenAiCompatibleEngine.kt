package com.haha.llmsdk.engine

import com.haha.llmsdk.chat.ChatRequest
import com.haha.llmsdk.chat.LlmCall
import com.haha.llmsdk.chat.LlmEvent
import com.haha.llmsdk.chat.LlmListener
import com.haha.llmsdk.config.LlmConfig
import com.haha.llmsdk.json.ChatCompletionsCodec
import com.haha.llmsdk.sse.SseEvent
import com.haha.llmsdk.sse.SseParser
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

/**
 * OpenAI Compatible HTTP 引擎：`POST {baseUrl}/chat/completions`。
 * 兼容 OpenAI / DeepSeek / 硅基流动 / 本地 Ollama（/v1）等同一套 JSON。
 */
class OpenAiCompatibleEngine(
    private val config: LlmConfig,
    client: OkHttpClient? = null
) : LlmEngine {

    override val name: String = "OpenAI Compatible（${config.model}）"

    private val http = client ?: defaultClient()

    override fun chat(request: ChatRequest, listener: LlmListener): LlmCall {
        val url = chatCompletionsUrl(config.baseUrl)
        val bodyJson = ChatCompletionsCodec.encodeRequest(request, pretty = false)
        val prettyJson = ChatCompletionsCodec.encodeRequest(request, pretty = true)
        listener.onEvent(
            LlmEvent.RequestReady(
                url = url,
                headerHint = if (config.apiKey.isBlank()) {
                    "Authorization: （空，适合本地 Ollama）"
                } else {
                    "Authorization: Bearer ***"
                },
                bodyJson = prettyJson
            )
        )
        val builder = Request.Builder()
            .url(url)
            .header("Content-Type", "application/json")
            .header("Accept", if (request.stream) "text/event-stream" else "application/json")
            .post(bodyJson.toRequestBody(JSON_MEDIA))
        if (config.apiKey.isNotBlank()) {
            builder.header("Authorization", "Bearer ${config.apiKey}")
        }
        val call = http.newCall(builder.build())
        val canceled = AtomicBoolean(false)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (canceled.get() || call.isCanceled()) return
                listener.onEvent(LlmEvent.Error(e.message ?: "网络失败"))
            }

            override fun onResponse(call: Call, response: Response) {
                response.use { resp ->
                    if (canceled.get() || call.isCanceled()) return
                    val body = resp.body ?: run {
                        listener.onEvent(LlmEvent.Error("HTTP ${resp.code} 空响应"))
                        return
                    }
                    if (!resp.isSuccessful) {
                        val raw = body.string()
                        val message = ChatCompletionsCodec.decodeErrorMessage(raw)
                            ?: raw.ifBlank { "HTTP ${resp.code}" }
                        listener.onEvent(LlmEvent.Error(message))
                        return
                    }
                    if (request.stream) {
                        readStream(body.source(), canceled, listener)
                    } else {
                        emitComplete(body.string(), listener)
                    }
                }
            }
        })
        return LlmCall {
            canceled.set(true)
            call.cancel()
        }
    }

    override fun release() = Unit

    private fun readStream(
        source: okio.BufferedSource,
        canceled: AtomicBoolean,
        listener: LlmListener
    ) {
        val parser = SseParser()
        val acc = StringBuilder()
        var finished = false
        try {
            while (!canceled.get() && !finished && !source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                val event = parser.parseLine(line)
                if (event is SseEvent.Data) {
                    finished = handleSseData(event.data, acc, listener)
                }
            }
            if (!canceled.get() && !finished) {
                listener.onEvent(LlmEvent.Done(acc.toString()))
            }
        } catch (t: Throwable) {
            if (!canceled.get()) {
                listener.onEvent(LlmEvent.Error(t.message ?: "读取流失败"))
            }
        }
    }

    private fun handleSseData(
        data: String,
        acc: StringBuilder,
        listener: LlmListener
    ): Boolean {
        listener.onEvent(LlmEvent.SseLine(data))
        if (data.trim() == "[DONE]") {
            listener.onEvent(LlmEvent.Done(acc.toString()))
            return true
        }
        try {
            val delta = ChatCompletionsCodec.decodeDeltaContent(data)
            if (!delta.isNullOrEmpty()) {
                acc.append(delta)
                listener.onEvent(LlmEvent.Token(delta))
            }
            ChatCompletionsCodec.decodeUsage(data)?.let { (prompt, completion) ->
                listener.onEvent(LlmEvent.Usage(prompt, completion))
            }
        } catch (t: Throwable) {
            listener.onEvent(LlmEvent.Error(t.message ?: "解析 SSE JSON 失败"))
            return true
        }
        return false
    }

    private fun emitComplete(raw: String, listener: LlmListener) {
        listener.onEvent(LlmEvent.SseLine(raw))
        try {
            val content = ChatCompletionsCodec.decodeCompleteContent(raw)
            if (content.isNotEmpty()) {
                listener.onEvent(LlmEvent.Token(content))
            }
            ChatCompletionsCodec.decodeUsage(raw)?.let { (prompt, completion) ->
                listener.onEvent(LlmEvent.Usage(prompt, completion))
            }
            listener.onEvent(LlmEvent.Done(content))
        } catch (t: Throwable) {
            listener.onEvent(LlmEvent.Error(t.message ?: "解析响应失败"))
        }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        fun chatCompletionsUrl(baseUrl: String): String {
            val base = baseUrl.trim().trimEnd('/')
            return if (base.endsWith("/chat/completions")) {
                base
            } else {
                "$base/chat/completions"
            }
        }

        fun defaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(20, TimeUnit.SECONDS)
                .writeTimeout(20, TimeUnit.SECONDS)
                .readTimeout(0, TimeUnit.MILLISECONDS)
                .build()
        }
    }
}
