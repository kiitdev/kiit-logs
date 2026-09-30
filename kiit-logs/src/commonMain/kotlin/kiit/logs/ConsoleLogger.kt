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
class ConsoleLogger(
    settings: LogSettings,
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
    ) : this(LogSettings.safe().copy(level = level), name, logType)

    /**
     * Logs to the console
     *
     * @param entry: 
     */
    override fun emit(entry: LogEntry) {
        // The exception is part of the same write, so multi-line output stays together
        val trace = entry.ex?.let { settings.stackTraces.render(it, settings.maxTraceLines) }
        consoleWrite(entry.level, name, entry.time, if (trace == null) entry.text else "${entry.text}\n$trace")
    }
}
