package com.haha.llmsdk.json

import com.haha.llmsdk.chat.ChatMessage
import com.haha.llmsdk.chat.ChatRequest
import com.haha.llmsdk.chat.ChatRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChatCompletionsCodecTest {

    @Test
    fun encodeContainsRolesAndStream() {
        val json = ChatCompletionsCodec.encodeRequest(
            ChatRequest(
                model = "gpt-4o-mini",
                messages = listOf(
                    ChatMessage(ChatRole.SYSTEM, "你是助手"),
                    ChatMessage(ChatRole.USER, "你好")
                ),
                stream = true
            )
        )
        assertTrue(json.contains("\"role\""))
        assertTrue(json.contains("system"))
        assertTrue(json.contains("user"))
        assertTrue(json.contains("stream"))
        assertTrue(!json.contains("apiKey"))
        assertTrue(!json.contains("Authorization"))
    }

    @Test
    fun decodeDeltaContent() {
        val chunk = """{"choices":[{"delta":{"content":"你"}}]}"""
        assertEquals("你", ChatCompletionsCodec.decodeDeltaContent(chunk))
        assertNull(
            ChatCompletionsCodec.decodeDeltaContent(
                """{"choices":[{"delta":{"role":"assistant"}}]}"""
            )
        )
    }

    @Test
    fun decodeCompleteAndUsage() {
        val json = """
            {
              "choices":[{"message":{"role":"assistant","content":"世界"}}],
              "usage":{"prompt_tokens":12,"completion_tokens":2}
            }
        """.trimIndent()
        assertEquals("世界", ChatCompletionsCodec.decodeCompleteContent(json))
        assertEquals(12 to 2, ChatCompletionsCodec.decodeUsage(json))
    }

    @Test
    fun decodeErrorMessage() {
        val json = """{"error":{"message":"invalid api key","type":"auth"}}"""
        assertEquals("invalid api key", ChatCompletionsCodec.decodeErrorMessage(json))
    }
}
