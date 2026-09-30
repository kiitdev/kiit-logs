package kiit.logs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StackTracesTests {

    private fun chain(depth: Int): Exception {
        var ex: Exception = IllegalStateException("cause-$depth")
        for (i in depth - 1 downTo 1) ex = RuntimeException("cause-$i", ex)
        return ex
    }

    @Test
    fun off_renders_nothing() {
        assertNull(StackTraces.Off.render(IllegalStateException("boom")))
    }

    @Test
    fun summary_is_the_type_and_message() {
        assertEquals("IllegalStateException: boom", StackTraces.Summary.render(IllegalStateException("boom")))
    }

    @Test
    fun summary_follows_the_cause_chain() {
        val ex = RuntimeException("charge failed", IllegalArgumentException("bad card", IllegalStateException("closed")))
        assertEquals(
            "RuntimeException: charge failed\nCaused by: IllegalArgumentException: bad card\nCaused by: IllegalStateException: closed",
            StackTraces.Summary.render(ex)
        )
    }

    @Test
    fun summary_stops_after_five_causes() {
        val rendered = StackTraces.Summary.render(chain(9))
        assertNotNull(rendered)
        assertEquals(5, rendered.lines().size)
    }

    @Test
    fun full_includes_the_message() {
        val rendered = StackTraces.Full.render(IllegalStateException("boom"), maxLines = 1000)
        assertNotNull(rendered)
        assertTrue("boom" in rendered)
        assertTrue("more lines" !in rendered)
    }

    @Test
    fun full_is_cut_to_the_line_cap() {
        val message = (1..10).joinToString("\n") { "line$it" }
        val rendered = StackTraces.Full.render(IllegalStateException(message), maxLines = 3)
        assertNotNull(rendered)
        val lines = rendered.lines()
        assertEquals(4, lines.size)
        assertTrue(lines.last().startsWith("... ") && lines.last().endsWith(" more lines"), lines.last())
        assertTrue("line3" in lines.take(3).joinToString("\n"))
    }

    @Test
    fun the_entry_trace_follows_the_setting_and_the_exception_is_kept() {
        val ex = IllegalStateException("boom")
        fun traceFor(mode: StackTraces): String? {
            val sink = MemorySink()
            Logger(testSettings().copy(stackTraces = mode), "L", sink).error("charge", ex)
            val entry = sink.entries.single()
            assertEquals(ex, entry.ex)
            return entry.trace
        }
        assertNull(traceFor(StackTraces.Off))
        assertEquals("IllegalStateException: boom", traceFor(StackTraces.Summary))
        assertTrue("boom" in traceFor(StackTraces.Full).orEmpty())
    }

    @Test
    fun the_settings_cap_is_used_for_full_traces() {
        val message = (1..10).joinToString("\n") { "line$it" }
        val sink = MemorySink()
        val settings = testSettings().copy(stackTraces = StackTraces.Full, maxTraceLines = 2)
        Logger(settings, "L", sink).error("charge", IllegalStateException(message))
        assertEquals(3, sink.entries.single().trace.orEmpty().lines().size)
    }

    @Test
    fun no_exception_means_no_trace() {
        val sink = MemorySink()
        Logger(testSettings().copy(stackTraces = StackTraces.Full), "L", sink).error("charge")
        assertNull(sink.entries.single().trace)
    }
}
