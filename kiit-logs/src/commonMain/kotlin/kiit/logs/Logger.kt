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
 * A logger sends every entry that passes the level check and the filter to its [LogSink].
 */
class Logger private constructor(
    private val state: LogState,
    val name: String,
    private val sink: LogSink,
    private val bound: List<Pair<String, Any?>>
) {

    constructor(settings: LogSettings, name: String, sink: LogSink) : this(LogState(settings, name), name, sink, emptyList())

    /**
     * Convenience constructor for when only the level is customized.
     */
    constructor(level: LogLevel, name: String, sink: LogSink) : this(LogSettings.safe().copy(level = level), name, sink)

    /**
     * The current settings. They can be replaced at runtime, e.g. to lower the level for diagnostics:
     *
     *     logger.settings = logger.settings.copy(level = LogLevel.Debug)
     */
    var settings: LogSettings
        get() = state.settings
        set(value) {
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
        settings.errors.guard(LogStage.Lifecycle, null) { sink.flush() }
    }

    /** =====================================================================
     * Structured logging: an action with key/value fields ( redacted by the logger's settings )
     * ======================================================================
     */
    fun debug(action: String, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Debug, action, null, fields)
    fun info (action: String, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Info , action, null, fields)
    fun warn (action: String, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Warn , action, null, fields)
    fun error(action: String, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Error, action, null, fields)
    fun fatal(action: String, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Fatal, action, null, fields)

    /** =====================================================================
     * Structured logging with an exception
     * ======================================================================
     */
    fun debug(action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Debug, action, ex, fields)
    fun info (action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Info , action, ex, fields)
    fun warn (action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Warn , action, ex, fields)
    fun error(action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Error, action, ex, fields)
    fun fatal(action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = logAction(LogLevel.Fatal, action, ex, fields)

    /** =====================================================================
     * Structured logging, lazy: fields are only built if the level is enabled
     * ======================================================================
     */
    fun debug(action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) = logIfEnabled(LogLevel.Debug, action, ex, fields)
    fun info (action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) = logIfEnabled(LogLevel.Info , action, ex, fields)
    fun warn (action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) = logIfEnabled(LogLevel.Warn , action, ex, fields)
    fun error(action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) = logIfEnabled(LogLevel.Error, action, ex, fields)
    fun fatal(action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) = logIfEnabled(LogLevel.Fatal, action, ex, fields)

    /**
     * Logs an action at any level
     */
    fun logAction(level: LogLevel, action: String, ex: Throwable?, fields: Array<out Pair<String, Any?>>) {
        logIfEnabled(level, null, fields.asList(), ex, action)
    }

    /** =====================================================================
     * Free text
     * ======================================================================
     */

    /**
     * Logs a message. If there is an exception, its message is appended.
     * @param level
     * @param msg
     * @param ex
     */
    fun log(level: LogLevel, msg: String?, ex: Throwable? = null) {
        send(level) {
            val text = when {
                ex == null -> msg
                msg.isNullOrEmpty() -> ex.message
                else -> ex.message?.let { "$msg\n$it" } ?: msg
            }
            build(level, text ?: "", ex, null, emptyList())
        }
    }

    /**
     * Logs a message that is only built if the level is enabled
     *
     * log(LogLevel.Debug, "updating user") { " some expensive message to build" }
     */
    fun log(level: LogLevel, msg: String? = null, callback: () -> String) {
        logIfEnabled(level, msg, callback)
    }

    private fun logIfEnabled(level: LogLevel, msg: String?, callback: () -> String) {
        send(level) { build(level, "${msg ?: ""} : ${callback()}", null, null, emptyList()) }
    }

    /**
     * Logs an entry with key/value fields. Fields are redacted per [settings] before the
     * entry is created.
     */
    private fun logIfEnabled(
        level: LogLevel,
        msg: String?,
        fields: List<Pair<String, Any?>>,
        ex: Throwable? = null,
        action: String? = null
    ) {
        send(level) { build(level, msg ?: ex?.message ?: "", ex, action, fields) }
    }

    /**
     * Logs an action. The fields are only built if the level is enabled.
     */
    private fun logIfEnabled(level: LogLevel, action: String, ex: Throwable?, fields: () -> List<Pair<String, Any?>>) {
        send(level) { build(level, ex?.message ?: "", ex, action, fields()) }
    }

    /**
     * A logger that adds these fields to every entry it logs, e.g. an id for one request:
     *
     *     val log = logger.with("trace_id" to traceId)
     *     log.info("place", "order_id" to id)   // fields: trace_id, order_id
     *
     * It shares this logger's settings, so a level change applies to it too. The fields are redacted
     * like any others.
     */
    fun with(vararg fields: Pair<String, Any?>): Logger = Logger(state, name, sink, bound + fields.asList())

    /**
     * The one path every log call takes: level check, build the entry, filter, deliver. Anything that
     * throws along the way goes to the [LogSettings.errors] policy, and a lazy message or field lambda
     * is only run when the level is enabled.
     */
    private inline fun send(level: LogLevel, make: () -> LogEntry) {
        if (!isEnabled(level)) return
        val s = settings
        val entry = s.errors.guard(LogStage.Build, null, make) ?: return
        val keep = s.errors.guard(LogStage.Filter, entry) { s.filter?.invoke(entry) != false } ?: true
        if (keep) s.errors.guard(LogStage.Sink, entry) { sink.emit(entry) }
    }

    private fun build(
        level: LogLevel,
        msg: String,
        ex: Throwable?,
        action: String?,
        fields: List<Pair<String, Any?>>
    ): LogEntry {
        val s = settings
        return LogEntry(
            name = name,
            level = level,
            msg = msg,
            ex = ex,
            action = action,
            origin = s.origin,
            scope = s.scope,
            fields = s.redaction.redact(bound + fields),
            time = s.clock.now(),
            trace = ex?.let { s.stackTraces.render(it, s.maxTraceLines) }
        )
    }

    companion object
}

/**
 * [Logger.raw] as T, or null if there is none or it is a different type.
 */
inline fun <reified T> Logger.rawAs(): T? = raw as? T
