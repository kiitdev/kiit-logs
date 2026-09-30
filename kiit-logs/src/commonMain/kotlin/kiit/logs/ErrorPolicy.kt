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
}

/**
 * What happens when something in logging throws, e.g. a sink that is down, a filter or redactor with a
 * bug, or a lazy message that fails. Set with [LogSettings.errors].
 */
sealed class ErrorPolicy {

    /** Ignore it. Logging never throws into your code. This is the default. */
    object Swallow : ErrorPolicy()

    /** Throw it to the caller of the log method. Useful in tests and development. */
    object Propagate : ErrorPolicy()

    /** Give it to a [LogErrorHandler], e.g. to print it once or send it to a crash reporter. */
    class Handle(val handler: LogErrorHandler) : ErrorPolicy()
}

internal fun ErrorPolicy.report(stage: LogStage, error: Exception, entry: LogEntry?) {
    when (this) {
        ErrorPolicy.Swallow -> Unit
        ErrorPolicy.Propagate -> throw error
        is ErrorPolicy.Handle -> try {
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
