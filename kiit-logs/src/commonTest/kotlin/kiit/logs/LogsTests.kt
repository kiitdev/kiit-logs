package kiit.logs

import kiit.logs.sinks.ConsoleSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LogsTests {
    @Test
    fun sink_uses_the_settings_and_sends_entries_to_the_sink() {
        val sink = MemorySink()
        val settings = testSettings(LogLevel.Info)
        val factory = Logs.sink(settings, sink)
        assertIs<DefaultLogFactory>(factory)
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
    fun console_is_a_sink_factory_with_a_console_sink_and_the_settings() {
        val settings = testSettings()
        val factory = Logs.console(settings)
        assertIs<DefaultLogFactory>(factory)
        assertSame(settings, factory.settings)
        assertIs<ConsoleSink>(factory.provider)
    }

    @Test
    fun getLogger_returns_the_same_logger_for_the_same_name() {
        val factory = Logs.sink(testSettings(), MemorySink())
        assertSame(factory.getLogger("a"), factory.getLogger("a"))
        assertTrue(factory.getLogger("a") !== factory.getLogger("b"))
    }

    @Test
    fun a_level_change_on_the_returned_factory_applies() {
        val sink = MemorySink()
        val factory = Logs.sink(testSettings(LogLevel.Error), sink)
        val log = factory.getLogger("orders")
        log.info("before")
        factory.setLevel(LogLevel.Info)
        log.info("after")
        assertEquals(listOf("after"), sink.entries.map { it.action })
    }

    @Test
    fun each_call_returns_a_new_factory() {
        val sink = MemorySink()
        val settings = testSettings()
        assertTrue(Logs.sink(settings, sink) !== Logs.sink(settings, sink))
    }
}
