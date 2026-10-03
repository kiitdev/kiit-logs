package kiit.logs

import kiit.logs.policies.ErrorHandler
import kiit.logs.policies.RedactPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class EntryAndSettingsTests {
    private fun entry(
        msg: String = "",
        prefix: Prefix? = null,
        name: String = "L",
        fields: List<Pair<String, Any?>> = emptyList(),
        ex: Throwable? = null
    ) = LogEntry(
        name = name,
        level = LogLevel.Info,
        msg = msg,
        ex = ex,
        prefix = prefix,
        source = Source("shop", "orders"),
        fields = fields,
    )

    private val action = Prefix("ACTION", "place_order")

    @Test
    fun text_for_an_action_is_the_prefix_then_the_fields_then_the_logger() {
        assertEquals(
            "ACTION: place_order, order_id=abc, total=42, logger=L",
            entry(prefix = action, fields = fields("order_id" to "abc", "total" to 42)).text,
        )
        assertEquals("ACTION: place_order, logger=L", entry(prefix = action).text)
    }

    @Test
    fun text_for_an_event_is_not_padded() {
        val event = Prefix("EVENT", "order_placed")
        assertEquals("EVENT: order_placed, order_id=abc, logger=L", entry(prefix = event, fields = fields("order_id" to "abc")).text)
    }

    @Test
    fun text_without_a_prefix_starts_with_the_message_as_it_is() {
        assertEquals("payment slow, logger=L", entry(msg = "payment slow").text)
        assertEquals("payment slow, a=1, logger=L", entry(msg = "payment slow", fields = fields("a" to 1)).text)
    }

    @Test
    fun text_with_a_prefix_has_the_message_as_a_quoted_msg_after_the_prefix() {
        assertEquals(
            "ACTION: place_order, msg=\"low stock\", order_id=abc, logger=L",
            entry(msg = "low stock", prefix = action, fields = fields("order_id" to "abc")).text,
        )
    }

    @Test
    fun text_adds_the_exception_message_after_the_msg_and_the_entry_keeps_them_apart() {
        val ex = IllegalStateException("card declined")
        val failed = entry(msg = "payment failed", ex = ex)
        assertEquals("payment failed", failed.msg)
        assertEquals("payment failed: card declined, logger=L", failed.text)
        assertEquals("card declined, logger=L", entry(ex = ex).text)
        assertEquals(
            "ACTION: place_order, msg=\"low stock: card declined\", logger=L",
            entry(msg = "low stock", prefix = action, ex = ex).text,
        )
    }

    @Test
    fun an_exception_without_a_message_adds_nothing_to_the_text() {
        val ex = IllegalStateException()
        assertEquals("payment failed, logger=L", entry(msg = "payment failed", ex = ex).text)
        assertEquals("logger=L", entry(ex = ex).text)
    }

    @Test
    fun a_quoted_msg_escapes_the_backslash_the_quote_and_the_newline() {
        val text = entry(msg = "say \"hi\"\nC:\\tmp", prefix = action).text
        assertEquals("ACTION: place_order, msg=\"say \\\"hi\\\"\\nC:\\\\tmp\", logger=L", text)
    }

    @Test
    fun text_has_only_the_parts_that_are_set() {
        assertEquals("a=1, b=2, logger=L", entry(fields = fields("a" to 1, "b" to 2)).text)
        assertEquals("logger=L", entry().text)
        assertEquals("", entry(name = "").text)
        assertEquals("a=1", entry(name = "", fields = fields("a" to 1)).text)
    }

    @Test
    fun text_keeps_field_keys_as_written_and_leaves_out_the_source() {
        val text = entry(prefix = action, fields = fields("orderId" to 1)).text
        assertEquals("ACTION: place_order, orderId=1, logger=L", text)
        assertTrue("shop" !in text)
        assertTrue("orders" !in text)
    }

    @Test
    fun an_entry_has_no_prefix_and_the_default_source_unless_given() {
        val bare = LogEntry(level = LogLevel.Info)
        assertNull(bare.prefix)
        assertEquals(Source.DEFAULT, bare.source)
    }

    @Test
    fun thread_is_empty_by_default_and_not_part_of_the_text() {
        assertEquals("", entry(msg = "slow").thread)
        val withThread = LogEntry(name = "L", level = LogLevel.Info, msg = "slow", thread = "worker-1")
        assertEquals("slow, logger=L", withThread.text)
    }

    @Test
    fun safe_settings_are_the_safe_defaults() {
        val settings = LogSettings.safe()
        assertEquals(LogLevel.Error, settings.level)
        assertEquals(StackTraces.Off, settings.stackTraces)
        assertTrue(settings.policies.single() is RedactPolicy)
        assertTrue(settings.errors !== ErrorHandler.Throw)
        assertEquals(Source.DEFAULT, settings.source)
        assertTrue(settings.levels.isEmpty())
        assertEquals(StackTraces.DEFAULT_MAX_LINES, settings.maxTraceLines)
    }

    @Test
    fun safe_settings_take_a_source() {
        val settings = LogSettings.safe(Source("shop.example.com", "orders"))
        assertEquals("shop.example.com", settings.source.origin)
        assertEquals("orders", settings.source.scope)
    }

    @Test
    fun copies_of_settings_share_the_same_error_handler() {
        val settings = LogSettings.safe()
        assertSame(settings.errors, settings.copy(level = LogLevel.Debug).errors)
    }

    @Test
    fun the_default_redaction_covers_the_common_sensitive_keys() {
        val keys = listOf("password", "email", "phone", "token", "secret", "authorization", "cookie", "ssn", "apikey")
        keys.forEach { assertTrue(it in RedactPolicy.defaults, it) }
    }
}
