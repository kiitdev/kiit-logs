package kiit.logs

import kiit.logs.data.Action
import kiit.logs.sinks.ConsoleSink
import kiit.logs.sinks.MemorySink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LogsTests {
    @Test
    fun a_logs_uses_the_settings_and_sends_entries_to_the_sink() {
        val sink = MemorySink()
        val settings = testSettings(LogLevel.Info)
        val factory = Logs(settings, sink)
        assertSame(settings, factory.settings)

        val log = factory.logger("orders")
        log.debug(Action("skipped"))
        log.info(Action("place", "order_id" to "abc"))
        assertEquals(1, sink.entries.size)
        assertEquals("place", sink.entries[0].prefix?.value)
        assertEquals("orders", sink.entries[0].name)
        assertEquals(fixedTime, sink.entries[0].time)
    }

    @Test
    fun console_uses_the_settings_and_a_console_sink() {
        val settings = testSettings()
        val factory = Logs.console(settings)
        assertSame(settings, factory.settings)
        assertIs<ConsoleSink>(factory.raw)
    }

    @Test
    fun console_without_settings_uses_the_safe_defaults() {
        val logs = Logs.console()
        assertEquals(LogSettings.safe().level, logs.settings.level)
        assertEquals(LogLevel.Error, logs.logger("app").level)
        logs.setLevel(LogLevel.Debug)
        assertEquals(LogLevel.Debug, logs.logger("app").level)
    }

    @Test
    fun logger_returns_the_same_logger_for_the_same_name() {
        val factory = Logs(testSettings(), MemorySink())
        assertSame(factory.logger("a"), factory.logger("a"))
        assertTrue(factory.logger("a") !== factory.logger("b"))
    }

    @Test
    fun a_level_change_on_the_returned_factory_applies() {
        val sink = MemorySink()
        val factory = Logs(testSettings(LogLevel.Error), sink)
        val log = factory.logger("orders")
        log.info(Action("before"))
        factory.setLevel(LogLevel.Info)
        log.info(Action("after"))
        assertEquals(listOf("after"), sink.entries.map { it.prefix?.value })
    }

    @Test
    fun each_console_call_returns_a_new_factory() {
        val settings = testSettings()
        assertTrue(Logs.console(settings) !== Logs.console(settings))
    }

    private fun scoped(sink: MemorySink) = Logs(testSettings().copy(source = Source("shop", "root")), sink)

    @Test
    fun a_logger_without_a_scope_uses_the_scope_of_the_settings() {
        val sink = MemorySink()
        scoped(sink).logger("orders").info(Action("place"))
        assertEquals(Source("shop", "root"), sink.entries.single().source)
    }

    @Test
    fun a_logger_with_a_scope_replaces_the_scope_and_keeps_the_origin() {
        val sink = MemorySink()
        val logs = scoped(sink)
        logs.logger("orders", scope = "orders.payment").info(Action("charge"))
        logs.logger(PaymentService::class, scope = "orders.payment").info(Action("refund"))
        assertEquals(listOf(Source("shop", "orders.payment"), Source("shop", "orders.payment")), sink.entries.map { it.source })
        assertEquals(listOf("orders", PaymentService::class.qualifiedName), sink.entries.map { it.name })
    }

    @Test
    fun an_empty_scope_is_a_scope_and_replaces_the_one_of_the_settings() {
        val sink = MemorySink()
        scoped(sink).logger("orders", scope = "").info(Action("place"))
        assertEquals(Source("shop", ""), sink.entries.single().source)
    }

    @Test
    fun the_same_name_and_scope_give_the_same_logger() {
        val logs = scoped(MemorySink())
        assertSame(logs.logger("a", scope = "x"), logs.logger("a", scope = "x"))
        assertSame(logs.logger(PaymentService::class, scope = "x"), logs.logger(PaymentService::class, scope = "x"))
        assertSame(logs.logger("a"), logs.logger("a", scope = null))
    }

    @Test
    fun the_same_name_with_a_different_scope_is_a_different_logger() {
        val sink = MemorySink()
        val logs = scoped(sink)
        assertNotSame(logs.logger("a", scope = "x"), logs.logger("a", scope = "y"))
        assertNotSame(logs.logger("a"), logs.logger("a", scope = "x"))
        logs.logger("a", scope = "x").info(Action("one"))
        logs.logger("a", scope = "y").info(Action("two"))
        logs.logger("a").info(Action("three"))
        assertEquals(listOf("x", "y", "root"), sink.entries.map { it.source.scope })
    }

    @Test
    fun with_keeps_the_scope_of_the_logger() {
        val sink = MemorySink()
        scoped(sink).logger("a", scope = "orders.payment").with("trace_id" to "t-1").info(Action("charge"))
        val entry = sink.entries.single()
        assertEquals("orders.payment", entry.source.scope)
        assertEquals(fields("trace_id" to "t-1"), entry.fields)
    }

    @Test
    fun a_settings_change_applies_to_the_origin_and_the_root_scope_but_not_a_logger_scope() {
        val sink = MemorySink()
        val logs = scoped(sink)
        val plain = logs.logger("a")
        val payment = logs.logger("b", scope = "orders.payment")
        plain.settings = plain.settings.copy(source = Source("after", "other"))
        plain.info(Action("one"))
        payment.info(Action("two"))
        assertEquals(listOf(Source("after", "other"), Source("after", "orders.payment")), sink.entries.map { it.source })
    }

    @Test
    fun a_level_is_set_by_name_whatever_the_scope() {
        val sink = MemorySink()
        val logs = Logs(testSettings(LogLevel.Error), sink)
        logs.setLevel("com.shop.orders", LogLevel.Debug)
        logs.logger("com.shop.orders.Place", scope = "x").debug(Action("shown"))
        logs.logger("com.shop.other.Place", scope = "x").debug(Action("hidden"))
        assertEquals(listOf("shown"), sink.entries.map { it.prefix?.value })
    }

    private class PaymentService
}
