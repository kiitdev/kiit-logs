package kiit.logs

import kiit.logs.sinks.LogSink
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcurrencyTests {
    private class SafeSink : LogSink {
        val entries: MutableList<LogEntry> = Collections.synchronizedList(mutableListOf())

        override fun emit(entry: LogEntry) {
            entries.add(entry)
        }
    }

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
        val factory = DefaultLogFactory(testSettings(), SafeSink())
        val seen = ConcurrentHashMap<String, MutableSet<Logger>>()
        runThreads(8) {
            repeat(500) { i ->
                val name = "n${i % 20}"
                seen.computeIfAbsent(name) { ConcurrentHashMap.newKeySet() }.add(factory.getLogger(name))
                if (i % 50 == 0) factory.setLevel(LogLevel.Info)
            }
        }
        assertEquals(20, seen.size)
        seen.values.forEach { assertEquals(1, it.size) }
    }

    @Test
    fun every_entry_arrives_when_many_threads_log() {
        val sink = SafeSink()
        val log = Logger(testSettings(), "L", sink)
        runThreads(8) { n -> repeat(250) { log.info("place", "thread" to n) } }
        assertEquals(2000, sink.entries.size)
    }

    @Test
    fun a_level_change_while_threads_log_does_not_fail() {
        val sink = SafeSink()
        val factory = DefaultLogFactory(testSettings(), sink)
        val log = factory.getLogger("L")
        runThreads(6) { n ->
            repeat(500) { i ->
                if (n == 0 && i % 25 == 0) factory.setLevel(if (i % 50 == 0) LogLevel.Off else LogLevel.Debug)
                log.info("x")
            }
        }
    }
}
