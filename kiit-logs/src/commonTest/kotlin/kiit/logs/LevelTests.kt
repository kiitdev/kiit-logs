package kiit.logs

import kiit.logs.sinks.ConsoleSink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

class LevelTests {
    private val byName =
        LogSettings.safe().copy(
            levels = mapOf("com.shop.orders" to LogLevel.Debug, "com.shop.orders.audit" to LogLevel.Warn),
        )

    @Test
    fun levels_are_ordered() {
        val ordered = listOf(LogLevel.Trace, LogLevel.Debug, LogLevel.Info, LogLevel.Warn, LogLevel.Error, LogLevel.Fatal, LogLevel.Off)
        assertEquals(ordered, ordered.sortedBy { it.code })
        assertTrue(LogLevel.Fatal < LogLevel.Off)
    }

    @Test
    fun a_name_uses_the_default_level_when_nothing_matches() {
        assertEquals(LogLevel.Error, byName.levelFor("com.other"))
        assertEquals(LogLevel.Error, LogSettings.safe().levelFor("anything"))
    }

    @Test
    fun a_name_matches_itself_and_the_names_under_it() {
        assertEquals(LogLevel.Debug, byName.levelFor("com.shop.orders"))
        assertEquals(LogLevel.Debug, byName.levelFor("com.shop.orders.checkout"))
    }

    @Test
    fun the_longest_matching_name_wins() {
        assertEquals(LogLevel.Warn, byName.levelFor("com.shop.orders.audit"))
        assertEquals(LogLevel.Warn, byName.levelFor("com.shop.orders.audit.x"))
    }

    @Test
    fun a_prefix_must_end_at_a_dot() {
        assertEquals(LogLevel.Error, byName.levelFor("com.shop.ordersX"))
        assertEquals(LogLevel.Error, byName.levelFor("com.shop"))
    }

    @Test
    fun a_logger_resolves_its_level_at_creation_and_when_settings_change() {
        val log = testLogger(byName, MemorySink(), "com.shop.orders.checkout")
        assertEquals(LogLevel.Debug, log.level)
        log.settings = byName.copy(levels = emptyMap())
        assertEquals(LogLevel.Error, log.level)
    }

    @Test
    fun the_factory_returns_the_same_logger_for_the_same_name() {
        val factory = Logs(testSettings(), MemorySink())
        assertSame(factory.logger("A"), factory.logger("A"))
        assertNotSame(factory.logger("A"), factory.logger("B"))
    }

    @Test
    fun a_logger_for_a_class_is_named_after_the_class() {
        val factory = Logs(testSettings(), MemorySink())
        val expected = LevelTests::class.qualifiedName ?: LevelTests::class.simpleName
        assertEquals(expected, factory.logger(LevelTests::class).name)
        assertSame(factory.logger(LevelTests::class), factory.logger(LevelTests::class))
    }

    @Test
    fun a_missing_name_is_root() {
        val logs = Logs(testSettings(), MemorySink())
        assertEquals("root", LogFactory.DEFAULT_NAME)
        assertEquals("root", logs.logger(null).name)
        assertEquals("root", logs.logger().name)
    }

    @Test
    fun no_name_and_null_give_the_same_logger() {
        val logs = Logs(testSettings(), MemorySink())
        assertSame(logs.logger(), logs.logger(null))
        assertNotSame(logs.logger(), logs.logger(""))
    }

    @Test
    fun a_class_without_a_name_gets_the_root_logger() {
        val logs = Logs(testSettings(), MemorySink())
        assertSame(logs.logger(), logs.logger(object {}::class))
    }

    @Test
    fun set_level_reaches_existing_and_new_loggers() {
        val sink = MemorySink()
        val factory = Logs(testSettings(LogLevel.Error), sink)
        val existing = factory.logger("A")
        factory.setLevel(LogLevel.Debug)
        assertEquals(LogLevel.Debug, existing.level)
        assertEquals(LogLevel.Debug, factory.logger("B").level)
        assertEquals(LogLevel.Debug, factory.settings.level)
        existing.debug("now shown")
        assertEquals(1, sink.entries.size)
    }

    @Test
    fun set_level_by_name_covers_the_names_under_it_for_existing_and_new_loggers() {
        val factory = Logs(testSettings(LogLevel.Error), MemorySink())
        val existing = factory.logger("com.shop.orders.checkout")
        val other = factory.logger("com.other")
        factory.setLevel("com.shop.orders", LogLevel.Debug)
        assertEquals(LogLevel.Debug, existing.level)
        assertEquals(LogLevel.Error, other.level)
        assertEquals(LogLevel.Debug, factory.logger("com.shop.orders.audit").level)
    }

    @Test
    fun a_global_level_change_keeps_named_levels() {
        val factory = Logs(testSettings(LogLevel.Error), MemorySink())
        factory.setLevel("com.shop", LogLevel.Debug)
        factory.setLevel(LogLevel.Warn)
        assertEquals(LogLevel.Debug, factory.logger("com.shop.orders").level)
        assertEquals(LogLevel.Warn, factory.logger("com.other").level)
    }

    @Test
    fun a_logger_created_after_the_settings_have_the_named_levels() {
        val factory = Logs(byName, MemorySink())
        assertEquals(LogLevel.Debug, factory.logger("com.shop.orders.x").level)
        assertEquals(LogLevel.Warn, factory.logger("com.shop.orders.audit.x").level)
    }

    @Test
    fun the_factory_flush_and_close_go_to_the_sink() {
        val sink = MemorySink()
        val factory = Logs(testSettings(), sink)
        factory.flush()
        factory.close()
        assertEquals(1, sink.flushed)
        assertEquals(1, sink.closed)
    }

    @Test
    fun the_provider_defaults_to_the_sink_and_can_be_given() {
        val sink = MemorySink()
        assertSame(sink, Logs(testSettings(), sink).provider)
        assertEquals("root", Logs(testSettings(), sink, "root").provider)
        val console = Logs.console(testSettings())
        assertTrue(console.provider is ConsoleSink)
        assertTrue(console.providerAs<ConsoleSink>() != null)
        assertEquals(null, console.providerAs<Int>())
    }
}
