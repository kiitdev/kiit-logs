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

package kiit.logs.internal

import kiit.logs.LogEntry
import kiit.logs.policies.ErrorHandler

/**
 * Runs logging steps so that an error goes to the [ErrorHandler] and doesn't reach the caller.
 */
internal object ErrorGuard {
    /**
     * Hands the error to the handler. [ErrorHandler.Throw] is recognized here and rethrows, any other handler
     * that throws is ignored, since nothing can report a failure of the reporter.
     */
    fun report(
        handler: ErrorHandler,
        stage: ErrorHandler.Stage,
        error: Exception,
        entry: LogEntry?
    ) {
        if (handler === ErrorHandler.Throw) throw error
        try {
            handler.onError(stage, error, entry)
        } catch (ignored: Exception) {
            // Nothing can report a failure of the reporter
        }
    }

    /**
     * Runs the block. If it throws, the handler decides what happens, and the result is null when it doesn't rethrow.
     * It catches any Exception on purpose, since a sink, policy or lazy message can throw anything.
     */
    @Suppress("TooGenericExceptionCaught")
    inline fun <T> guard(
        handler: ErrorHandler,
        stage: ErrorHandler.Stage,
        entry: LogEntry?,
        block: () -> T
    ): T? =
        try {
            block()
        } catch (e: Exception) {
            report(handler, stage, e, entry)
            null
        }
}
