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
 * Controls how much of an exception a logger prints. Stack traces can expose internals
 * and data, so the default in [LogSettings] is [Off].
 * The exception message is always part of the logged message, this only controls the type and trace.
 * A provider that ships entries to a secure backend can ignore this and forward the Throwable.
 */
enum class StackTraces {
    /** Nothing beyond the message text. */
    Off,

    /** Exception type and message, then the same for each cause. */
    Summary,

    /** The full stack trace, cut to a maximum number of lines. */
    Full;

    /**
     * @param ex the exception to render
     * @param maxLines cap for [Full], the rest is replaced by a line saying how many were cut
     */
    fun render(ex: Throwable, maxLines: Int = DEFAULT_MAX_LINES): String? = when (this) {
        Off -> null
        Summary -> generateSequence(ex) { it.cause }
            .take(MAX_CAUSES)
            .joinToString("\nCaused by: ") { "${it::class.simpleName}: ${it.message}" }
        Full -> {
            val lines = ex.stackTraceToString().lines()
            if (lines.size <= maxLines) {
                lines.joinToString("\n")
            } else {
                lines.take(maxLines).joinToString("\n") + "\n... ${lines.size - maxLines} more lines"
            }
        }
    }

    companion object {
        const val DEFAULT_MAX_LINES = 50

        // Bounds the walk so a cause cycle can't loop
        private const val MAX_CAUSES = 5
    }
}
