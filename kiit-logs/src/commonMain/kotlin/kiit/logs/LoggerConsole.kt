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

/**
 * Lightweight logger that prints to the console
 * using the ConsoleWriter in slate kit which has support
 * for colors and text semantics.
 * This is just used mostly for defaults.
 * You should be using the kiit.providers module with support for logback
 */
class LoggerConsole(
    settings: LogSettings = LogSettings(),
    name: String = "console",
    logType: KClass<*>? = null
) : Logger(settings, name, logType) {

    /**
     * Convenience constructor for when only the level is customized.
     */
    constructor(
        level: LogLevel,
        name: String = "console",
        logType: KClass<*>? = null
    ) : this(LogSettings(level = level), name, logType)

    /**
     * Logs to the console
     *
     * @param entry: 
     */
    override fun log(entry: LogEntry) {
        // e.g. "orders.checkout place, order_id=abc, total=42". Origin is app-wide, so it is not printed
        val what = listOf(entry.scope, entry.action ?: "").filter { it.isNotEmpty() }.joinToString(" ")
        val text = listOf(what, entry.msg, LogUtils.render(entry.fields))
            .filter { it.isNotEmpty() }
            .joinToString(", ")
        println("${entry.time} [$name] ${entry.level.name} : $text")
        entry.ex?.let { ex -> settings.stackTraces.render(ex)?.let { println(it) } }
    }
}
