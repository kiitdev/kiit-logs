package kiit.logs

import kiit.logs.policies.ErrorPolicy
import kiit.logs.policies.Redaction
import kiit.logs.policies.StackTraces
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EntryAndSettingsTests {
    private fun entry(
        msg: String = "",
        action: String? = null,
        scope: String = "",
        fields: List<Pair<String, Any?>> = emptyList()
    ) = LogEntry(name = "L", level = LogLevel.Info, msg = msg, action = action, origin = "shop", scope = scope, fields = fields)

    @Test
    fun text_is_built_from_the_parts_that_are_set() {
        assertEquals(
            "orders place, order_id=abc, total=42",
            entry(action = "place", scope = "orders", fields = fields("order_id" to "abc", "total" to 42)).text,
        )
        assertEquals("place, order_id=abc", entry(action = "place", fields = fields("order_id" to "abc")).text)
        assertEquals("payment slow", entry(msg = "payment slow").text)
        assertEquals("place, declined, order_id=abc", entry(msg = "declined", action = "place", fields = fields("order_id" to "abc")).text)
        assertEquals("a=1, b=2", entry(fields = fields("a" to 1, "b" to 2)).text)
        assertEquals("", entry().text)
    }

    @Test
    fun text_keeps_field_keys_as_written_and_leaves_out_the_origin() {
        val text = entry(action = "place", fields = fields("orderId" to 1)).text
        assertEquals("place, orderId=1", text)
        assertTrue("shop" !in text)
    }

    @Test
    fun safe_settings_are_the_safe_defaults() {
        val settings = LogSettings.safe()
        assertEquals(LogLevel.Error, settings.level)
        assertEquals(StackTraces.Off, settings.stackTraces)
        assertTrue(settings.redaction is Redaction)
        assertTrue(settings.errors is ErrorPolicy.Handle)
        assertEquals("", settings.origin)
        assertEquals("", settings.scope)
        assertTrue(settings.levels.isEmpty())
        assertNull(settings.filter)
        assertEquals(StackTraces.DEFAULT_MAX_LINES, settings.maxTraceLines)
    }

    @Test
    fun safe_settings_take_origin_and_scope() {
        val settings = LogSettings.safe(origin = "shop.example.com", scope = "orders")
        assertEquals("shop.example.com", settings.origin)
        assertEquals("orders", settings.scope)
    }

    @Test
    fun copies_of_settings_share_the_same_error_handler() {
        val settings = LogSettings.safe()
        assertSame(settings.errors, settings.copy(level = LogLevel.Debug).errors)
    }

    @Test
    fun the_default_redaction_covers_the_common_sensitive_keys() {
        val keys = listOf("password", "email", "phone", "token", "secret", "authorization", "cookie", "ssn", "apikey")
        keys.forEach { assertTrue(it in Redaction.defaults, it) }
    }
}
