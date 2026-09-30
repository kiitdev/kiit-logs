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

sealed class LogLevel(val name: String, val code: Int) {
    operator fun compareTo(lv: LogLevel): Int = this.code.compareTo(lv.code)

    /** Finest detail, step by step. Below Debug. Use logAction(LogLevel.Trace, ...) to log at it. */
    object Trace : LogLevel("Trace", 0)

    object Debug : LogLevel("Debug", 1)

    object Info : LogLevel("Info", 2)

    object Warn : LogLevel("Warn", 3)

    object Error : LogLevel("Error", 4)

    object Fatal : LogLevel("Fatal", 5)

    /** Not a level to log at. Set as a logger's minimum level to turn logging off. */
    object Off : LogLevel("Off", 6)
}
