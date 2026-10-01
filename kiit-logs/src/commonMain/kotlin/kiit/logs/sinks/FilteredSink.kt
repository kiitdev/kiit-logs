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
import kiit.logs.LogLevel

private class FilteredSink(private val sink: LogSink, private val keep: (LogEntry) -> Boolean) : LogSink {
    override fun emit(entry: LogEntry) {
        if (keep(entry)) sink.emit(entry)
    }

    override fun flush() = sink.flush()

    override fun close() = sink.close()

    override fun rawFor(name: String): Any? = sink.rawFor(name)
}

/**
 * A sink that only gets entries at this level or above, e.g. only errors for a remote service.
 * The logger's own level still decides what is logged at all, this only narrows one sink.
 */
fun LogSink.minLevel(level: LogLevel): LogSink = FilteredSink(this) { it.level >= level }

/**
 * A sink that only gets the entries this returns true for.
 */
fun LogSink.filtered(keep: (LogEntry) -> Boolean): LogSink = FilteredSink(this, keep)
