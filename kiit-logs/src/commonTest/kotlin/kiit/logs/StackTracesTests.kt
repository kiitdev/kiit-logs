package kiit.logs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StackTracesTests {
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
