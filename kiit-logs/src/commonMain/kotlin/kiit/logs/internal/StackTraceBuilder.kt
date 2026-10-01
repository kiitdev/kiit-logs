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

import kiit.logs.StackTraces

/**
 * Renders an exception for [StackTraces], the setting a logger uses to decide how much of it is kept.
 */
internal object StackTraceBuilder {
    // Bounds the walk so a cause cycle can't loop
    private const val MAX_CAUSES = 5

    /**
     * @param mode how much to render
     * @param ex the exception to render
     * @param maxLines cap for [StackTraces.Full], the rest is replaced by a line saying how many were cut
     */
    fun render(mode: StackTraces, ex: Throwable, maxLines: Int = StackTraces.DEFAULT_MAX_LINES): String? =
        when (mode) {
            StackTraces.Off -> null
            StackTraces.Summary ->
                generateSequence(ex) { it.cause }
                    .take(MAX_CAUSES)
                    .joinToString("\nCaused by: ") { "${it::class.simpleName}: ${it.message}" }
            StackTraces.Full -> {
                val lines = ex.stackTraceToString().lines()
                if (lines.size <= maxLines) {
                    lines.joinToString("\n")
                } else {
                    lines.take(maxLines).joinToString("\n") + "\n... ${lines.size - maxLines} more lines"
                }
            }
        }
}
