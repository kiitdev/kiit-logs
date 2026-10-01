package kiit.logs

import kiit.logs.sinks.ConsoleSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LogsTests {
    @Test
    fun a_logs_uses_the_settings_and_sends_entries_to_the_sink() {
        val sink = MemorySink()
        val settings = testSettings(LogLevel.Info)
        val factory = Logs(settings, sink)
        assertSame(settings, factory.settings)

        val log = factory.getLogger("orders")
        log.debug("skipped")
        log.info("place", "order_id" to "abc")
        assertEquals(1, sink.entries.size)
        assertEquals("place", sink.entries[0].action)
        assertEquals("orders", sink.entries[0].name)
        assertEquals(fixedTime, sink.entries[0].time)
    }

    @Test
    fun console_uses_the_settings_and_a_console_sink() {
        val settings = testSettings()
        val factory = Logs.console(settings)
        assertSame(settings, factory.settings)
        assertIs<ConsoleSink>(factory.provider)
    }

    @Test
    fun getLogger_returns_the_same_logger_for_the_same_name() {
        val factory = Logs(testSettings(), MemorySink())
        assertSame(factory.getLogger("a"), factory.getLogger("a"))
        assertTrue(factory.getLogger("a") !== factory.getLogger("b"))
    }

    @Test
    fun a_level_change_on_the_returned_factory_applies() {
        val sink = MemorySink()
        val factory = Logs(testSettings(LogLevel.Error), sink)
        val log = factory.getLogger("orders")
        log.info("before")
        factory.setLevel(LogLevel.Info)
        log.info("after")
        assertEquals(listOf("after"), sink.entries.map { it.action })
    }

    @Test
    fun each_console_call_returns_a_new_factory() {
        val settings = testSettings()
        assertTrue(Logs.console(settings) !== Logs.console(settings))
    }
}
