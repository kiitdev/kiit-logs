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
 *     Policy { entry -> if (entry.prefix?.value == "heartbeat") null else entry }
 *     FilterPolicy { it.prefix?.value != "heartbeat" }
 *
 * The policies in [LogSettings.policies] run in list order and stop at the first null. If a policy throws, the
 * entry is dropped and the error goes to the logger's error policy, so an entry whose redaction failed is never
 * delivered.
 */
fun interface Policy {
    fun apply(entry: LogEntry): LogEntry?
}

/**
 * Working with [Policy] lists, e.g. [LogSettings.policies][kiit.logs.LogSettings.policies].
 */
object Policies {
    /**
     * Runs the policies in order, each one getting what the one before it returned. Null if one of them
     * dropped the entry.
     */
    internal fun applyTo(policies: List<Policy>, entry: LogEntry): LogEntry? {
        if (policies.isEmpty()) return entry
        var current = entry
        for (policy in policies) {
            current = policy.apply(current) ?: return null
        }
        return current
    }
}
