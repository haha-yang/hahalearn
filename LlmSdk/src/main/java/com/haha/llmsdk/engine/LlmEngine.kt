package com.haha.llmsdk.engine

import com.haha.llmsdk.chat.ChatRequest
import com.haha.llmsdk.chat.LlmCall
import com.haha.llmsdk.chat.LlmListener
import com.haha.llmsdk.config.LlmConfig
import com.haha.llmsdk.config.LlmProvider

/**
 * 大模型引擎：输入 [ChatRequest]，按事件吐出 token / SSE / 结束。
 * Mock 与 HTTP 实现同一接口，页面只依赖这一层。
 */
interface LlmEngine {
    val name: String
    fun chat(request: ChatRequest, listener: LlmListener): LlmCall
    fun release()
}

object LlmEngineFactory {
    fun create(config: LlmConfig): LlmEngine {
        return when (config.provider) {
            LlmProvider.MOCK -> MockLlmEngine()
            LlmProvider.OPENAI_COMPATIBLE -> OpenAiCompatibleEngine(config)
        }
    }
}
