package kiit.logs.policies

import kiit.logs.FailingSink
import kiit.logs.LogEntry
import kiit.logs.LogLevel
import kiit.logs.LogSettings
import kiit.logs.Logs
import kiit.logs.data.Action
import kiit.logs.sinks.CompositeSink
import kiit.logs.sinks.MemorySink
import kiit.logs.testLogger
import kiit.logs.testSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ErrorHandlerTests {
    private class Recorded(val stage: ErrorHandler.Stage, val error: Exception, val entry: LogEntry?)

    private fun handling(records: MutableList<Recorded>) =
        ErrorHandler { stage, error, entry ->
            records.add(Recorded(stage, error, entry))
        }

    private fun settings(handler: ErrorHandler) = testSettings(LogLevel.Info).copy(errors = handler)

    @Test
    fun propagate_throws_the_error_to_the_caller() {
        val log = testLogger(settings(ErrorHandler.Throw), FailingSink("down"), "L")
        assertEquals("down", assertFailsWith<IllegalStateException> { log.info(Action("x")) }.message)
    }

    @Test
    fun a_handler_is_told_the_stage_the_error_and_the_entry() {
        val records = mutableListOf<Recorded>()
        testLogger(settings(handling(records)), FailingSink("down"), "L").info(Action("place"))
        val record = records.single()
        assertEquals(ErrorHandler.Stage.Sink, record.stage)
        assertEquals("down", record.error.message)
        assertEquals("place", record.entry?.prefix?.value)
    }

    @Test
    fun a_policy_that_throws_is_reported_without_the_entry_and_the_entry_is_dropped() {
        val records = mutableListOf<Recorded>()
        val sink = MemorySink()
        val policy = Policy { throw IllegalArgumentException("redaction bug") }
        testLogger(settings(handling(records)).copy(policies = listOf(policy)), sink, "L").info(Action("secret", "k" to "v"))
        assertEquals(ErrorHandler.Stage.Policy, records.single().stage)
        assertNull(records.single().entry)
        assertTrue(sink.entries.isEmpty())
    }

    @Test
    fun a_filter_that_throws_is_reported_and_the_entry_is_dropped() {
        val records = mutableListOf<Recorded>()
        val sink = MemorySink()
        val filter = FilterPolicy { throw IllegalArgumentException("filter bug") }
        testLogger(settings(handling(records)).copy(policies = listOf(filter)), sink, "L").info(Action("dropped"))
        assertEquals(ErrorHandler.Stage.Policy, records.single().stage)
        assertNull(records.single().entry)
        assertTrue(sink.entries.isEmpty())
    }

    @Test
    fun a_lazy_message_or_fields_that_throw_are_reported_and_the_entry_is_dropped() {
        val records = mutableListOf<Recorded>()
        val sink = MemorySink()
        val log = testLogger(settings(handling(records)), sink, "L")
        log.info { throw IllegalArgumentException("fields bug") }
        log.log(LogLevel.Info) { throw IllegalArgumentException("message bug") }
        assertEquals(listOf(ErrorHandler.Stage.Build, ErrorHandler.Stage.Build), records.map { it.stage })
        assertTrue(sink.entries.isEmpty())
    }

    @Test
    fun a_disabled_level_runs_nothing_and_reports_nothing() {
        val records = mutableListOf<Recorded>()
        val log = testLogger(settings(handling(records)), FailingSink(), "L")
        log.debug { throw IllegalArgumentException("must not run") }
        log.log(LogLevel.Debug) { throw IllegalArgumentException("must not run") }
        assertTrue(records.isEmpty())
    }

    @Test
    fun a_handler_that_throws_is_ignored() {
        val handler = ErrorHandler { _, _, _ -> throw RuntimeException("handler bug") }
        testLogger(settings(handler), FailingSink(), "L").info(Action("x"))
    }

    @Test
    fun a_handler_that_does_nothing_makes_logging_silent() {
        testLogger(settings(ErrorHandler { _, _, _ -> }), FailingSink(), "L").info(Action("x"))
    }

    @Test
    fun the_default_reports_but_never_throws() {
        val settings = LogSettings.safe().copy(level = LogLevel.Info)
        assertTrue(settings.errors !== ErrorHandler.Throw)
        val log = testLogger(settings, FailingSink(), "L")
        repeat(5) { log.info(Action("x")) }
    }

    @Test
    fun the_printing_handler_can_be_called_past_its_limit() {
        val handler = ErrorHandler.printing(limit = 2)
        repeat(5) { handler.onError(ErrorHandler.Stage.Sink, IllegalStateException("down"), null) }
    }

    @Test
    fun flush_and_close_failures_are_reported_as_lifecycle_errors() {
        val records = mutableListOf<Recorded>()
        val settings = settings(handling(records))
        testLogger(settings, FailingSink(), "L").flush()
        val factory = Logs(settings, FailingSink())
        factory.flush()
        factory.close()
        assertEquals(
            listOf(ErrorHandler.Stage.Lifecycle, ErrorHandler.Stage.Lifecycle, ErrorHandler.Stage.Lifecycle),
            records.map { it.stage },
        )
    }

    @Test
    fun flush_failures_are_thrown_when_the_policy_is_propagate() {
        assertFailsWith<IllegalStateException> { testLogger(settings(ErrorHandler.Throw), FailingSink(), "L").flush() }
    }

    @Test
    fun a_composite_failure_reaches_the_handler_once_with_the_others_attached() {
        val records = mutableListOf<Recorded>()
        val ok = MemorySink()
        val sink = CompositeSink(FailingSink("one"), FailingSink("two"), ok)
        testLogger(settings(handling(records)), sink, "L").info(Action("composite"))
        val record = records.single()
        assertEquals("one", record.error.message)
        assertEquals(1, record.error.suppressedExceptions.size)
        assertEquals(1, ok.entries.size)
    }
}
