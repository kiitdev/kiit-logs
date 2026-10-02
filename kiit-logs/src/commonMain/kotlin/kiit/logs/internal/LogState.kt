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
 * with [Logger.with] share one, and the loggers of one factory share the same [SettingsRef], so a change
 * applies to all of them at once.
 */
internal class LogState(private val source: SettingsRef, private val name: String) {
    // Worked out again only when the settings were replaced, not on every call. The settings and the level in
    // it always belong together, so the settings are read once
    @Volatile
    private var worked: Worked = source.get().let { Worked(it, it.levelFor(name)) }

    var settings: LogSettings
        get() = source.get()
        set(value) = source.set(value)

    val level: LogLevel
        get() {
            val current = source.get()
            val known = worked
            if (known.settings === current) return known.level
            val fresh = Worked(current, current.levelFor(name))
            worked = fresh
            return fresh.level
        }

    private class Worked(val settings: LogSettings, val level: LogLevel)
}
