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

import kotlin.concurrent.Volatile
import kotlin.reflect.KClass

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
 * A provider extends Logger and implements [emit].
 */
abstract class Logger(
    settings: LogSettings,
    open val name: String = "",
    open val logType: KClass<*>? = null
) {

    /**
     * Convenience constructor for when only the level is customized.
     */
    constructor(
        level: LogLevel,
        name: String = "",
        logType: KClass<*>? = null
    ) : this(LogSettings.safe().copy(level = level), name, logType)

    /**
     * The current settings. They can be replaced at runtime, e.g. to lower the level for diagnostics:
     *
     *     logger.settings = logger.settings.copy(level = LogLevel.Debug)
     */
    @Volatile
    open var settings: LogSettings = settings
        set(value) {
            field = value
            resolved = resolveLevel(value)
        }

    // The level for this logger's name, worked out once per settings change, not on every call
    @Volatile
    private var resolved: LogLevel = resolveLevel(settings)

    /**
     * The level this logger uses: the longest matching name in [LogSettings.levels], otherwise
     * [LogSettings.level].
     */
    open val level: LogLevel get() = resolved

    private fun resolveLevel(s: LogSettings): LogLevel {
        if (s.levels.isEmpty()) return s.level
        val match = s.levels.keys.filter { name == it || name.startsWith("$it.") }.maxByOrNull { it.length }
        return if (match == null) s.level else s.levels.getValue(match)
    }

    fun isEnabled(level: LogLevel): Boolean = level != LogLevel.Off && level >= this.level

    /**
     * Escape hatch to the wrapped library's logger, e.g. Logback's own Logger, or null if this logger
     * doesn't wrap one. It is Any because the wrapped types are platform specific and can't be
     * named in common code. Use [LogFactory.provider] for the wrapped library's root object.
     */
    open val raw: Any? = null

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
        if(!isEnabled(level)) return
        val fmsg = when {
            ex == null -> msg
            msg.isNullOrEmpty() -> ex.message
            else -> ex.message?.let { "$msg\n$it" } ?: msg
        }
        logIfEnabled(level, fmsg, ex)
    }

    /**
     * Logs a message that is only built if the level is enabled
     *
     * log(LogLevel.Debug, "updating user") { " some expensive message to build" }
     */
    fun log(level: LogLevel, msg: String? = null, callback: () -> String) {
        logIfEnabled(level, msg, callback)
    }

    /**
     * Logs an entry
     *
     * @param level
     * @param msg
     * @param ex
     */
    private fun logIfEnabled(level: LogLevel, msg: String?, ex: Throwable?) {
        if(isEnabled(level)) {
            val s = settings
            deliver(LogEntry(name, level, msg ?: "", ex, origin = s.origin, scope = s.scope, time = s.clock.now()))
        }
    }

    /**
     * Logs an entry
     *
     * @param level
     * @param ex
     */
    private fun logIfEnabled(level: LogLevel, msg:String?, callback: () -> String) {
        if(isEnabled(level)) {
            val label = msg ?: ""
            val output = callback()
            val s = settings
            deliver(LogEntry(name, level, "$label : $output", origin = s.origin, scope = s.scope, time = s.clock.now()))
        }
    }

    /**
     * Logs an entry with key/value fields. Fields are redacted per [settings] before the
     * entry is created.
     *
     * @param level
     * @param msg
     * @param fields
     * @param ex
     * @param action what was attempted, for structured logs
     */
    private fun logIfEnabled(
        level: LogLevel,
        msg: String?,
        fields: List<Pair<String, Any?>>,
        ex: Throwable? = null,
        action: String? = null
    ) {
        if(isEnabled(level)) {
            val s = settings
            val text = msg ?: ex?.message ?: ""
            deliver(LogEntry(name, level, text, ex, action, s.origin, s.scope, s.redaction.redact(fields), s.clock.now()))
        }
    }

    /**
     * Logs an action. The fields are only built if the level is enabled.
     */
    private fun logIfEnabled(level: LogLevel, action: String, ex: Throwable?, fields: () -> List<Pair<String, Any?>>) {
        if(isEnabled(level)) {
            logIfEnabled(level, null, fields(), ex, action)
        }
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
    fun with(vararg fields: Pair<String, Any?>): Logger = BoundLogger(this, fields.asList())

    /**
     * Adds anything this logger contributes to the entry, e.g. bound fields. Runs before the filter.
     */
    internal open fun decorate(entry: LogEntry): LogEntry = entry

    private fun deliver(entry: LogEntry) {
        val e = decorate(entry)
        if (settings.filter?.invoke(e) != false) emit(e)
    }

    /**
     * Receives every entry that passed the level check and delivers it to an output, e.g. the console
     * or a wrapped library such as Logback. This is the method a provider implements.
     */
    abstract fun emit(entry: LogEntry)
}

/**
 * [Logger.raw] as T, or null if there is none or it is a different type.
 */
inline fun <reified T> Logger.rawAs(): T? = raw as? T
