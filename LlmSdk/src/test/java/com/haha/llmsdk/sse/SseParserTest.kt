package com.haha.llmsdk.sse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SseParserTest {

    private val parser = SseParser()

    @Test
    fun parseDataLineWithSpace() {
        val event = parser.parseLine("data: {\"choices\":[]}")
        assertTrue(event is SseEvent.Data)
        assertEquals("{\"choices\":[]}", (event as SseEvent.Data).data)
    }

    @Test
    fun parseDone() {
        val event = parser.parseLine("data: [DONE]")
        assertEquals("[DONE]", (event as SseEvent.Data).data)
    }

    @Test
    fun parseEmptyLineAsDispatch() {
        assertEquals(SseEvent.Dispatch, parser.parseLine(""))
        assertEquals(SseEvent.Dispatch, parser.parseLine("\r"))
    }

    @Test
    fun ignoreComment() {
        assertNull(parser.parseLine(": keep-alive"))
    }

    @Test
    fun parseEventAndId() {
        assertEquals("token", (parser.parseLine("event: token") as SseEvent.Event).name)
        assertEquals("1", (parser.parseLine("id: 1") as SseEvent.Id).id)
    }
}
