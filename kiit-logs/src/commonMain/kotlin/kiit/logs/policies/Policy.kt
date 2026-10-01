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

package kiit.logs.policies

import kiit.logs.LogEntry
import kiit.logs.LogSettings

/**
 * A step every entry goes through before it is delivered, e.g. redacting fields or dropping noise.
 * Return the entry to continue with, the same one or a changed copy, or null to drop it.
 *
 *     Policy { entry -> if (entry.action == "heartbeat") null else entry }
 *     Policy.filter { it.action != "heartbeat" }
 *
 * The policies in [LogSettings.policies] run in list order and stop at the first null. If a policy throws, the
 * entry is dropped and the error goes to the logger's error policy, so an entry whose redaction failed is never
 * delivered.
 */
fun interface Policy {
    fun apply(entry: LogEntry): LogEntry?

    companion object {
        /**
         * A policy that drops the entries [keep] returns false for.
         */
        fun filter(keep: (LogEntry) -> Boolean): Policy = Policy { if (keep(it)) it else null }
    }
}

/**
 * Runs the policies in order. Null if one of them dropped the entry.
 */
internal fun List<Policy>.applyTo(entry: LogEntry): LogEntry? {
    if (isEmpty()) return entry
    var current = entry
    for (policy in this) {
        current = policy.apply(current) ?: return null
    }
    return current
}
