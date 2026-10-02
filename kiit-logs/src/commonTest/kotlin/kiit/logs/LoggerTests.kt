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
        val log = testLogger(testSettings().copy(origin = "shop.example.com", scope = "orders"), sink, "OrderService")
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
        testLogger(testSettings(), sink, "Payments").error("charge", ex, "order_id" to "abc")

        val entry = sink.entries.single()
        assertEquals("card declined", entry.msg)
        assertEquals(ex, entry.ex)
        assertEquals(fields("order_id" to "abc"), entry.fields)
    }

    @Test
    fun free_text_appends_the_exception_message() {
        val sink = MemorySink()
        val log = testLogger(testSettings(), sink, "L")
        log.log(LogLevel.Info, "error check", IllegalStateException("testing exception message"))
        assertEquals("error check: testing exception message", sink.entries.single().msg)
    }

    @Test
    fun free_text_with_only_an_exception_uses_its_message() {
        val sink = MemorySink()
        testLogger(testSettings(), sink, "L").log(LogLevel.Info, null, IllegalStateException("testing exception message"))
        assertEquals("testing exception message", sink.entries.single().msg)
    }

    @Test
    fun free_text_without_an_exception_is_the_message() {
        val sink = MemorySink()
        testLogger(testSettings(), sink, "L").log(LogLevel.Warn, "payment slow")
        val entry = sink.entries.single()
        assertEquals("payment slow", entry.msg)
        assertNull(entry.action)
    }

    @Test
    fun each_level_method_logs_at_its_level() {
        val sink = MemorySink()
        val log = testLogger(testSettings(LogLevel.Verbose), sink, "L")
        log.verbose("v")
        log.debug("d")
        log.info("i")
        log.warn("w")
        log.error("e")
        log.fatal("f")
        assertEquals(
            listOf(LogLevel.Verbose, LogLevel.Debug, LogLevel.Info, LogLevel.Warn, LogLevel.Error, LogLevel.Fatal),
            sink.entries.map { it.level },
        )
    }

    @Test
    fun only_entries_at_or_above_the_level_are_logged() {
        val all = listOf(LogLevel.Debug, LogLevel.Info, LogLevel.Warn, LogLevel.Error, LogLevel.Fatal)
        all.forEach { min ->
            val sink = MemorySink()
            val log = testLogger(testSettings(min), sink, "L")
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
    fun verbose_is_below_debug() {
        assertTrue(LogLevel.Verbose < LogLevel.Debug)
        val sink = MemorySink()
        val log = testLogger(testSettings(LogLevel.Debug), sink, "L")
        log.verbose("v", "k" to 1)
        log.verbose("v", IllegalStateException("boom"), "k" to 1)
        log.verbose("v") { listOf("k" to 1) }
        assertTrue(sink.entries.isEmpty())
    }

    @Test
    fun verbose_has_the_same_three_forms_as_the_other_levels() {
        val sink = MemorySink()
        val ex = IllegalStateException("boom")
        val log = testLogger(testSettings(LogLevel.Verbose), sink, "L")
        log.verbose("plain", "k" to 1)
        log.verbose("with_exception", ex, "k" to 2)
        log.verbose("lazy", ex) { listOf("k" to 3) }
        assertEquals(listOf("plain", "with_exception", "lazy"), sink.entries.map { it.action })
        assertEquals(listOf(LogLevel.Verbose, LogLevel.Verbose, LogLevel.Verbose), sink.entries.map { it.level })
        assertEquals(listOf<Throwable?>(null, ex, ex), sink.entries.map { it.ex })
        assertEquals(listOf(1, 2, 3), sink.entries.map { it.fields.single().second })
    }

    @Test
    fun lazy_verbose_fields_are_only_built_when_verbose_is_enabled() {
        var calls = 0
        val sink = MemorySink()
        testLogger(testSettings(LogLevel.Debug), sink, "L").verbose("v") {
            calls++
            listOf("k" to 1)
        }
        assertEquals(0, calls)
        testLogger(testSettings(LogLevel.Verbose), sink, "L").verbose("v") {
            calls++
            listOf("k" to 1)
        }
        assertEquals(1, calls)
        assertEquals(1, sink.entries.size)
    }

    @Test
    fun off_logs_nothing_and_is_never_enabled() {
        val sink = MemorySink()
        val log = testLogger(testSettings(LogLevel.Off), sink, "L")
        log.fatal("f")
        log.log(LogLevel.Fatal, "text")
        assertTrue(sink.entries.isEmpty())
        val levels = listOf(LogLevel.Verbose, LogLevel.Debug, LogLevel.Info, LogLevel.Warn, LogLevel.Error, LogLevel.Fatal, LogLevel.Off)
        levels.forEach { assertFalse(log.isEnabled(it), it.name) }
        assertFalse(testLogger(testSettings(LogLevel.Verbose), sink, "L").isEnabled(LogLevel.Off))
    }

    @Test
    fun a_lazy_message_is_only_built_when_the_level_is_enabled() {
        val sink = MemorySink()
        var calls = 0
        val log = testLogger(testSettings(LogLevel.Info), sink, "L")
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
        val log = testLogger(testSettings(LogLevel.Info), sink, "L")
        log.log(LogLevel.Error, ex) { "charge failed" }
        log.log(LogLevel.Error, ex) { "" }
        log.log(LogLevel.Error, null) { "no exception" }
        assertEquals(listOf("charge failed: boom", "boom", "no exception"), sink.entries.map { it.msg })
        assertEquals(listOf<Throwable?>(ex, ex, null), sink.entries.map { it.ex })
    }

    @Test
    fun a_lazy_message_with_an_exception_is_not_built_when_the_level_is_disabled() {
        var calls = 0
        val sink = MemorySink()
        testLogger(testSettings(LogLevel.Error), sink, "L").log(LogLevel.Info, IllegalStateException("boom")) {
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
        val log = testLogger(testSettings(LogLevel.Info), sink, "L")
        log.log(LogLevel.Error, "charge failed", ex)
        log.log(LogLevel.Error, ex) { "charge failed" }
        assertEquals(sink.entries[0].msg, sink.entries[1].msg)
    }

    @Test
    fun one_call_uses_one_snapshot_of_the_settings() {
        val sink = MemorySink()
        val log = testLogger(testSettings().copy(origin = "before"), sink, "L")
        // The lambda runs during the call, so this changes the settings in the middle of it
        log.log(LogLevel.Info) {
            log.settings = log.settings.copy(origin = "after")
            "first"
        }
        log.log(LogLevel.Info) { "second" }
        assertEquals(listOf("before", "after"), sink.entries.map { it.origin })
    }

    @Test
    fun a_settings_change_during_a_call_applies_to_the_next_call() {
        val sink = MemorySink()
        val log = testLogger(testSettings(), sink, "L")
        log.log(LogLevel.Info) {
            log.settings = log.settings.copy(policies = listOf(FilterPolicy { false }))
            "first"
        }
        log.log(LogLevel.Info) { "second" }
        assertEquals(listOf("first"), sink.entries.map { it.msg })
    }

    @Test
    fun lazy_fields_are_only_built_when_the_level_is_enabled() {
        val sink = MemorySink()
        var calls = 0
        val log = testLogger(testSettings(LogLevel.Info), sink, "L")
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
        testLogger(testSettings(), sink, "L").error("charge", ex) { fields("a" to 1) }
        val entry = sink.entries.single()
        assertEquals(ex, entry.ex)
        assertEquals("boom", entry.msg)
        assertEquals(fields("a" to 1), entry.fields)
    }

    @Test
    fun the_level_can_be_changed_at_runtime() {
        val sink = MemorySink()
        val log = testLogger(testSettings(LogLevel.Error), sink, "L")
        log.info("hidden")
        log.settings = log.settings.copy(level = LogLevel.Info)
        log.info("shown")
        assertEquals(listOf("shown"), sink.entries.map { it.action })
        assertEquals(LogLevel.Info, log.level)
    }

    @Test
    fun the_level_constructor_uses_safe_settings_with_that_level() {
        val log = testLogger(testSettings(LogLevel.Info), MemorySink(), "L")
        assertEquals(LogLevel.Info, log.level)
        assertEquals(StackTraces.Off, log.settings.stackTraces)
        assertTrue(log.settings.policies.single() is RedactPolicy)
    }

    @Test
    fun bound_fields_come_first_in_order_and_are_redacted() {
        val sink = MemorySink()
        val log = testLogger(testSettings(), sink, "L")
        log.with("trace_id" to "t-1").with("password" to "secret").info("place", "order_id" to "abc")

        assertEquals(
            fields("trace_id" to "t-1", "password" to "***", "order_id" to "abc"),
            sink.entries.single().fields,
        )
    }

    @Test
    fun with_does_not_change_the_logger_it_came_from() {
        val sink = MemorySink()
        val log = testLogger(testSettings(), sink, "L")
        log.with("trace_id" to "t-1")
        log.info("plain")
        assertTrue(sink.entries.single().fields.isEmpty())
    }

    @Test
    fun a_bound_logger_follows_a_settings_change_on_its_parent() {
        val sink = MemorySink()
        val log = testLogger(testSettings(LogLevel.Info), sink, "L")
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
        val bound = testLogger(testSettings(), sink, "com.shop.A").with("k" to 1)
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
        val log = testLogger(settings, sink, "L")
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
        val a = testLogger(testSettings(), sink, "A")
        val b = testLogger(testSettings(), sink, "B")
        assertEquals("raw:A", a.raw)
        assertEquals("raw:B", b.raw)
        assertEquals("raw:A", a.rawAs<String>())
        assertNull(a.rawAs<Int>())
        assertNull(testLogger(testSettings(), MemorySink(), "C").raw)
    }

    @Test
    fun flush_goes_to_the_sink() {
        val sink = MemorySink()
        testLogger(testSettings(), sink, "L").flush()
        assertEquals(1, sink.flushed)
    }
}
