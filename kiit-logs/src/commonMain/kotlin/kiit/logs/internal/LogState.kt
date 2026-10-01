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

import kiit.logs.LogLevel
import kiit.logs.LogSettings
import kiit.logs.Logger
import kotlin.concurrent.Volatile

/**
 * The settings of a logger and the level worked out from them. A logger and the loggers made from it
 * with [Logger.with] share one, so a change applies to all of them.
 */
internal class LogState(settings: LogSettings, private val name: String) {
    @Volatile
    var settings: LogSettings = settings
        set(value) {
            field = value
            level = value.levelFor(name)
        }

    // Worked out once per settings change, not on every call
    @Volatile
    var level: LogLevel = settings.levelFor(name)
        private set
}
