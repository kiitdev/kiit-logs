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
 * A provider that ships entries to a secure backend can ignore this and forward the Throwable.
 */
enum class StackTraces {
    /** Nothing beyond the message text. */
    Off,

    /** Exception type and message on one line. */
    Summary,

    /** The full stack trace. */
    Full;

    fun render(ex: Throwable): String? = when (this) {
        Off -> null
        Summary -> "${ex::class.simpleName}: ${ex.message}"
        Full -> ex.stackTraceToString()
    }
}
