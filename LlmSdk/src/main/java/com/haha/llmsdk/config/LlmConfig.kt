package com.haha.llmsdk.config

/**
 * 引擎类型。Mock 不发网，用来看 messages / SSE 协议；HTTP 走 OpenAI 兼容 Chat Completions。
 */
enum class LlmProvider {
    MOCK,
    OPENAI_COMPATIBLE
}

/**
 * SDK 运行配置。apiKey 只用于 HTTP Header，不会写进请求 JSON。
 */
data class LlmConfig(
    val provider: LlmProvider = LlmProvider.MOCK,
    val baseUrl: String = DEFAULT_BASE_URL,
    val apiKey: String = "",
    val model: String = DEFAULT_MODEL,
    val stream: Boolean = true,
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_SYSTEM_PROMPT =
            "你是 HahaLearn 的 LLM 学习助手，用简体中文回答，必要时指出当前请求走的是 Mock 还是 HTTP。"
    }
}
