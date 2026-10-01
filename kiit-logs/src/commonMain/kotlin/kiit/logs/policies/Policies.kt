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
