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
import kiit.logs.policies.ErrorPolicy

/**
 * Sends each entry to several sinks, e.g. the console and a crash reporter. A sink that throws
 * doesn't stop the others. Once they have all run, the first error is thrown, with the rest attached
 * to it, so the logger's [ErrorPolicy] decides what happens.
 *
 *     val sink = CompositeSink(ConsoleSink(), remoteSink.minLevel(LogLevel.Error))
 */
class CompositeSink(private val sinks: List<LogSink>) : LogSink {
    constructor(vararg sinks: LogSink) : this(sinks.asList())

    override fun emit(entry: LogEntry) = each { it.emit(entry) }

    override fun flush() = each { it.flush() }

    override fun close() = each { it.close() }

    /**
     * The first sink that has one for the name.
     */
    override fun rawFor(name: String): Any? = sinks.firstNotNullOfOrNull { it.rawFor(name) }

    // It catches any Exception on purpose, one broken sink must not stop the others
    @Suppress("TooGenericExceptionCaught")
    private inline fun each(action: (LogSink) -> Unit) {
        var first: Exception? = null
        sinks.forEach { sink ->
            try {
                action(sink)
            } catch (e: Exception) {
                val head = first
                if (head == null) first = e else head.addSuppressed(e)
            }
        }
        first?.let { throw it }
    }
}
