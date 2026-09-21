package com.haha.llmsdk.sse

/**
 * 极简 SSE 行解析。OpenAI Compatible 流式接口是 `text/event-stream`：
 * 一行 `data: {json}` 就是一个 chunk，空行是事件分隔，`data: [DONE]` 表示结束。
 */
class SseParser {

    fun parseLine(line: String): SseEvent? {
        val trimmed = line.trimEnd('\r')
        if (trimmed.isEmpty()) {
            return SseEvent.Dispatch
        }
        if (trimmed.startsWith(":")) {
            return null
        }
        val colon = trimmed.indexOf(':')
        if (colon <= 0) {
            return null
        }
        val field = trimmed.substring(0, colon)
        var value = trimmed.substring(colon + 1)
        if (value.startsWith(" ")) {
            value = value.substring(1)
        }
        return when (field) {
            "data" -> SseEvent.Data(value)
            "event" -> SseEvent.Event(value)
            "id" -> SseEvent.Id(value)
            else -> null
        }
    }
}

sealed interface SseEvent {
    data class Data(val data: String) : SseEvent
    data class Event(val name: String) : SseEvent
    data class Id(val id: String) : SseEvent
    data object Dispatch : SseEvent
}
