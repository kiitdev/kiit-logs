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
import kiit.logs.Logger
import kiit.logs.factories.LogFactory

/**
 * Where entries end up: the console, a wrapped library such as Logback, a file or the network.
 * This is what a provider implements. A [Logger] does the level check, the filter and redaction
 * before it calls [emit], so a sink only delivers.
 *
 * Sinks are shared by many loggers, so closing one is the job of the [LogFactory] that owns it.
 */
interface LogSink {
    /**
     * Delivers one entry. The fields are already redacted, and [LogEntry.trace] already follows
     * the stack trace setting. [LogEntry.ex] is still there for a sink that wants the Throwable.
     */
    fun emit(entry: LogEntry)

    /**
     * Pushes out anything buffered, e.g. before the app goes to the background. Does nothing by default.
     */
    fun flush() {}

    /**
     * Releases resources and flushes. Call it once, when the app shuts down. Does nothing by default.
     */
    fun close() {}

    /**
     * Escape hatch to the wrapped library's logger for a logger name, e.g. Logback's own Logger, or
     * null if this sink doesn't wrap one. A sink is shared by every logger of a factory, so it is given
     * the name. It is Any because the wrapped types are platform specific and can't be named in
     * common code. Use [LogFactory.provider] for the wrapped library's root object.
     */
    fun rawFor(name: String): Any? = null
}
