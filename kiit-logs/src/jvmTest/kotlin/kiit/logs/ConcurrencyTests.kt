package kiit.logs

import kiit.logs.data.Action
import kiit.logs.sinks.MemorySink
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcurrencyTests {
    private fun runThreads(count: Int, body: (Int) -> Unit) {
        val failures = Collections.synchronizedList(mutableListOf<Throwable>())
        val threads =
            (0 until count).map { n ->
                Thread {
                    try {
                        body(n)
                    } catch (t: Throwable) {
                        failures.add(t)
                    }
                }
            }
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        assertEquals(emptyList<Throwable>(), failures.toList())
    }

    @Test
    fun the_factory_makes_one_logger_per_name_when_threads_race() {
        val factory = Logs(testSettings(), MemorySink())
        val seen = ConcurrentHashMap<String, MutableSet<Logger>>()
        runThreads(8) {
            repeat(500) { i ->
                val name = "n${i % 20}"
                seen.computeIfAbsent(name) { ConcurrentHashMap.newKeySet() }.add(factory.logger(name))
                if (i % 50 == 0) factory.setLevel(LogLevel.Info)
            }
        }
        assertEquals(20, seen.size)
        seen.values.forEach { assertEquals(1, it.size) }
    }

    @Test
    fun every_entry_arrives_when_many_threads_log() {
        val sink = MemorySink()
        val log = testLogger(testSettings(), sink, "L")
        runThreads(8) { n -> repeat(250) { log.info(Action("place", "thread" to n)) } }
        assertEquals(2000, sink.entries.size)
    }

    @Test
    fun each_threads_entries_keep_their_order_in_a_memory_sink() {
        val sink = MemorySink()
        val log = testLogger(testSettings(), sink, "L")
        runThreads(8) { n -> repeat(250) { i -> log.info(Action("place", "thread" to n, "i" to i)) } }
        val byThread = sink.entries.groupBy { e -> e.fields.first { it.first == "thread" }.second }
        assertEquals(8, byThread.size)
        byThread.values.forEach {
                list ->
            assertEquals((0 until 250).toList(), list.map { e -> e.fields.first { it.first == "i" }.second })
        }
    }

    @Test
    fun a_level_change_while_threads_log_does_not_fail() {
        val sink = MemorySink()
        val factory = Logs(testSettings(), sink)
        val log = factory.logger("L")
        runThreads(6) { n ->
            repeat(500) { i ->
                if (n == 0 && i % 25 == 0) factory.setLevel(if (i % 50 == 0) LogLevel.Off else LogLevel.Debug)
                log.info(Action("x"))
            }
        }
    }

    @Test
    fun level_changes_by_name_from_many_threads_are_all_kept() {
        val factory = Logs(testSettings(LogLevel.Error), MemorySink())
        val names = (0 until 8).flatMap { n -> (0 until 100).map { i -> "t$n.n$i" } }
        // Loggers that exist before the changes
        names.forEach { factory.logger(it) }
        runThreads(8) { n ->
            repeat(100) { i -> factory.setLevel("t$n.n$i", LogLevel.Debug) }
        }
        assertEquals(names.size, factory.settings.levels.size)
        names.forEach { assertEquals(LogLevel.Debug, factory.logger(it).level, it) }
    }

    @Test
    fun clearing_levels_from_many_threads_keeps_exactly_the_names_left_set() {
        val factory = Logs(testSettings(LogLevel.Error), MemorySink())
        val names = (0 until 8).flatMap { n -> (0 until 100).map { i -> "t$n.n$i" } }
        names.forEach { factory.logger(it) }
        // Each thread sets its names and clears the odd ones, while thread 0 also changes the global level
        runThreads(8) { n ->
            repeat(100) { i ->
                factory.setLevel("t$n.n$i", LogLevel.Debug)
                if (i % 2 == 1) factory.setLevel("t$n.n$i", null)
                if (n == 0 && i % 10 == 0) factory.setLevel(LogLevel.Info)
            }
        }
        val kept = names.filter { it.substringAfter(".n").toInt() % 2 == 0 }.toSet()
        assertEquals(kept, factory.settings.levels.keys)
        names.forEach { assertEquals(if (it in kept) LogLevel.Debug else LogLevel.Info, factory.logger(it).level, it) }
    }
}
