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

package kiit.logs

/**
 * Sends each entry to several sinks, e.g. the console and a crash reporter. A sink that throws
 * doesn't stop the others, and logging never throws into the caller.
 *
 *     val sink = CompositeSink(ConsoleSink(), remoteSink.minLevel(LogLevel.Error))
 */
class CompositeSink(private val sinks: List<LogSink>) : LogSink {

    constructor(vararg sinks: LogSink) : this(sinks.asList())

    override fun emit(entry: LogEntry) = each { it.emit(entry) }

    override fun flush() = each { it.flush() }

    override fun close() = each { it.close() }

    private inline fun each(action: (LogSink) -> Unit) {
        sinks.forEach { sink ->
            try {
                action(sink)
            } catch (ignored: Throwable) {
                // One broken sink shouldn't take the others, or the app, down with it
            }
        }
    }
}

private class FilteredSink(private val sink: LogSink, private val keep: (LogEntry) -> Boolean) : LogSink {
    override fun emit(entry: LogEntry) {
        if (keep(entry)) sink.emit(entry)
    }

    override fun flush() = sink.flush()

    override fun close() = sink.close()

    override val raw: Any? get() = sink.raw
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
