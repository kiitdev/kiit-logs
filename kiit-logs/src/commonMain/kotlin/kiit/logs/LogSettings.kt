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

import kiit.logs.policies.ErrorPolicy
import kiit.logs.policies.LogErrorHandler
import kiit.logs.policies.Policy
import kiit.logs.policies.RedactPolicy
import kotlinx.datetime.Clock

/**
 * Settings for a [Logger]. Passed as the primary constructor argument so new options
 * can be added here without changing every logger's constructor.
 *
 * The level, stack trace and policy settings are required, so a logger's behavior is always
 * a deliberate choice. Use [safe] for the safe defaults, and copy() to change one:
 *
 *     LogSettings.safe(origin = "shop.example.com").copy(level = LogLevel.Info)
 *
 * @param level minimum level that is logged
 * @param stackTraces how exceptions are rendered by loggers that print them, e.g. the console
 * @param policies what happens to every entry before a sink gets it, in list order, e.g. redaction and
 *                 filters. [RedactPolicy] is the default, add a [Policy.filter] after it or your own [Policy]. An
 *                 empty list delivers entries as they are
 * @param origin who owns the system that emits the logs, set once for the app, e.g. "shop.example.com".
 *               A domain or any other stable id. Same convention as origin in kiit-codes and
 *               kiit-service-id. Empty means unset
 * @param scope free-form label for where in the origin this is, e.g. "orders.checkout". Dots express
 *              hierarchy. Same convention as scope in kiit-codes and kiit-service-id. Empty means unset
 * @param levels levels for logger names, e.g. "com.shop.orders" to Debug. A logger uses the longest name
 *               that equals its name or is a prefix ending at a dot, otherwise [level]
 * @param maxTraceLines cap on the lines of a full stack trace
 * @param errors what happens when something in logging throws, see [ErrorPolicy]. By default the first few
 *               errors are printed and logging never throws into your code
 * @param clock supplies the time of each entry. Replace it in tests to get exact times
 */
data class LogSettings(
    val level: LogLevel,
    val stackTraces: StackTraces,
    val policies: List<Policy>,
    val origin: String = "",
    val scope: String = "",
    val clock: Clock = Clock.System,
    val maxTraceLines: Int = StackTraces.DEFAULT_MAX_LINES,
    val levels: Map<String, LogLevel> = emptyMap(),
    val errors: ErrorPolicy = ErrorPolicy.Handle(LogErrorHandler.printing())
) {
    /**
     * The level for a logger name: the longest name in [levels] that equals it or is a prefix ending
     * at a dot, otherwise [level].
     */
    fun levelFor(name: String): LogLevel {
        if (levels.isEmpty()) return level
        val match = levels.keys.filter { name == it || name.startsWith("$it.") }.maxByOrNull { it.length }
        return if (match == null) level else levels.getValue(match)
    }

    companion object {
        /**
         * Safe defaults: only [LogLevel.Error] and above, no stack traces, and default redaction
         * ( sensitive keys are masked ).
         */
        fun safe(origin: String = "", scope: String = ""): LogSettings =
            LogSettings(
                level = LogLevel.Error,
                stackTraces = StackTraces.Off,
                policies = listOf(RedactPolicy()),
                origin = origin,
                scope = scope,
            )
    }
}
