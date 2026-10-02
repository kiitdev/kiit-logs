package kiit.logs.policies

import kiit.logs.MemorySink
import kiit.logs.fields
import kiit.logs.testLogger
import kiit.logs.testSettings
import kotlin.test.Test
import kotlin.test.assertEquals

class RedactPolicyTests {
    private fun masked(redaction: RedactPolicy, vararg keys: String): List<String> =
        redaction.redact(keys.map { it to "value" }).filter { it.second == redaction.replacement }.map { it.first }

    @Test
    fun sensitive_keys_are_masked_by_default() {
        val result = RedactPolicy().redact(fields("email" to "a@b.com", "plan" to "pro", "password" to "x"))
        assertEquals(fields("email" to "***", "plan" to "pro", "password" to "***"), result)
    }

    @Test
    fun keys_are_compared_without_case_or_separators() {
        val keys = arrayOf("api_key", "apiKey", "API-KEY", "Api Key", "api.key")
        assertEquals(keys.toList(), masked(RedactPolicy(), *keys))
    }

    @Test
    fun contains_matches_a_word_inside_a_key() {
        assertEquals(listOf("user_password"), masked(RedactPolicy(), "user_password", "plan"))
    }

    @Test
    fun contains_also_matches_harmless_keys_that_hold_a_sensitive_word() {
        assertEquals(listOf("token_count"), masked(RedactPolicy(), "token_count", "count"))
    }

    @Test
    fun exact_matches_the_whole_key_only() {
        val redaction = RedactPolicy(keys = setOf("pin"), match = KeyMatch.Exact)
        assertEquals(listOf("pin", "PIN"), masked(redaction, "pin", "pinned", "PIN", "my_pin"))
    }

    @Test
    fun suffix_matches_the_end_of_the_key() {
        val redaction = RedactPolicy(keys = setOf("token"), match = KeyMatch.Suffix)
        assertEquals(listOf("access_token", "token"), masked(redaction, "access_token", "token_count", "token"))
    }

    @Test
    fun drop_removes_the_field() {
        val redaction = RedactPolicy(action = RedactAction.Drop)
        assertEquals(fields("plan" to "pro"), redaction.redact(fields("email" to "a@b.com", "plan" to "pro")))
    }

    @Test
    fun the_replacement_can_be_changed() {
        val redaction = RedactPolicy(replacement = "<hidden>")
        assertEquals(fields("email" to "<hidden>"), redaction.redact(fields("email" to "a@b.com")))
    }

    @Test
    fun keys_can_be_added_to_the_defaults() {
        val redaction = RedactPolicy(keys = RedactPolicy.defaults + "account_no")
        assertEquals(listOf("account_no", "email"), masked(redaction, "account_no", "email", "plan"))
    }

    @Test
    fun the_order_of_the_fields_is_kept() {
        val result = RedactPolicy().redact(fields("b" to 1, "email" to "x", "a" to 2))
        assertEquals(listOf("b", "email", "a"), result.map { it.first })
    }

    @Test
    fun a_custom_policy_replaces_the_default_redaction() {
        val sink = MemorySink()
        val policy =
            Policy { entry ->
                entry.copy(fields = entry.fields.map { (k, v) -> if (v is String && "@" in v) k to "<email>" else k to v })
            }
        val log = testLogger(testSettings().copy(policies = listOf(policy)), sink, "L")
        log.info("signup", "contact" to "a@b.com", "email" to "kept?", "plan" to "pro")
        assertEquals(
            fields("contact" to "<email>", "email" to "kept?", "plan" to "pro"),
            sink.entries.single().fields,
        )
    }

    @Test
    fun a_policy_sees_bound_and_call_fields_together() {
        var seen: List<String> = emptyList()
        val policy =
            Policy { entry ->
                seen = entry.fields.map { it.first }
                entry
            }
        testLogger(testSettings().copy(policies = listOf(policy)), MemorySink(), "L").with("a" to 1).info("x", "b" to 2)
        assertEquals(listOf("a", "b"), seen)
    }
}
