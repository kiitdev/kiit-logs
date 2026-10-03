package kiit.logs.sinks

import kiit.logs.LogEntry
import kiit.logs.LogLevel
import kiit.logs.LogSettings
import kiit.logs.Logs
import kiit.logs.fixedTime
import kiit.logs.testLogger
import kiit.logs.testSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemorySinkTests {
    private fun entry(action: String) = LogEntry(name = "L", level = LogLevel.Info, action = action, time = fixedTime)

    @Test
    fun entries_are_in_the_order_they_were_logged() {
        val sink = MemorySink()
        listOf("a", "b", "c").forEach { sink.emit(entry(it)) }
        assertEquals(listOf("a", "b", "c"), sink.entries.map { it.action })
    }

    @Test
    fun entries_is_a_snapshot_that_later_entries_are_not_added_to() {
        val sink = MemorySink()
        sink.emit(entry("a"))
        val before = sink.entries
        sink.emit(entry("b"))
        assertEquals(1, before.size)
        assertEquals(2, sink.entries.size)
    }

    @Test
    fun clear_removes_the_entries() {
        val sink = MemorySink()
        sink.emit(entry("a"))
        sink.clear()
        assertTrue(sink.entries.isEmpty())
        sink.emit(entry("b"))
        assertEquals(listOf("b"), sink.entries.map { it.action })
    }

    @Test
    fun find_returns_the_entries_with_that_action_in_order() {
        val sink = MemorySink()
        listOf("a", "b", "a").forEachIndexed { i, action -> sink.emit(entry(action).copy(msg = "$i")) }
        assertEquals(listOf("0", "2"), sink.find("a").map { it.msg })
    }

    @Test
    fun find_returns_nothing_when_no_entry_has_the_action() {
        val sink = MemorySink()
        sink.emit(entry("a"))
        sink.emit(LogEntry(name = "L", level = LogLevel.Info, msg = "no action", time = fixedTime))
        assertTrue(sink.find("b").isEmpty())
    }

    @Test
    fun flush_and_close_are_counted_and_clear_keeps_the_counts() {
        val sink = MemorySink()
        sink.flush()
        sink.flush()
        sink.close()
        sink.clear()
        assertEquals(2, sink.flushed)
        assertEquals(1, sink.closed)
    }

    @Test
    fun an_app_can_test_its_logging() {
        val sink = MemorySink()
        val logs = Logs(LogSettings.safe().copy(level = LogLevel.Info), sink)

        logs.logger("orders").info("place", "order_id" to "abc")

        assertEquals("place", sink.entries.single().action)
        assertEquals(listOf("order_id" to "abc"), sink.find("place").single().fields)
    }

    @Test
    fun the_fields_are_already_redacted_when_the_sink_gets_them() {
        val sink = MemorySink()
        val settings = testSettings().copy(policies = listOf(kiit.logs.policies.RedactPolicy()))
        testLogger(settings, sink).info("pay", "password" to "hunter2")
        assertTrue(sink.entries.single().fields.none { it.second == "hunter2" })
    }
}
