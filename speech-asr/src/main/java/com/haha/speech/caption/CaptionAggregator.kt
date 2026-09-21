package com.haha.speech.caption

import com.haha.speech.asr.AsrEvent

/**
 * Partial 覆盖当前行，Final 追加历史。实时字幕只展示最近 [maxLines] 句。
 */
class CaptionAggregator(private val maxLines: Int = 24) {

    private val committed = ArrayList<String>()
    private var partial: String = ""

    fun onEvent(event: AsrEvent) {
        when (event) {
            is AsrEvent.Partial -> partial = event.text
            is AsrEvent.Final -> {
                val line = event.text.trim()
                if (line.isNotEmpty()) {
                    committed.add(line)
                    while (committed.size > maxLines) {
                        committed.removeAt(0)
                    }
                }
                partial = ""
            }
        }
    }

    fun clear() {
        committed.clear()
        partial = ""
    }

    fun displayText(): String {
        val lines = ArrayList<String>(committed.size + 1)
        lines.addAll(committed)
        if (partial.isNotBlank()) {
            lines.add(partial)
        }
        return lines.joinToString("\n")
    }

    fun committedText(): String = committed.joinToString("\n")

    fun partialText(): String = partial
}
