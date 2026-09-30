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

import kotlin.reflect.KClass

abstract class Logger(
    open val settings: LogSettings = LogSettings(),
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
    ) : this(LogSettings(level = level), name, logType)

    open val level: LogLevel get() = settings.level

    fun isEnabled(level: LogLevel): Boolean = level != LogLevel.Off && level >= this.level

    override val logger: Logger get() = this
    open val raw: Any? = null

    /**
     * Logs an entry
     *
     * @param level
     * @param msg
     * @param ex
     */
    fun performLog(level: LogLevel, msg: String?, ex: Throwable?) {
        if(isEnabled(level)) {
            log(LogEntry(name, level, msg ?: "", ex, origin = settings.origin, scope = settings.scope))
        }
    }

    /**
     * Logs an entry
     *
     * @param level
     * @param ex
     */
    fun performLog(level: LogLevel, msg:String?, callback: () -> String) {
        if(isEnabled(level)) {
            val label = msg ?: ""
            val output = callback()
            log(LogEntry(name, level, "$label : $output", origin = settings.origin, scope = settings.scope))
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
    fun performLog(
        level: LogLevel,
        msg: String?,
        fields: List<Pair<String, Any?>>,
        ex: Throwable? = null,
        action: String? = null
    ) {
        if(isEnabled(level)) {
            val text = msg ?: ex?.message ?: ""
            log(LogEntry(name, level, text, ex, action, settings.origin, settings.scope, settings.redaction.apply(fields)))
        }
    }

    /**
     * Logs an action. The fields are only built if the level is enabled.
     */
    fun performLog(level: LogLevel, action: String, ex: Throwable?, fields: () -> List<Pair<String, Any?>>) {
        if(isEnabled(level)) {
            performLog(level, null, fields(), ex, action)
        }
    }

    abstract fun log(entry: LogEntry)
}
