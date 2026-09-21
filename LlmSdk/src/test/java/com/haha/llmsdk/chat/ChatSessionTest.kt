package com.haha.llmsdk.chat

import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ChatSessionTest {

    @Test
    fun mockStreamCompletesAndKeepsRoles() {
        val done = CountDownLatch(1)
        var latest = ""
        val session = ChatSession(object : ChatSession.Listener {
            override fun onEngineChanged(name: String) = Unit
            override fun onTranscript(transcript: String, partial: String) {
                latest = transcript
                if (partial.isEmpty() && transcript.contains("[assistant]")) {
                    done.countDown()
                }
            }

            override fun onRequest(url: String, headerHint: String, bodyJson: String) = Unit
            override fun onSse(raw: String) = Unit
            override fun onUsage(promptTokens: Int, completionTokens: Int) = Unit
            override fun onStreamingChanged(streaming: Boolean) = Unit
            override fun onError(message: String) = Unit
        })
        session.send("什么是 SSE")
        assertTrue(done.await(5, TimeUnit.SECONDS))
        val messages = session.snapshotMessages()
        assertTrue(messages.any { it.role == ChatRole.SYSTEM })
        assertTrue(messages.any { it.role == ChatRole.USER })
        assertTrue(messages.any { it.role == ChatRole.ASSISTANT })
        assertTrue(latest.contains("[assistant]"))
        session.release()
    }
}
