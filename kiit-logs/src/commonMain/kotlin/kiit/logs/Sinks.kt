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
