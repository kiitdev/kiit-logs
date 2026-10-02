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

import kiit.logs.LogSettings
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

/**
 * The settings that a [kiit.logs.Logs] and all of its loggers read. Replacing them is one atomic step, so every
 * logger switches together, and an [update] never loses another thread's change.
 */
@OptIn(ExperimentalAtomicApi::class)
internal class SettingsRef(initial: LogSettings) {
    private val ref = AtomicReference(initial)

    fun get(): LogSettings = ref.load()

    fun set(value: LogSettings) = ref.store(value)

    /**
     * Applies [change] to the latest settings and stores the result. If another thread changed them in the
     * meantime, it is applied again to what that thread stored, so no change is lost.
     */
    fun update(change: (LogSettings) -> LogSettings): LogSettings {
        while (true) {
            val old = ref.load()
            val new = change(old)
            if (ref.compareAndSet(old, new)) return new
        }
    }
}
