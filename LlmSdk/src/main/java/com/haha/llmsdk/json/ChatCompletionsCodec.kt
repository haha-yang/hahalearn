package com.haha.llmsdk.json

import com.haha.llmsdk.chat.ChatRequest
import org.json.JSONArray
import org.json.JSONObject

/**
 * OpenAI Compatible `POST /v1/chat/completions` 编解码。
 * 请求体只有 model / messages / stream，apiKey 不进 JSON。
 */
object ChatCompletionsCodec {

    fun encodeRequest(request: ChatRequest, pretty: Boolean = true): String {
        val root = JSONObject()
        root.put("model", request.model)
        root.put("stream", request.stream)
        val messages = JSONArray()
        request.messages.forEach { message ->
            messages.put(
                JSONObject()
                    .put("role", message.role.wire)
                    .put("content", message.content)
            )
        }
        root.put("messages", messages)
        return if (pretty) root.toString(2) else root.toString()
    }

    fun decodeDeltaContent(json: String): String? {
        val root = parseObject(json) ?: return null
        throwIfError(root)
        val choices = root.optJSONArray("choices") ?: return null
        if (choices.length() == 0) return null
        val first = choices.optJSONObject(0) ?: return null
        val delta = first.optJSONObject("delta") ?: return null
        if (!delta.has("content") || delta.isNull("content")) return null
        return delta.optString("content")
    }

    fun decodeCompleteContent(json: String): String {
        val root = parseObject(json) ?: throw IllegalArgumentException("响应不是 JSON")
        throwIfError(root)
        val choices = root.optJSONArray("choices")
            ?: throw IllegalArgumentException("响应缺少 choices")
        if (choices.length() == 0) {
            throw IllegalArgumentException("choices 为空")
        }
        val message = choices.getJSONObject(0).optJSONObject("message")
            ?: throw IllegalArgumentException("响应缺少 message")
        return message.optString("content")
    }

    fun decodeUsage(json: String): Pair<Int, Int>? {
        val root = parseObject(json) ?: return null
        val usage = root.optJSONObject("usage") ?: return null
        if (!usage.has("prompt_tokens") && !usage.has("completion_tokens")) {
            return null
        }
        return usage.optInt("prompt_tokens") to usage.optInt("completion_tokens")
    }

    fun decodeErrorMessage(json: String): String? {
        val root = parseObject(json) ?: return null
        val error = root.optJSONObject("error") ?: return null
        val message = error.optString("message")
        return message.takeIf { it.isNotBlank() }
    }

    private fun parseObject(json: String): JSONObject? {
        val trimmed = json.trim()
        if (trimmed.isEmpty() || trimmed == "[DONE]") return null
        if (!trimmed.startsWith("{")) return null
        return try {
            JSONObject(trimmed)
        } catch (_: Exception) {
            null
        }
    }

    private fun throwIfError(root: JSONObject) {
        val error = root.optJSONObject("error") ?: return
        val message = error.optString("message").ifBlank { error.toString() }
        throw IllegalStateException(message)
    }
}
