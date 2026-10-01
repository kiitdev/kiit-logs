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

package kiit.logs.policies

import kiit.logs.LogEntry
import kiit.logs.LogSettings
import kiit.logs.sinks.LogSink
import kotlin.concurrent.Volatile

/**
 * Receives an error from logging, e.g. a sink that is down, a policy with a bug, or a lazy message that fails.
 * Set with [LogSettings.errors]. Logging doesn't throw into your code, unless the handler is [Throw]. The default is
 * [printing], which shows the first few errors.
 *
 * It runs on the calling thread, so keep it quick. If it throws, that is ignored. To ignore errors completely, use
 * a handler that does nothing: `ErrorHandler { _, _, _ -> }`.
 */
fun interface ErrorHandler {
    /**
     * @param stage where it went wrong
     * @param error what was thrown
     * @param entry the entry being logged, or null if it wasn't built yet, or if a policy failed ( the entry may
     *              not be redacted yet )
     */
    fun onError(stage: Stage, error: Exception, entry: LogEntry?)

    /**
     * Where in logging something went wrong.
     */
    enum class Stage {
        /** Building the entry: a lazy message or fields, or rendering the stack trace. */
        Build,

        /** Running the [LogSettings.policies], e.g. redaction or a filter. The error is reported without an entry. */
        Policy,

        /** Delivering the entry to the [LogSink]. */
        Sink,

        /** [LogSink.flush] or [LogSink.close]. */
        Lifecycle
    }

    companion object {
        /**
         * Prints the first [limit] errors to the console, then stays quiet, so a broken sink or policy is noticed
         * without flooding the output. It prints the stage, the logger name and action, and the error type and
         * message, never the field values. This is the default handler.
         */
        fun printing(limit: Int = 3): ErrorHandler =
            object : ErrorHandler {
                // Not exact under threads, at worst one extra line is printed
                @Volatile
                private var printed = 0

                override fun onError(stage: Stage, error: Exception, entry: LogEntry?) {
                    if (printed >= limit) return
                    printed++
                    val where = entry?.let { " in ${it.name} ${it.action ?: ""}".trimEnd() } ?: ""
                    println("kiit-logs: logging failed at $stage$where: ${error::class.simpleName}: ${error.message}")
                    if (printed == limit) println("kiit-logs: more logging failures won't be shown")
                }
            }

        /**
         * Throws the error to the caller of the log method. Useful in tests and development.
         */
        val Throw: ErrorHandler =
            object : ErrorHandler {
                override fun onError(stage: Stage, error: Exception, entry: LogEntry?) {
                    throw error
                }
            }
    }
}
