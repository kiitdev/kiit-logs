package kiit.logs.policies

import kiit.logs.FailingSink
import kiit.logs.LogEntry
import kiit.logs.LogLevel
import kiit.logs.LogSettings
import kiit.logs.Logger
import kiit.logs.MemorySink
import kiit.logs.factories.SinkLogFactory
import kiit.logs.sinks.CompositeSink
import kiit.logs.testSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ErrorPolicyTests {
    private class Recorded(val stage: LogStage, val error: Exception, val entry: LogEntry?)

    private fun handling(records: MutableList<Recorded>) =
        ErrorPolicy.Handle { stage, error, entry ->
            records.add(Recorded(stage, error, entry))
        }

    private fun settings(policy: ErrorPolicy) = testSettings(LogLevel.Info).copy(errors = policy)

    @Test
    fun propagate_throws_the_error_to_the_caller() {
        val log = Logger(settings(ErrorPolicy.Propagate), "L", FailingSink("down"))
        assertEquals("down", assertFailsWith<IllegalStateException> { log.info("x") }.message)
    }

    @Test
    fun a_handler_is_told_the_stage_the_error_and_the_entry() {
        val records = mutableListOf<Recorded>()
        Logger(settings(handling(records)), "L", FailingSink("down")).info("place")
        val record = records.single()
        assertEquals(LogStage.Sink, record.stage)
        assertEquals("down", record.error.message)
        assertEquals("place", record.entry?.action)
    }

    @Test
    fun a_policy_that_throws_is_reported_without_the_entry_and_the_entry_is_dropped() {
        val records = mutableListOf<Recorded>()
        val sink = MemorySink()
        val policy = Policy { throw IllegalArgumentException("redaction bug") }
        Logger(settings(handling(records)).copy(policies = listOf(policy)), "L", sink).info("secret", "k" to "v")
        assertEquals(LogStage.Policy, records.single().stage)
        assertNull(records.single().entry)
        assertTrue(sink.entries.isEmpty())
    }

    @Test
    fun a_filter_that_throws_is_reported_and_the_entry_is_dropped() {
        val records = mutableListOf<Recorded>()
        val sink = MemorySink()
        val filter = Policy.filter { throw IllegalArgumentException("filter bug") }
        Logger(settings(handling(records)).copy(policies = listOf(filter)), "L", sink).info("dropped")
        assertEquals(LogStage.Policy, records.single().stage)
        assertNull(records.single().entry)
        assertTrue(sink.entries.isEmpty())
    }

    @Test
    fun a_lazy_message_or_fields_that_throw_are_reported_and_the_entry_is_dropped() {
        val records = mutableListOf<Recorded>()
        val sink = MemorySink()
        val log = Logger(settings(handling(records)), "L", sink)
        log.info("place") { throw IllegalArgumentException("fields bug") }
        log.log(LogLevel.Info, "label") { throw IllegalArgumentException("message bug") }
        assertEquals(listOf(LogStage.Build, LogStage.Build), records.map { it.stage })
        assertTrue(sink.entries.isEmpty())
    }

    @Test
    fun a_disabled_level_runs_nothing_and_reports_nothing() {
        val records = mutableListOf<Recorded>()
        val log = Logger(settings(handling(records)), "L", FailingSink())
        log.debug("place") { throw IllegalArgumentException("must not run") }
        log.log(LogLevel.Debug, "label") { throw IllegalArgumentException("must not run") }
        assertTrue(records.isEmpty())
    }

    @Test
    fun a_handler_that_throws_is_ignored() {
        val policy = ErrorPolicy.Handle { _, _, _ -> throw RuntimeException("handler bug") }
        Logger(settings(policy), "L", FailingSink()).info("x")
    }

    @Test
    fun a_handler_that_does_nothing_makes_logging_silent() {
        Logger(settings(ErrorPolicy.Handle { _, _, _ -> }), "L", FailingSink()).info("x")
    }

    @Test
    fun the_default_reports_but_never_throws() {
        val settings = LogSettings.safe().copy(level = LogLevel.Info)
        assertTrue(settings.errors is ErrorPolicy.Handle)
        val log = Logger(settings, "L", FailingSink())
        repeat(5) { log.info("x") }
    }

    @Test
    fun the_printing_handler_can_be_called_past_its_limit() {
        val handler = LogErrorHandler.printing(limit = 2)
        repeat(5) { handler.onError(LogStage.Sink, IllegalStateException("down"), null) }
    }

    @Test
    fun flush_and_close_failures_are_reported_as_lifecycle_errors() {
        val records = mutableListOf<Recorded>()
        val settings = settings(handling(records))
        Logger(settings, "L", FailingSink()).flush()
        val factory = SinkLogFactory(settings, FailingSink())
        factory.flush()
        factory.close()
        assertEquals(listOf(LogStage.Lifecycle, LogStage.Lifecycle, LogStage.Lifecycle), records.map { it.stage })
    }

    @Test
    fun flush_failures_are_thrown_when_the_policy_is_propagate() {
        assertFailsWith<IllegalStateException> { Logger(settings(ErrorPolicy.Propagate), "L", FailingSink()).flush() }
    }

    @Test
    fun a_composite_failure_reaches_the_handler_once_with_the_others_attached() {
        val records = mutableListOf<Recorded>()
        val ok = MemorySink()
        val sink = CompositeSink(FailingSink("one"), FailingSink("two"), ok)
        Logger(settings(handling(records)), "L", sink).info("composite")
        val record = records.single()
        assertEquals("one", record.error.message)
        assertEquals(1, record.error.suppressedExceptions.size)
        assertEquals(1, ok.entries.size)
    }
}
