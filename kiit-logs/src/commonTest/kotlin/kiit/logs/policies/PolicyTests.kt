package kiit.logs.policies

import kiit.logs.LogEntry
import kiit.logs.LogLevel
import kiit.logs.Logger
import kiit.logs.MemorySink
import kiit.logs.testSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PolicyTests {
    private class Recorded(val stage: LogStage, val entry: LogEntry?)

    private fun handling(records: MutableList<Recorded>) = ErrorPolicy.Handle { stage, _, entry -> records.add(Recorded(stage, entry)) }

    private fun adding(key: String) = Policy { it.copy(fields = it.fields + (key to true)) }

    private fun logger(sink: MemorySink, vararg policies: Policy) = Logger(testSettings().copy(policies = policies.asList()), "L", sink)

    @Test
    fun policies_run_in_list_order() {
        val sink = MemorySink()
        logger(sink, adding("first"), adding("second")).info("x", "call" to 1)
        assertEquals(listOf("call", "first", "second"), sink.entries.single().fields.map { it.first })
    }

    @Test
    fun a_null_stops_the_chain_and_drops_the_entry() {
        var reached = false
        val sink = MemorySink()
        logger(
            sink,
            Policy { null },
            Policy {
                reached = true
                it
            },
        ).info("x")
        assertTrue(sink.entries.isEmpty())
        assertTrue(!reached)
    }

    @Test
    fun filter_keeps_what_the_predicate_accepts() {
        val sink = MemorySink()
        val log = logger(sink, Policy.filter { it.action != "noisy" })
        log.info("noisy")
        log.info("kept")
        assertEquals(listOf("kept"), sink.entries.map { it.action })
    }

    @Test
    fun a_filter_after_the_default_redaction_only_sees_masked_values() {
        val seen = mutableListOf<Any?>()
        val sink = MemorySink()
        logger(
            sink,
            Redaction(),
            Policy {
                seen.addAll(it.fields.map { f -> f.second })
                it
            },
        )
            .info("signup", "email" to "a@b.com", "plan" to "pro")
        assertEquals(listOf<Any?>("***", "pro"), seen)
        assertEquals("***", sink.entries.single().fields.first().second)
    }

    @Test
    fun without_policies_entries_are_delivered_as_they_are() {
        val sink = MemorySink()
        logger(sink).info("signup", "password" to "hunter2")
        assertEquals("hunter2", sink.entries.single().fields.single().second)
    }

    @Test
    fun redaction_returns_the_same_entry_when_no_field_is_sensitive_and_a_copy_otherwise() {
        val clean = LogEntry(level = LogLevel.Info, fields = listOf("plan" to "pro"))
        assertSame(clean, Redaction().apply(clean))

        val sensitive = LogEntry(level = LogLevel.Info, fields = listOf("email" to "a@b.com"))
        val redacted = Redaction().apply(sensitive)
        assertNotSame(sensitive, redacted)
        assertEquals("***", redacted?.fields?.single()?.second)
        assertEquals("a@b.com", sensitive.fields.single().second)
    }

    @Test
    fun a_policy_that_throws_drops_the_entry_and_is_reported_without_it() {
        val records = mutableListOf<Recorded>()
        val sink = MemorySink()
        val settings =
            testSettings().copy(
                policies = listOf(Policy { throw IllegalStateException("redaction bug") }),
                errors = handling(records),
            )
        Logger(settings, "L", sink).info("signup", "password" to "hunter2")
        assertTrue(sink.entries.isEmpty())
        assertEquals(LogStage.Policy, records.single().stage)
        assertNull(records.single().entry)
    }

    @Test
    fun a_policy_that_throws_after_the_redaction_still_never_delivers_raw_fields() {
        val sink = MemorySink()
        val settings =
            testSettings().copy(
                policies = listOf(Redaction(), Policy { throw IllegalStateException("bug") }),
                errors = handling(mutableListOf()),
            )
        Logger(settings, "L", sink).info("signup", "password" to "hunter2")
        assertTrue(sink.entries.isEmpty())
    }
}
