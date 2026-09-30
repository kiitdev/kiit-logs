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

    fun isEnabled(level: LogLevel): Boolean = level >= this.level

    override val logger: Logger? by lazy { this }
    open val raw: Any? = null

    /**
     * Logs an entry
     *
     * @param level
     * @param msg
     * @param ex
     */
    fun performLog(level: LogLevel, msg: String?, ex: Throwable?) {
        if(level >= this.level) {
            log(LogEntry(name, level, msg ?: "", ex))
        }
    }

    /**
     * Logs an entry
     *
     * @param level
     * @param ex
     */
    fun performLog(level: LogLevel, msg:String?, callback: () -> String) {
        if(level >= this.level) {
            val label = msg ?: ""
            val output = callback()
            log(LogEntry(name, level, "$label : $output"))
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
     */
    fun performLog(level: LogLevel, msg: String?, fields: List<Pair<String, Any?>>, ex: Throwable? = null) {
        if(level >= this.level) {
            log(LogEntry(name, level, msg ?: "", ex, fields = settings.redaction.apply(fields)))
        }
    }

    abstract fun log(entry: LogEntry)
}
