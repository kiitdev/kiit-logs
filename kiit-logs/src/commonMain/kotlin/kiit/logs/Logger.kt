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

abstract class Logger(
    settings: LogSettings,
    open val name: String = "",
    open val logType: KClass<*>? = null
) : LogSupport {

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

    open val level: LogLevel get() = settings.level

    fun isEnabled(level: LogLevel): Boolean = level != LogLevel.Off && level >= this.level

    override val logger: Logger get() = this

    /**
     * Escape hatch to the wrapped library's logger, e.g. Logback's own Logger, or null if this logger
     * doesn't wrap one. It is Any because the wrapped types are platform specific and can't be
     * named in common code. Use [LogFactory.provider] for the wrapped library's root object.
     */
    open val raw: Any? = null

    /**
     * Logs an entry
     *
     * @param level
     * @param msg
     * @param ex
     */
    fun logIfEnabled(level: LogLevel, msg: String?, ex: Throwable?) {
        if(isEnabled(level)) {
            val s = settings
            emit(LogEntry(name, level, msg ?: "", ex, origin = s.origin, scope = s.scope, time = s.clock.now()))
        }
    }

    /**
     * Logs an entry
     *
     * @param level
     * @param ex
     */
    fun logIfEnabled(level: LogLevel, msg:String?, callback: () -> String) {
        if(isEnabled(level)) {
            val label = msg ?: ""
            val output = callback()
            val s = settings
            emit(LogEntry(name, level, "$label : $output", origin = s.origin, scope = s.scope, time = s.clock.now()))
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
    fun logIfEnabled(
        level: LogLevel,
        msg: String?,
        fields: List<Pair<String, Any?>>,
        ex: Throwable? = null,
        action: String? = null
    ) {
        if(isEnabled(level)) {
            val s = settings
            val text = msg ?: ex?.message ?: ""
            emit(LogEntry(name, level, text, ex, action, s.origin, s.scope, s.redaction.redact(fields), s.clock.now()))
        }
    }

    /**
     * Logs an action. The fields are only built if the level is enabled.
     */
    fun logIfEnabled(level: LogLevel, action: String, ex: Throwable?, fields: () -> List<Pair<String, Any?>>) {
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
     * Receives every entry that passed the level check and delivers it to an output, e.g. the console
     * or a wrapped library such as Logback. This is the method a provider implements.
     */
    abstract fun emit(entry: LogEntry)
}

/**
 * [Logger.raw] as T, or null if there is none or it is a different type.
 */
inline fun <reified T> Logger.rawAs(): T? = raw as? T
