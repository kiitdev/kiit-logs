/**
 *  <kiit_header>
 * url: www.kiit.dev
 * git: www.github.com/slatekit/kiit
 * org: www.codehelix.co
 * author: Kishore Reddy
 * copyright: 2016 CodeHelix Solutions Inc.
 * license: refer to website and/or github
 *
 *
 *  </kiit_header>
 */

package kiit.logs.sinks

import kiit.logs.LogEntry
import kotlin.concurrent.atomics.AtomicInt
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * Keeps the entries it receives, so a test can check what an app logged. It is safe to use from several threads.
 * The policies run before an entry reaches a sink, so the fields here are already redacted.
 *
 *     val sink = MemorySink()
 *     val logs = Logs(LogSettings.safe().copy(level = LogLevel.Info), sink)
 *     logs.logger("orders").info(Action("place", "order_id" to "abc"))
 *     assertEquals("place", sink.entries.single().prefix?.value)
 */
@OptIn(ExperimentalAtomicApi::class)
class MemorySink : LogSink {
    private val stored = AtomicReference<List<LogEntry>>(emptyList())
    private val flushes = AtomicInt(0)
    private val closes = AtomicInt(0)

    /**
     * What was logged so far, in order. It is a snapshot, so entries logged later are not added to it.
     */
    val entries: List<LogEntry>
        get() = stored.load()

    /**
     * How many times [flush] was called. [clear] doesn't reset it.
     */
    val flushed: Int
        get() = flushes.load()

    /**
     * How many times [close] was called. [clear] doesn't reset it.
     */
    val closed: Int
        get() = closes.load()

    /**
     * The entries whose prefix has this value, e.g. the name of an action or an event, in order. The label is not
     * compared.
     */
    fun find(value: String): List<LogEntry> = entries.filter { it.prefix?.value == value }

    /**
     * Removes the entries, e.g. between tests.
     */
    fun clear() = stored.store(emptyList())

    override fun emit(entry: LogEntry) {
        while (true) {
            val old = stored.load()
            if (stored.compareAndSet(old, old + entry)) return
        }
    }

    override fun flush() {
        flushes.fetchAndAdd(1)
    }

    override fun close() {
        closes.fetchAndAdd(1)
    }
}
