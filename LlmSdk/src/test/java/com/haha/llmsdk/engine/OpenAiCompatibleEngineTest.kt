package com.haha.llmsdk.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class OpenAiCompatibleEngineTest {

    @Test
    fun appendChatCompletionsPath() {
        assertEquals(
            "https://api.openai.com/v1/chat/completions",
            OpenAiCompatibleEngine.chatCompletionsUrl("https://api.openai.com/v1")
        )
        assertEquals(
            "http://10.0.2.2:11434/v1/chat/completions",
            OpenAiCompatibleEngine.chatCompletionsUrl("http://10.0.2.2:11434/v1/")
        )
        assertEquals(
            "https://api.deepseek.com/v1/chat/completions",
            OpenAiCompatibleEngine.chatCompletionsUrl(
                "https://api.deepseek.com/v1/chat/completions"
            )
        )
    }
}
