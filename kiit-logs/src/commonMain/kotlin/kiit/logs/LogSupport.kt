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
 * Logging methods. Structured logging is the default style, log an action with key/value fields:
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
 */
interface LogSupport {

    /**
     * The logger that receives all entries. Use [LoggerNone] to turn logging off explicitly.
     */
    val logger: Logger

    /** =====================================================================
     * Structured logging: an action with key/value fields ( redacted by the logger's settings )
     * ======================================================================
     */
    fun debug(action: String, vararg fields: Pair<String, Any?>) = action(LogLevel.Debug, action, null, fields)
    fun info (action: String, vararg fields: Pair<String, Any?>) = action(LogLevel.Info , action, null, fields)
    fun warn (action: String, vararg fields: Pair<String, Any?>) = action(LogLevel.Warn , action, null, fields)
    fun error(action: String, vararg fields: Pair<String, Any?>) = action(LogLevel.Error, action, null, fields)
    fun fatal(action: String, vararg fields: Pair<String, Any?>) = action(LogLevel.Fatal, action, null, fields)

    /** =====================================================================
     * Structured logging with an exception
     * ======================================================================
     */
    fun debug(action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = action(LogLevel.Debug, action, ex, fields)
    fun info (action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = action(LogLevel.Info , action, ex, fields)
    fun warn (action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = action(LogLevel.Warn , action, ex, fields)
    fun error(action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = action(LogLevel.Error, action, ex, fields)
    fun fatal(action: String, ex: Throwable?, vararg fields: Pair<String, Any?>) = action(LogLevel.Fatal, action, ex, fields)

    /** =====================================================================
     * Structured logging, lazy: fields are only built if the level is enabled
     * ======================================================================
     */
    fun debug(action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) = logger.performLog(LogLevel.Debug, action, ex, fields)
    fun info (action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) = logger.performLog(LogLevel.Info , action, ex, fields)
    fun warn (action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) = logger.performLog(LogLevel.Warn , action, ex, fields)
    fun error(action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) = logger.performLog(LogLevel.Error, action, ex, fields)
    fun fatal(action: String, ex: Throwable? = null, fields: () -> List<Pair<String, Any?>>) = logger.performLog(LogLevel.Fatal, action, ex, fields)

    /**
     * Logs an action at any level
     */
    fun action(level: LogLevel, action: String, ex: Throwable?, fields: Array<out Pair<String, Any?>>) {
        logger.performLog(level, null, fields.asList(), ex, action)
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
        if(!logger.isEnabled(level)) return
        val hasMsg = !msg.isNullOrEmpty()
        val hasEx = ex != null
        var fmsg = msg
        if(!hasMsg && hasEx) fmsg = ex?.message
        if(hasMsg && hasEx) fmsg += "\n" + ex?.message
        logger.performLog(level, fmsg, ex)
    }

    /**
     * Logs a message that is only built if the level is enabled
     *
     * log(LogLevel.Debug, "updating user") { " some expensive message to build" }
     */
    fun log(level: LogLevel, msg: String? = null, callback: () -> String) {
        logger.performLog(level, msg, callback)
    }
}
