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
import kiit.logs.policies.ErrorHandler.Stage
import kiit.logs.policies.Policies
import kiit.logs.sinks.LogSink

/**
 * A logger. Structured logging is the default style, log an action with key/value fields:
 *
 *     info("place", "order_id" to id, "total" to 42)
 *     error("place", ex, "order_id" to id)
 *
 * Fields are built before the level is checked. If they are expensive to build, pass a lambda so
 * they are only built when the level is enabled:
 *
 *     debug("place") { listOf("total" to expensive()) }
 *
 * Kiit modules use Result<T, E> for errors as values, so an error usually propagates to an edge
 * ( e.g. an API handler ) where it is logged once, with the action and its inputs/outputs.
 *
 * Free text is also supported, with [log]:
 *
 *     log(LogLevel.Error, "payment failed", ex)
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
     * Pushes out anything the sink has buffered.
     */
    fun flush() {
        ErrorGuard.guard(settings.errors, Stage.Lifecycle, null) { sink.flush() }
    }

    // Structured logging: an action with key/value fields ( redacted by the logger's policies )
    fun verbose(action: String, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Verbose, action, null, fields)

    fun debug(action: String, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Debug, action, null, fields)

    fun info(action: String, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Info, action, null, fields)

    fun warn(action: String, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Warn, action, null, fields)

    fun error(action: String, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Error, action, null, fields)

    fun fatal(action: String, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Fatal, action, null, fields)

    // Structured logging with an exception
    fun verbose(action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Verbose, action, ex, fields)

    fun debug(action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Debug, action, ex, fields)

    fun info(action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Info, action, ex, fields)

    fun warn(action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Warn, action, ex, fields)

    fun error(action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Error, action, ex, fields)

    fun fatal(action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Fatal, action, ex, fields)

    // Structured logging, lazy: fields are only built if the level is enabled
    fun verbose(action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) =
        logIfEnabled(LogLevel.Verbose, action, ex, fields)

    fun debug(action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) =
        logIfEnabled(LogLevel.Debug, action, ex, fields)

    fun info(action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) =
        logIfEnabled(LogLevel.Info, action, ex, fields)

    fun warn(action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) =
        logIfEnabled(LogLevel.Warn, action, ex, fields)

    fun error(action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) =
        logIfEnabled(LogLevel.Error, action, ex, fields)

    fun fatal(action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) =
        logIfEnabled(LogLevel.Fatal, action, ex, fields)

    /**
     * Logs an action at any level
     */
    fun logAction(
        level: LogLevel,
        action: String,
        ex: Throwable?,
        fields: Array<out Pair<String, Any?>>
    ) {
        logIfEnabled(level, null, fields.asList(), ex, action)
    }

    // Free text

    /**
     * Logs a message. If there is an exception, its message is appended after a colon:
     * "payment failed: card declined".
     * @param level
     * @param msg
     * @param ex
     */
    fun log(level: LogLevel, msg: String?, ex: Throwable? = null) {
        send(level) { s -> build(s, level, textWith(msg, ex), ex, null, emptyList()) }
    }

    /**
     * Logs a message that is only built if the level is enabled, with an exception if there is one. The
     * exception's message is appended, as in the other [log]:
     *
     *     log(LogLevel.Debug) { "cache ${expensive()}" }
     *     log(LogLevel.Error, ex) { "charge ${expensive()}" }
     *
     * Details that should be searchable are better as fields, see [debug] with a lambda of fields.
     */
    fun log(level: LogLevel, ex: Throwable? = null, callback: () -> String) {
        send(level) { s -> build(s, level, textWith(callback(), ex), ex, null, emptyList()) }
    }

    // The message, then the exception's message after a colon when there is one: "payment failed: card declined"
    private fun textWith(msg: String?, ex: Throwable?): String =
        when {
            ex == null -> msg
            msg.isNullOrEmpty() -> ex.message
            else -> ex.message?.let { "$msg: $it" } ?: msg
        } ?: ""

    /**
     * Logs an entry with key/value fields. Fields are redacted by the [LogSettings.policies] before
     * the entry is delivered.
     */
    private fun logIfEnabled(
        level: LogLevel,
        msg: String?,
        fields: List<Pair<String, Any?>>,
        ex: Throwable? = null,
        action: String? = null
    ) {
        send(level) { s -> build(s, level, msg ?: ex?.message ?: "", ex, action, fields) }
    }

    /**
     * Logs an action. The fields are only built if the level is enabled.
     */
    private fun logIfEnabled(
        level: LogLevel,
        action: String,
        ex: Throwable?,
        fields: () -> List<Pair<String, Any?>>
    ) {
        send(level) { s -> build(s, level, ex?.message ?: "", ex, action, fields()) }
    }

    /**
     * A logger that adds these fields to every entry it logs, e.g. an id for one request:
     *
     *     val log = logger.with("trace_id" to traceId)
     *     log.info("place", "order_id" to id)   // fields: trace_id, order_id
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

    // The parts of one entry, plus the settings snapshot it is stamped from
    @Suppress("LongParameterList")
    private fun build(
        s: LogSettings,
        level: LogLevel,
        msg: String,
        ex: Throwable?,
        action: String?,
        fields: List<Pair<String, Any?>>
    ): LogEntry {
        return LogEntry(
            name = name,
            level = level,
            msg = msg,
            ex = ex,
            action = action,
            origin = s.origin,
            scope = s.scope,
            fields = bound + fields,
            time = s.clock.now(),
            trace = ex?.let { StackTraceBuilder.render(s.stackTraces, it, s.maxTraceLines) },
        )
    }

    companion object
}

/**
 * [Logger.raw] as T, or null if there is none or it is a different type.
 */
inline fun <reified T> Logger.rawAs(): T? = raw as? T
