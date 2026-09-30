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

/**
 * Where in logging something went wrong.
 */
enum class LogStage {
    /** Building the entry: a lazy message or fields, redaction, or rendering the stack trace. */
    Build,

    /** The [LogSettings.filter]. */
    Filter,

    /** Delivering the entry to the [LogSink]. */
    Sink,

    /** [LogSink.flush] or [LogSink.close]. */
    Lifecycle
}

/**
 * Receives an error from logging, when [ErrorPolicy.Handle] is the policy. It runs on the calling
 * thread, so keep it quick. If it throws, that is ignored.
 */
fun interface LogErrorHandler {
    /**
     * @param stage where it went wrong
     * @param error what was thrown
     * @param entry the entry being logged, or null if it wasn't built yet
     */
    fun onError(stage: LogStage, error: Exception, entry: LogEntry?)

    companion object {
        /**
         * Prints the first [limit] errors to the console, then stays quiet, so a broken sink, filter or
         * redactor is noticed without flooding the output. It prints the stage, the logger name and action,
         * and the error type and message, never the field values. This is the default handler.
         * To ignore errors completely, use a handler that does nothing: `ErrorPolicy.Handle { _, _, _ -> }`.
         */
        fun printing(limit: Int = 3): LogErrorHandler = PrintingHandler(limit)
    }
}

private class PrintingHandler(private val limit: Int) : LogErrorHandler {
    // Not exact under threads, at worst one extra line is printed
    @Volatile
    private var printed = 0

    override fun onError(stage: LogStage, error: Exception, entry: LogEntry?) {
        if (printed >= limit) return
        printed++
        val where = entry?.let { " in ${it.name} ${it.action ?: ""}".trimEnd() } ?: ""
        println("kiit-logs: logging failed at $stage$where: ${error::class.simpleName}: ${error.message}")
        if (printed == limit) println("kiit-logs: more logging failures won't be shown")
    }
}

/**
 * What happens when something in logging throws, e.g. a sink that is down, a filter or redactor with a
 * bug, or a lazy message that fails. Set with [LogSettings.errors].
 */
sealed class ErrorPolicy {
    /** Throw it to the caller of the log method. Useful in tests and development. */
    object Propagate : ErrorPolicy()

    /**
     * Give it to a [LogErrorHandler], e.g. to print it or send it to a crash reporter. Logging doesn't throw
     * into your code. The default is [LogErrorHandler.printing], which shows the first few errors.
     */
    class Handle(val handler: LogErrorHandler) : ErrorPolicy()
}

internal fun ErrorPolicy.report(stage: LogStage, error: Exception, entry: LogEntry?) {
    when (this) {
        ErrorPolicy.Propagate -> throw error
        is ErrorPolicy.Handle ->
            try {
                handler.onError(stage, error, entry)
            } catch (ignored: Exception) {
                // Nothing can report a failure of the reporter
            }
    }
}

/**
 * Runs the block. If it throws, the policy decides what happens, and the result is null when it doesn't propagate.
 */
internal inline fun <T> ErrorPolicy.guard(stage: LogStage, entry: LogEntry?, block: () -> T): T? =
    try {
        block()
    } catch (e: Exception) {
        report(stage, e, entry)
        null
    }
