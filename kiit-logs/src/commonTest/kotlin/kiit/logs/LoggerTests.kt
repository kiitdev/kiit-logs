package kiit.logs

import kiit.logs.policies.FilterPolicy
import kiit.logs.policies.RedactPolicy
import kiit.logs.sinks.LogSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoggerTests {
    @Test
    fun structured_entry_has_action_fields_origin_scope_and_time() {
        val sink = MemorySink()
        val log = Logger(testSettings().copy(origin = "shop.example.com", scope = "orders"), "OrderService", sink)
        log.info("place", "order_id" to "abc", "total" to 42)

        val entry = sink.entries.single()
        assertEquals("OrderService", entry.name)
        assertEquals(LogLevel.Info, entry.level)
        assertEquals("place", entry.action)
        assertEquals("shop.example.com", entry.origin)
        assertEquals("orders", entry.scope)
        assertEquals(fields("order_id" to "abc", "total" to 42), entry.fields)
        assertEquals(fixedTime, entry.time)
        assertEquals("", entry.msg)
        assertNull(entry.ex)
        assertNull(entry.trace)
    }

    @Test
    fun structured_entry_with_an_exception_uses_its_message_and_keeps_it() {
        val sink = MemorySink()
        val ex = IllegalStateException("card declined")
        Logger(testSettings(), "Payments", sink).error("charge", ex, "order_id" to "abc")

        val entry = sink.entries.single()
        assertEquals("card declined", entry.msg)
        assertEquals(ex, entry.ex)
        assertEquals(fields("order_id" to "abc"), entry.fields)
    }

    @Test
    fun free_text_appends_the_exception_message() {
        val sink = MemorySink()
        val log = Logger(testSettings(), "L", sink)
        log.log(LogLevel.Info, "error check", IllegalStateException("testing exception message"))
        assertEquals("error check\ntesting exception message", sink.entries.single().msg)
    }

    @Test
    fun free_text_with_only_an_exception_uses_its_message() {
        val sink = MemorySink()
        Logger(testSettings(), "L", sink).log(LogLevel.Info, null, IllegalStateException("testing exception message"))
        assertEquals("testing exception message", sink.entries.single().msg)
    }

    @Test
    fun free_text_without_an_exception_is_the_message() {
        val sink = MemorySink()
        Logger(testSettings(), "L", sink).log(LogLevel.Warn, "payment slow")
        val entry = sink.entries.single()
        assertEquals("payment slow", entry.msg)
        assertNull(entry.action)
    }

    @Test
    fun each_level_method_logs_at_its_level() {
        val sink = MemorySink()
        val log = Logger(testSettings(LogLevel.Trace), "L", sink)
        log.logAction(LogLevel.Trace, "t", null, emptyArray())
        log.debug("d")
        log.info("i")
        log.warn("w")
        log.error("e")
        log.fatal("f")
        assertEquals(
            listOf(LogLevel.Trace, LogLevel.Debug, LogLevel.Info, LogLevel.Warn, LogLevel.Error, LogLevel.Fatal),
            sink.entries.map { it.level },
        )
    }

    @Test
    fun only_entries_at_or_above_the_level_are_logged() {
        val all = listOf(LogLevel.Debug, LogLevel.Info, LogLevel.Warn, LogLevel.Error, LogLevel.Fatal)
        all.forEach { min ->
            val sink = MemorySink()
            val log = Logger(testSettings(min), "L", sink)
            log.debug("d")
            log.info("i")
            log.warn("w")
            log.error("e")
            log.fatal("f")
            val expected = all.filter { it.code >= min.code }
            assertEquals(expected, sink.entries.map { it.level }, "minimum level ${min.name}")
        }
    }

    @Test
    fun trace_is_below_debug() {
        assertTrue(LogLevel.Trace < LogLevel.Debug)
        val sink = MemorySink()
        val log = Logger(testSettings(LogLevel.Debug), "L", sink)
        log.logAction(LogLevel.Trace, "t", null, emptyArray())
        assertTrue(sink.entries.isEmpty())
    }

    @Test
    fun off_logs_nothing_and_is_never_enabled() {
        val sink = MemorySink()
        val log = Logger(testSettings(LogLevel.Off), "L", sink)
        log.fatal("f")
        log.log(LogLevel.Fatal, "text")
        assertTrue(sink.entries.isEmpty())
        val levels = listOf(LogLevel.Trace, LogLevel.Debug, LogLevel.Info, LogLevel.Warn, LogLevel.Error, LogLevel.Fatal, LogLevel.Off)
        levels.forEach { assertFalse(log.isEnabled(it), it.name) }
        assertFalse(Logger(testSettings(LogLevel.Trace), "L", sink).isEnabled(LogLevel.Off))
    }

    @Test
    fun a_lazy_message_is_only_built_when_the_level_is_enabled() {
        val sink = MemorySink()
        var calls = 0
        val log = Logger(testSettings(LogLevel.Info), "L", sink)
        log.log(LogLevel.Debug) {
            calls++
            "built"
        }
        assertEquals(0, calls)
        assertTrue(sink.entries.isEmpty())

        log.log(LogLevel.Info) {
            calls++
            "built"
        }
        assertEquals(1, calls)
        assertEquals("built", sink.entries.single().msg)
    }

    @Test
    fun a_lazy_message_with_an_exception_appends_its_message_and_keeps_the_exception() {
        val sink = MemorySink()
        val ex = IllegalStateException("boom")
        val log = Logger(testSettings(LogLevel.Info), "L", sink)
        log.log(LogLevel.Error, ex) { "charge failed" }
        log.log(LogLevel.Error, ex) { "" }
        log.log(LogLevel.Error, null) { "no exception" }
        assertEquals(listOf("charge failed\nboom", "boom", "no exception"), sink.entries.map { it.msg })
        assertEquals(listOf<Throwable?>(ex, ex, null), sink.entries.map { it.ex })
    }

    @Test
    fun a_lazy_message_with_an_exception_is_not_built_when_the_level_is_disabled() {
        var calls = 0
        val sink = MemorySink()
        Logger(testSettings(LogLevel.Error), "L", sink).log(LogLevel.Info, IllegalStateException("boom")) {
            calls++
            "built"
        }
        assertEquals(0, calls)
        assertTrue(sink.entries.isEmpty())
    }

    @Test
    fun the_lazy_and_eager_messages_are_built_the_same_way() {
        val sink = MemorySink()
        val ex = IllegalStateException("boom")
        val log = Logger(testSettings(LogLevel.Info), "L", sink)
        log.log(LogLevel.Error, "charge failed", ex)
        log.log(LogLevel.Error, ex) { "charge failed" }
        assertEquals(sink.entries[0].msg, sink.entries[1].msg)
    }

    @Test
    fun lazy_fields_are_only_built_when_the_level_is_enabled() {
        val sink = MemorySink()
        var calls = 0
        val log = Logger(testSettings(LogLevel.Info), "L", sink)
        log.debug("place") {
            calls++
            fields("total" to 42)
        }
        assertEquals(0, calls)
        assertTrue(sink.entries.isEmpty())

        log.info("place") {
            calls++
            fields("total" to 42)
        }
        assertEquals(1, calls)
        val entry = sink.entries.single()
        assertEquals("place", entry.action)
        assertEquals(fields("total" to 42), entry.fields)
    }

    @Test
    fun lazy_fields_with_an_exception() {
        val sink = MemorySink()
        val ex = IllegalStateException("boom")
        Logger(testSettings(), "L", sink).error("charge", ex) { fields("a" to 1) }
        val entry = sink.entries.single()
        assertEquals(ex, entry.ex)
        assertEquals("boom", entry.msg)
        assertEquals(fields("a" to 1), entry.fields)
    }

    @Test
    fun the_level_can_be_changed_at_runtime() {
        val sink = MemorySink()
        val log = Logger(testSettings(LogLevel.Error), "L", sink)
        log.info("hidden")
        log.settings = log.settings.copy(level = LogLevel.Info)
        log.info("shown")
        assertEquals(listOf("shown"), sink.entries.map { it.action })
        assertEquals(LogLevel.Info, log.level)
    }

    @Test
    fun the_level_constructor_uses_safe_settings_with_that_level() {
        val log = Logger(LogLevel.Info, "L", MemorySink())
        assertEquals(LogLevel.Info, log.level)
        assertEquals(StackTraces.Off, log.settings.stackTraces)
        assertTrue(log.settings.policies.single() is RedactPolicy)
    }

    @Test
    fun bound_fields_come_first_in_order_and_are_redacted() {
        val sink = MemorySink()
        val log = Logger(testSettings(), "L", sink)
        log.with("trace_id" to "t-1").with("password" to "secret").info("place", "order_id" to "abc")

        assertEquals(
            fields("trace_id" to "t-1", "password" to "***", "order_id" to "abc"),
            sink.entries.single().fields,
        )
    }

    @Test
    fun with_does_not_change_the_logger_it_came_from() {
        val sink = MemorySink()
        val log = Logger(testSettings(), "L", sink)
        log.with("trace_id" to "t-1")
        log.info("plain")
        assertTrue(sink.entries.single().fields.isEmpty())
    }

    @Test
    fun a_bound_logger_follows_a_settings_change_on_its_parent() {
        val sink = MemorySink()
        val log = Logger(testSettings(LogLevel.Info), "L", sink)
        val bound = log.with("k" to 1)
        log.settings = log.settings.copy(level = LogLevel.Off)
        bound.error("hidden")
        assertTrue(sink.entries.isEmpty())
        assertEquals(LogLevel.Off, bound.level)

        bound.settings = bound.settings.copy(level = LogLevel.Info)
        log.info("shown")
        assertEquals(1, sink.entries.size)
    }

    @Test
    fun a_bound_logger_keeps_the_name_and_the_raw_of_its_parent() {
        val sink =
            object : LogSink {
                override fun emit(entry: LogEntry) {}

                override fun rawFor(name: String): Any? = "raw:$name"
            }
        val bound = Logger(testSettings(), "com.shop.A", sink).with("k" to 1)
        assertEquals("com.shop.A", bound.name)
        assertEquals("raw:com.shop.A", bound.raw)
    }

    @Test
    fun the_filter_can_drop_entries_and_sees_bound_fields() {
        val sink = MemorySink()
        val settings =
            testSettings().copy(
                policies = listOf(FilterPolicy { entry -> entry.action != "noisy" && entry.fields.none { it.first == "muted" } }),
            )
        val log = Logger(settings, "L", sink)
        log.info("noisy")
        log.info("kept")
        log.with("muted" to true).info("hidden")
        assertEquals(listOf("kept"), sink.entries.map { it.action })
    }

    @Test
    fun no_logger_discards_everything() {
        assertEquals(LogLevel.Off, NoLogger.level)
        assertFalse(NoLogger.isEnabled(LogLevel.Fatal))
        NoLogger.fatal("nothing")
        NoLogger.log(LogLevel.Fatal, "nothing")
        NoLogger.with("k" to 1).fatal("nothing")
    }

    @Test
    fun raw_is_asked_of_the_sink_with_the_logger_name() {
        val sink =
            object : LogSink {
                override fun emit(entry: LogEntry) {}

                override fun rawFor(name: String): Any? = "raw:$name"
            }
        val a = Logger(testSettings(), "A", sink)
        val b = Logger(testSettings(), "B", sink)
        assertEquals("raw:A", a.raw)
        assertEquals("raw:B", b.raw)
        assertEquals("raw:A", a.rawAs<String>())
        assertNull(a.rawAs<Int>())
        assertNull(Logger(testSettings(), "C", MemorySink()).raw)
    }

    @Test
    fun flush_goes_to_the_sink() {
        val sink = MemorySink()
        Logger(testSettings(), "L", sink).flush()
        assertEquals(1, sink.flushed)
    }
}
