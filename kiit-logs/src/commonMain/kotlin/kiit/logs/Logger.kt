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

import kiit.logs.internal.ErrorGuard
import kiit.logs.internal.LogState
import kiit.logs.internal.SettingsRef
import kiit.logs.internal.StackTraceBuilder
import kiit.logs.internal.currentThreadName
import kiit.logs.policies.ErrorHandler.Stage
import kiit.logs.policies.Policies
import kiit.logs.sinks.LogSink

/**
 * A logger. Everything it logs is a [LogData], and the built-in kinds say why it is logged:
 *
 *     log.info(Action("place_order", "order_id" to id))              // something about to be done
 *     log.info(Event("order_placed", "order_id" to id, "total" to 42)) // something that has happened
 *     log.error(Text("charge failed", ex))                           // free text, the others are preferred
 *
 * Every level has an eager form and a lazy form. The data is built before the level is checked in the eager form.
 * If it is expensive to build, pass a lambda, which only runs if the level is enabled:
 *
 *     log.debug { Action("place_order", "total" to expensive()) }
 *
 * [log] is the one general method, it takes the level, and the methods named for a level call it:
 *
 *     log.log(LogLevel.Warn, Text("payment slow"))
 *     log.log(LogLevel.Warn) { Text("payment slow ${expensive()}") }
 *
 * Kiit modules use Result<T, E> for errors as values, so an error usually propagates to an edge
 * ( e.g. an API handler ) where it is logged once, with the action and its inputs/outputs.
 *
 * A logger sends every entry that passes the level check and the [LogSettings.policies] to its [LogSink].
 */
class Logger private constructor(
    private val state: LogState,
    val name: String,
    private val sink: LogSink,
    private val bound: List<Pair<String, Any?>>
) {
    // Loggers come from a [Logs], which gives all of its loggers the one settings reference it holds
    internal constructor(source: SettingsRef, name: String, sink: LogSink) :
        this(LogState(source, name), name, sink, emptyList())

    /**
     * The current settings, for reading. The level is the one setting that can change while the app runs, and
     * it changes through the factory, e.g. to lower it for diagnostics:
     *
     *     logs.setLevel(LogLevel.Debug)
     *     logs.setLevel("com.shop.orders", LogLevel.Debug)
     */
    var settings: LogSettings
        get() = state.settings
        internal set(value) {
            state.settings = value
        }

    /**
     * The level this logger uses: the longest matching name in [LogSettings.levels], otherwise
     * [LogSettings.level].
     */
    val level: LogLevel get() = state.level

    fun isEnabled(level: LogLevel): Boolean = level != LogLevel.Off && level >= this.level

    /**
     * The wrapped library's logger for this logger's name, if the sink wraps one. See [LogSink.rawFor].
     */
    val raw: Any? get() = sink.rawFor(name)

    /**
     * [raw] as T, or null if there is none or it is a different type.
     */
    inline fun <reified T> rawAs(): T? = raw as? T

    /**
     * Pushes out anything the sink has buffered.
     */
    fun flush() {
        ErrorGuard.guard(settings.errors, Stage.Lifecycle, null) { sink.flush() }
    }

    /**
     * Logs at any level. The data is built before the level is checked, see the lambda form for data that is
     * expensive to build. An exception in the data has its message added to the message after a colon:
     * "payment failed: card declined".
     */
    fun log(level: LogLevel, data: LogData) {
        send(level) { s -> build(s, level, data) }
    }

    /**
     * Logs at any level, with data that is only built if the level is enabled:
     *
     *     log(LogLevel.Debug) { Text("cache ${expensive()}") }
     */
    fun log(level: LogLevel, data: () -> LogData) {
        send(level) { s -> build(s, level, data()) }
    }

    // One method per level, each for the data and for a lambda that makes it
    fun verbose(data: LogData) = log(LogLevel.Verbose, data)

    fun verbose(data: () -> LogData) = log(LogLevel.Verbose, data)

    fun debug(data: LogData) = log(LogLevel.Debug, data)

    fun debug(data: () -> LogData) = log(LogLevel.Debug, data)

    fun info(data: LogData) = log(LogLevel.Info, data)

    fun info(data: () -> LogData) = log(LogLevel.Info, data)

    fun warn(data: LogData) = log(LogLevel.Warn, data)

    fun warn(data: () -> LogData) = log(LogLevel.Warn, data)

    fun error(data: LogData) = log(LogLevel.Error, data)

    fun error(data: () -> LogData) = log(LogLevel.Error, data)

    fun fatal(data: LogData) = log(LogLevel.Fatal, data)

    fun fatal(data: () -> LogData) = log(LogLevel.Fatal, data)

    // The message, then the exception's message after a colon when there is one: "payment failed: card declined"
    private fun textWith(msg: String?, ex: Throwable?): String =
        when {
            ex == null -> msg
            msg.isNullOrEmpty() -> ex.message
            else -> ex.message?.let { "$msg: $it" } ?: msg
        } ?: ""

    /**
     * A logger that adds these fields to every entry it logs, e.g. an id for one request:
     *
     *     val log = logger.with("trace_id" to traceId)
     *     log.info(Action("place_order", "order_id" to id))   // fields: trace_id, order_id
     *
     * It shares this logger's settings, so a level change applies to it too. The fields go through the policies
     * like any others.
     */
    fun with(vararg fields: Pair<String, Any?>): Logger = Logger(state, name, sink, bound + fields.asList())

    /**
     * The one path every log call takes: level check, build the entry, run the policies, deliver. Anything that
     * throws along the way goes to the [LogSettings.errors] handler, and a lazy message or field lambda
     * is only run when the level is enabled.
     *
     * The settings are read once, so one call never mixes two versions of them, even if they are replaced
     * while it runs. A change applies from the next call.
     */
    private inline fun send(level: LogLevel, make: (LogSettings) -> LogEntry) {
        if (!isEnabled(level)) return
        val s = settings
        val entry = ErrorGuard.guard(s.errors, Stage.Build, null) { make(s) } ?: return
        // A policy that throws drops the entry, and the error is reported without it, since it may not be redacted yet
        val delivered = ErrorGuard.guard(s.errors, Stage.Policy, null) { Policies.applyTo(s.policies, entry) } ?: return
        ErrorGuard.guard(s.errors, Stage.Sink, delivered) { sink.emit(delivered) }
    }

    // One entry from the data, plus the settings snapshot it is stamped from
    private fun build(s: LogSettings, level: LogLevel, data: LogData): LogEntry {
        val ex = data.ex
        return LogEntry(
            name = name,
            level = level,
            msg = textWith(data.msg, ex),
            ex = ex,
            prefix = data.prefix,
            source = s.source,
            fields = bound + data.fields,
            time = s.clock.now(),
            trace = ex?.let { StackTraceBuilder.render(s.stackTraces, it, s.maxTraceLines) },
            thread = currentThreadName(),
        )
    }

    companion object
}
