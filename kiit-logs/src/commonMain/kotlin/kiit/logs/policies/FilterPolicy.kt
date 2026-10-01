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
 * A [Policy] that drops the entries [keep] returns false for, e.g. to silence a noisy action:
 *
 *     FilterPolicy { it.action != "heartbeat" }
 *
 * Put it after [RedactPolicy] in [LogSettings.policies], so it only sees redacted fields.
 */
class FilterPolicy(private val keep: (LogEntry) -> Boolean) : Policy {
    override fun apply(entry: LogEntry): LogEntry? = if (keep(entry)) entry else null
}
