package kiit.logs

import kotlin.test.Test
import kotlin.test.assertEquals

class RedactionTests {

    private fun masked(redaction: Redaction, vararg keys: String): List<String> =
        redaction.redact(keys.map { it to "value" }).filter { it.second == redaction.replacement }.map { it.first }

    @Test
    fun sensitive_keys_are_masked_by_default() {
        val result = Redaction().redact(fields("email" to "a@b.com", "plan" to "pro", "password" to "x"))
        assertEquals(fields("email" to "***", "plan" to "pro", "password" to "***"), result)
    }

    @Test
    fun keys_are_compared_without_case_or_separators() {
        val keys = arrayOf("api_key", "apiKey", "API-KEY", "Api Key", "api.key")
        assertEquals(keys.toList(), masked(Redaction(), *keys))
    }

    @Test
    fun contains_matches_a_word_inside_a_key() {
        assertEquals(listOf("user_password"), masked(Redaction(), "user_password", "plan"))
    }

    @Test
    fun contains_also_matches_harmless_keys_that_hold_a_sensitive_word() {
        assertEquals(listOf("token_count"), masked(Redaction(), "token_count", "count"))
    }

    @Test
    fun exact_matches_the_whole_key_only() {
        val redaction = Redaction(keys = setOf("pin"), match = KeyMatch.Exact)
        assertEquals(listOf("pin", "PIN"), masked(redaction, "pin", "pinned", "PIN", "my_pin"))
    }

    @Test
    fun suffix_matches_the_end_of_the_key() {
        val redaction = Redaction(keys = setOf("token"), match = KeyMatch.Suffix)
        assertEquals(listOf("access_token", "token"), masked(redaction, "access_token", "token_count", "token"))
    }

    @Test
    fun drop_removes_the_field() {
        val redaction = Redaction(action = RedactAction.Drop)
        assertEquals(fields("plan" to "pro"), redaction.redact(fields("email" to "a@b.com", "plan" to "pro")))
    }

    @Test
    fun the_replacement_can_be_changed() {
        val redaction = Redaction(replacement = "<hidden>")
        assertEquals(fields("email" to "<hidden>"), redaction.redact(fields("email" to "a@b.com")))
    }

    @Test
    fun keys_can_be_added_to_the_defaults() {
        val redaction = Redaction(keys = Redaction.defaults + "account_no")
        assertEquals(listOf("account_no", "email"), masked(redaction, "account_no", "email", "plan"))
    }

    @Test
    fun the_order_of_the_fields_is_kept() {
        val result = Redaction().redact(fields("b" to 1, "email" to "x", "a" to 2))
        assertEquals(listOf("b", "email", "a"), result.map { it.first })
    }

    @Test
    fun a_custom_redactor_replaces_the_default() {
        val sink = MemorySink()
        val redactor = Redactor { fields -> fields.map { (k, v) -> if (v is String && "@" in v) k to "<email>" else k to v } }
        val log = Logger(testSettings().copy(redaction = redactor), "L", sink)
        log.info("signup", "contact" to "a@b.com", "email" to "kept?", "plan" to "pro")
        assertEquals(
            fields("contact" to "<email>", "email" to "kept?", "plan" to "pro"),
            sink.entries.single().fields
        )
    }

    @Test
    fun a_redactor_sees_bound_and_call_fields_together() {
        var seen: List<String> = emptyList()
        val redactor = Redactor { fields ->
            seen = fields.map { it.first }
            fields
        }
        Logger(testSettings().copy(redaction = redactor), "L", MemorySink()).with("a" to 1).info("x", "b" to 2)
        assertEquals(listOf("a", "b"), seen)
    }
}
