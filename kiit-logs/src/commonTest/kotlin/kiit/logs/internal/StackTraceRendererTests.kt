package kiit.logs.internal

import kiit.logs.StackTraces
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StackTraceRendererTests {
    private fun chain(depth: Int): Exception {
        var ex: Exception = IllegalStateException("cause-$depth")
        for (i in depth - 1 downTo 1) ex = RuntimeException("cause-$i", ex)
        return ex
    }

    @Test
    fun off_renders_nothing() {
        assertNull(StackTraceBuilder.render(StackTraces.Off, IllegalStateException("boom")))
    }

    @Test
    fun summary_is_the_type_and_message() {
        assertEquals("IllegalStateException: boom", StackTraceBuilder.render(StackTraces.Summary, IllegalStateException("boom")))
    }

    @Test
    fun summary_follows_the_cause_chain() {
        val ex = RuntimeException("charge failed", IllegalArgumentException("bad card", IllegalStateException("closed")))
        assertEquals(
            "RuntimeException: charge failed\nCaused by: IllegalArgumentException: bad card\nCaused by: IllegalStateException: closed",
            StackTraceBuilder.render(StackTraces.Summary, ex),
        )
    }

    @Test
    fun summary_stops_after_five_causes() {
        val rendered = StackTraceBuilder.render(StackTraces.Summary, chain(9))
        assertNotNull(rendered)
        assertEquals(5, rendered.lines().size)
    }

    @Test
    fun full_includes_the_message() {
        val rendered = StackTraceBuilder.render(StackTraces.Full, IllegalStateException("boom"), maxLines = 1000)
        assertNotNull(rendered)
        assertTrue("boom" in rendered)
        assertTrue("more lines" !in rendered)
    }

    @Test
    fun full_is_cut_to_the_line_cap() {
        val message = (1..10).joinToString("\n") { "line$it" }
        val rendered = StackTraceBuilder.render(StackTraces.Full, IllegalStateException(message), maxLines = 3)
        assertNotNull(rendered)
        val lines = rendered.lines()
        assertEquals(4, lines.size)
        assertTrue(lines.last().startsWith("... ") && lines.last().endsWith(" more lines"), lines.last())
        assertTrue("line3" in lines.take(3).joinToString("\n"))
    }
}
