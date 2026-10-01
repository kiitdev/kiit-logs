package kiit.logs.sinks

import kiit.logs.FailingSink
import kiit.logs.LogEntry
import kiit.logs.LogLevel
import kiit.logs.MemorySink
import kiit.logs.fixedTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SinkTests {
    private fun entry(level: LogLevel = LogLevel.Info, action: String = "x") =
        LogEntry(name = "L", level = level, action = action, time = fixedTime)

    @Test
    fun a_composite_gives_every_sink_the_entry() {
        val a = MemorySink()
        val b = MemorySink()
        CompositeSink(a, b).emit(entry())
        assertEquals(1, a.entries.size)
        assertEquals(1, b.entries.size)
    }

    @Test
    fun a_failing_sink_does_not_stop_the_others_and_its_error_is_thrown_afterwards() {
        val ok = MemorySink()
        val composite = CompositeSink(FailingSink("one"), ok, FailingSink("two"))
        val error = assertFailsWith<IllegalStateException> { composite.emit(entry()) }
        assertEquals("one", error.message)
        assertEquals(listOf("two"), error.suppressedExceptions.map { it.message })
        assertEquals(1, ok.entries.size)
    }

    @Test
    fun a_composite_forwards_flush_and_close_to_every_sink() {
        val a = MemorySink()
        val b = MemorySink()
        val composite = CompositeSink(listOf(a, FailingSink(), b))
        assertFailsWith<IllegalStateException> { composite.flush() }
        assertFailsWith<IllegalStateException> { composite.close() }
        assertEquals(listOf(1, 1), listOf(a.flushed, b.flushed))
        assertEquals(listOf(1, 1), listOf(a.closed, b.closed))
    }

    @Test
    fun an_empty_composite_does_nothing() {
        val composite = CompositeSink()
        composite.emit(entry())
        composite.flush()
        composite.close()
        assertNull(composite.rawFor("x"))
    }

    @Test
    fun a_composite_gives_the_first_raw_that_exists() {
        val first =
            object : LogSink {
                override fun emit(entry: LogEntry) {}
            }
        val second =
            object : LogSink {
                override fun emit(entry: LogEntry) {}

                override fun rawFor(name: String): Any? = "second:$name"
            }
        val third =
            object : LogSink {
                override fun emit(entry: LogEntry) {}

                override fun rawFor(name: String): Any? = "third:$name"
            }
        assertEquals("second:A", CompositeSink(first, second, third).rawFor("A"))
        assertNull(CompositeSink(first).rawFor("A"))
    }

    @Test
    fun the_console_sink_writes_without_failing() {
        val sink = ConsoleSink()
        sink.emit(entry())
        sink.emit(LogEntry(name = "com.shop.Orders", level = LogLevel.Error, action = "charge", trace = "IllegalStateException: boom"))
        sink.flush()
        sink.close()
        assertEquals(4000, ConsoleSink.DEFAULT_MAX_LENGTH)
        assertTrue(ConsoleSink(maxLength = 0).let { true })
    }
}
