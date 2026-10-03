package kiit.logs

import kiit.logs.data.Action
import kiit.logs.data.Event
import kiit.logs.data.Text
import kiit.logs.internal.currentThreadName
import kiit.logs.policies.FilterPolicy
import kiit.logs.policies.RedactPolicy
import kiit.logs.sinks.LogSink
import kiit.logs.sinks.MemorySink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LoggerTests {
    @Test
    fun structured_entry_has_prefix_fields_source_and_time() {
        val sink = MemorySink()
        val log = testLogger(testSettings().copy(source = Source("shop.example.com", "orders")), sink, "OrderService")
        log.info(Action("place", "order_id" to "abc", "total" to 42))

        val entry = sink.entries.single()
        assertEquals("OrderService", entry.name)
        assertEquals(LogLevel.Info, entry.level)
        assertEquals(Prefix("ACTION", "place"), entry.prefix)
        assertEquals("shop.example.com", entry.source.origin)
        assertEquals("orders", entry.source.scope)
        assertEquals(fields("order_id" to "abc", "total" to 42), entry.fields)
        assertEquals(fixedTime, entry.time)
        assertEquals("", entry.msg)
        assertNull(entry.ex)
        assertNull(entry.trace)
    }

    @Test
    fun entry_has_the_name_of_the_thread_that_logged_it() {
        val sink = MemorySink()
        testLogger(testSettings(), sink).info(Action("place"))
        assertEquals(currentThreadName(), sink.entries.single().thread)
    }

    @Test
    fun structured_entry_with_an_exception_keeps_it_apart_from_the_msg() {
        val sink = MemorySink()
        val ex = IllegalStateException("card declined")
        testLogger(testSettings(), sink, "Payments").error(Action("charge", "order_id" to "abc", ex = ex))

        val entry = sink.entries.single()
        assertEquals("", entry.msg)
        assertEquals(ex, entry.ex)
        assertEquals(fields("order_id" to "abc"), entry.fields)
        assertEquals("ACTION: charge, msg=\"card declined\", order_id=abc, logger=Payments", entry.text)
    }

    @Test
    fun free_text_keeps_the_exception_apart_and_the_text_adds_its_message() {
        val sink = MemorySink()
        val log = testLogger(testSettings(), sink, "L")
        log.log(LogLevel.Info, Text("error check", IllegalStateException("testing exception message")))
        val entry = sink.entries.single()
        assertEquals("error check", entry.msg)
        assertEquals("testing exception message", entry.ex?.message)
        assertEquals("error check: testing exception message, logger=L", entry.text)
    }

    @Test
    fun free_text_with_only_an_exception_has_no_msg_and_the_text_uses_its_message() {
        val sink = MemorySink()
        testLogger(testSettings(), sink, "L").log(LogLevel.Info, Text("", IllegalStateException("testing exception message")))
        val entry = sink.entries.single()
        assertEquals("", entry.msg)
        assertEquals("testing exception message, logger=L", entry.text)
    }

    @Test
    fun free_text_without_an_exception_is_the_message() {
        val sink = MemorySink()
        testLogger(testSettings(), sink, "L").log(LogLevel.Warn, Text("payment slow"))
        val entry = sink.entries.single()
        assertEquals("payment slow", entry.msg)
        assertNull(entry.prefix)
    }

    @Test
    fun each_level_method_logs_at_its_level() {
        val sink = MemorySink()
        val log = testLogger(testSettings(LogLevel.Verbose), sink, "L")
        log.verbose(Action("v"))
        log.debug(Action("d"))
        log.info(Action("i"))
        log.warn(Action("w"))
        log.error(Action("e"))
        log.fatal(Action("f"))
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
            log.debug(Action("d"))
            log.info(Action("i"))
            log.warn(Action("w"))
            log.error(Action("e"))
            log.fatal(Action("f"))
            val expected = all.filter { it.code >= min.code }
            assertEquals(expected, sink.entries.map { it.level }, "minimum level ${min.name}")
        }
    }

    @Test
    fun verbose_is_below_debug() {
        assertTrue(LogLevel.Verbose < LogLevel.Debug)
        val sink = MemorySink()
        val log = testLogger(testSettings(LogLevel.Debug), sink, "L")
        log.verbose(Action("v", "k" to 1))
        log.verbose(Action("v", "k" to 1, ex = IllegalStateException("boom")))
        log.verbose { Action("v", "k" to 1) }
        assertTrue(sink.entries.isEmpty())
    }

    @Test
    fun verbose_has_the_same_forms_as_the_other_levels() {
        val sink = MemorySink()
        val ex = IllegalStateException("boom")
        val log = testLogger(testSettings(LogLevel.Verbose), sink, "L")
        log.verbose(Action("plain", "k" to 1))
        log.verbose(Action("with_exception", "k" to 2, ex = ex))
        log.verbose { Action("lazy", "k" to 3, ex = ex) }
        assertEquals(listOf("plain", "with_exception", "lazy"), sink.entries.map { it.prefix?.value })
        assertEquals(listOf(LogLevel.Verbose, LogLevel.Verbose, LogLevel.Verbose), sink.entries.map { it.level })
        assertEquals(listOf<Throwable?>(null, ex, ex), sink.entries.map { it.ex })
        assertEquals(listOf(1, 2, 3), sink.entries.map { it.fields.single().second })
    }

    @Test
    fun lazy_verbose_fields_are_only_built_when_verbose_is_enabled() {
        var calls = 0
        val sink = MemorySink()
        testLogger(testSettings(LogLevel.Debug), sink, "L").verbose {
            calls++
            Action("v", "k" to 1)
        }
        assertEquals(0, calls)
        testLogger(testSettings(LogLevel.Verbose), sink, "L").verbose {
            calls++
            Action("v", "k" to 1)
        }
        assertEquals(1, calls)
        assertEquals(1, sink.entries.size)
    }

    @Test
    fun off_logs_nothing_and_is_never_enabled() {
        val sink = MemorySink()
        val log = testLogger(testSettings(LogLevel.Off), sink, "L")
        log.fatal(Action("f"))
        log.log(LogLevel.Fatal, Text("text"))
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
            Text("built")
        }
        assertEquals(0, calls)
        assertTrue(sink.entries.isEmpty())

        log.log(LogLevel.Info) {
            calls++
            Text("built")
        }
        assertEquals(1, calls)
        assertEquals("built", sink.entries.single().msg)
    }

    @Test
    fun a_lazy_message_with_an_exception_keeps_both_and_the_text_adds_the_exception_message() {
        val sink = MemorySink()
        val ex = IllegalStateException("boom")
        val log = testLogger(testSettings(LogLevel.Info), sink, "L")
        log.log(LogLevel.Error) { Text("charge failed", ex) }
        log.log(LogLevel.Error) { Text("", ex) }
        log.log(LogLevel.Error) { Text("no exception") }
        assertEquals(listOf("charge failed", "", "no exception"), sink.entries.map { it.msg })
        assertEquals(listOf("charge failed: boom, logger=L", "boom, logger=L", "no exception, logger=L"), sink.entries.map { it.text })
        assertEquals(listOf<Throwable?>(ex, ex, null), sink.entries.map { it.ex })
    }

    @Test
    fun a_lazy_message_with_an_exception_is_not_built_when_the_level_is_disabled() {
        var calls = 0
        val sink = MemorySink()
        testLogger(testSettings(LogLevel.Error), sink, "L").log(LogLevel.Info) {
            calls++
            Text("built", IllegalStateException("boom"))
        }
        assertEquals(0, calls)
        assertTrue(sink.entries.isEmpty())
    }

    @Test
    fun the_lazy_and_eager_messages_are_built_the_same_way() {
        val sink = MemorySink()
        val ex = IllegalStateException("boom")
        val log = testLogger(testSettings(LogLevel.Info), sink, "L")
        log.log(LogLevel.Error, Text("charge failed", ex))
        log.log(LogLevel.Error) { Text("charge failed", ex) }
        assertEquals(sink.entries[0].msg, sink.entries[1].msg)
        assertEquals(sink.entries[0].text, sink.entries[1].text)
    }

    @Test
    fun one_call_uses_one_snapshot_of_the_settings() {
        val sink = MemorySink()
        val log = testLogger(testSettings().copy(source = Source("before")), sink, "L")
        // The lambda runs during the call, so this changes the settings in the middle of it
        log.log(LogLevel.Info) {
            log.settings = log.settings.copy(source = Source("after"))
            Text("first")
        }
        log.log(LogLevel.Info) { Text("second") }
        assertEquals(listOf("before", "after"), sink.entries.map { it.source.origin })
    }

    @Test
    fun a_settings_change_during_a_call_applies_to_the_next_call() {
        val sink = MemorySink()
        val log = testLogger(testSettings(), sink, "L")
        log.log(LogLevel.Info) {
            log.settings = log.settings.copy(policies = listOf(FilterPolicy { false }))
            Text("first")
        }
        log.log(LogLevel.Info) { Text("second") }
        assertEquals(listOf("first"), sink.entries.map { it.msg })
    }

    @Test
    fun lazy_fields_are_only_built_when_the_level_is_enabled() {
        val sink = MemorySink()
        var calls = 0
        val log = testLogger(testSettings(LogLevel.Info), sink, "L")
        log.debug {
            calls++
            Action("place", "total" to 42)
        }
        assertEquals(0, calls)
        assertTrue(sink.entries.isEmpty())

        log.info {
            calls++
            Action("place", "total" to 42)
        }
        assertEquals(1, calls)
        val entry = sink.entries.single()
        assertEquals("place", entry.prefix?.value)
        assertEquals(fields("total" to 42), entry.fields)
    }

    @Test
    fun lazy_fields_with_an_exception() {
        val sink = MemorySink()
        val ex = IllegalStateException("boom")
        testLogger(testSettings(), sink, "L").error { Action("charge", "a" to 1, ex = ex) }
        val entry = sink.entries.single()
        assertEquals(ex, entry.ex)
        assertEquals("", entry.msg)
        assertEquals(fields("a" to 1), entry.fields)
        assertEquals("ACTION: charge, msg=\"boom\", a=1, logger=L", entry.text)
    }

    @Test
    fun the_level_can_be_changed_at_runtime() {
        val sink = MemorySink()
        val log = testLogger(testSettings(LogLevel.Error), sink, "L")
        log.info(Action("hidden"))
        log.settings = log.settings.copy(level = LogLevel.Info)
        log.info(Action("shown"))
        assertEquals(listOf("shown"), sink.entries.map { it.prefix?.value })
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
        log.with("trace_id" to "t-1").with("password" to "secret").info(Action("place", "order_id" to "abc"))

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
        log.info(Action("plain"))
        assertTrue(sink.entries.single().fields.isEmpty())
    }

    @Test
    fun a_bound_logger_follows_a_settings_change_on_its_parent() {
        val sink = MemorySink()
        val log = testLogger(testSettings(LogLevel.Info), sink, "L")
        val bound = log.with("k" to 1)
        log.settings = log.settings.copy(level = LogLevel.Off)
        bound.error(Action("hidden"))
        assertTrue(sink.entries.isEmpty())
        assertEquals(LogLevel.Off, bound.level)

        bound.settings = bound.settings.copy(level = LogLevel.Info)
        log.info(Action("shown"))
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
                policies = listOf(FilterPolicy { entry -> entry.prefix?.value != "noisy" && entry.fields.none { it.first == "muted" } }),
            )
        val log = testLogger(settings, sink, "L")
        log.info(Action("noisy"))
        log.info(Action("kept"))
        log.with("muted" to true).info(Action("hidden"))
        assertEquals(listOf("kept"), sink.entries.map { it.prefix?.value })
    }

    @Test
    fun no_logger_discards_everything() {
        assertEquals(LogLevel.Off, NoLogger.level)
        assertFalse(NoLogger.isEnabled(LogLevel.Fatal))
        NoLogger.fatal(Action("nothing"))
        NoLogger.log(LogLevel.Fatal, Text("nothing"))
        NoLogger.with("k" to 1).fatal(Action("nothing"))
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

    @Test
    fun an_event_has_the_event_prefix_and_its_fields() {
        val sink = MemorySink()
        testLogger(testSettings(), sink, "L").info(Event("order_placed", "order_id" to "abc"))
        val entry = sink.entries.single()
        assertEquals(Prefix("EVENT", "order_placed"), entry.prefix)
        assertEquals(fields("order_id" to "abc"), entry.fields)
        assertEquals("EVENT: order_placed, order_id=abc, logger=L", entry.text)
    }

    @Test
    fun an_action_with_a_msg_and_an_exception_keeps_them_apart() {
        val sink = MemorySink()
        val ex = IllegalStateException("boom")
        testLogger(testSettings(), sink, "L").warn(Action("place_order", msg = "low stock", ex = ex))
        val entry = sink.entries.single()
        assertEquals(Prefix("ACTION", "place_order"), entry.prefix)
        assertEquals("low stock", entry.msg)
        assertEquals(ex, entry.ex)
        assertEquals("ACTION: place_order, msg=\"low stock: boom\", logger=L", entry.text)
    }

    @Test
    fun a_custom_log_data_is_logged_like_the_built_ins() {
        val job =
            object : LogData {
                override val prefix = Prefix("JOB", "nightly")
                override val msg = "done"
                override val fields = fields("run" to 7)
            }
        val sink = MemorySink()
        val log = testLogger(testSettings(), sink, "L").with("trace_id" to "t-1")
        log.info(job)
        log.info { job }
        assertEquals(listOf(Prefix("JOB", "nightly"), Prefix("JOB", "nightly")), sink.entries.map { it.prefix })
        assertEquals(listOf("done", "done"), sink.entries.map { it.msg })
        assertEquals(fields("trace_id" to "t-1", "run" to 7), sink.entries.first().fields)
    }

    @Test
    fun each_level_method_logs_at_its_level_in_both_forms() {
        val sink = MemorySink()
        val log = testLogger(testSettings(LogLevel.Verbose), sink, "L")
        log.verbose(Action("a"))
        log.debug(Action("a"))
        log.info(Action("a"))
        log.warn(Action("a"))
        log.error(Action("a"))
        log.fatal(Action("a"))
        log.verbose { Action("a") }
        log.debug { Action("a") }
        log.info { Action("a") }
        log.warn { Action("a") }
        log.error { Action("a") }
        log.fatal { Action("a") }
        val levels = listOf(LogLevel.Verbose, LogLevel.Debug, LogLevel.Info, LogLevel.Warn, LogLevel.Error, LogLevel.Fatal)
        assertEquals(levels + levels, sink.entries.map { it.level })
    }

    @Test
    fun the_general_method_takes_the_level_in_both_forms() {
        val sink = MemorySink()
        val log = testLogger(testSettings(), sink, "L")
        log.log(LogLevel.Warn, Event("order_placed"))
        log.log(LogLevel.Error) { Event("order_failed") }
        assertEquals(listOf(LogLevel.Warn, LogLevel.Error), sink.entries.map { it.level })
        assertEquals(listOf("order_placed", "order_failed"), sink.entries.map { it.prefix?.value })
    }
}
