/**
 *  <kiit_header>
 * url: www.kiit.dev
 * git: www.github.com/slatekit/kiit
 * org: www.codehelix.co
 * author: Kishore Reddy
 * copyright: 2016 CodeHelix Solutions Inc.
 * license: refer to website and/or github
 *  </kiit_header>
 */

package kiit.logs

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * @param name logger name, e.g. the class
 * @param action what was attempted, e.g. "place". Set for structured logs
 * @param origin which system emitted the entry, from [LogSettings.origin]
 * @param scope namespace within the origin, from [LogSettings.scope], e.g. "orders.checkout"
 * @param fields key/value pairs, already redacted
 * @param msg free text, used when there is no action or as detail for one
 */
data class LogEntry(
    val name: String = "",
    val level: LogLevel,
    val msg: String = "",
    val ex: Throwable? = null,
    val action: String? = null,
    val origin: String = "",
    val scope: String = "",
    val fields: List<Pair<String, Any?>> = emptyList(),
    val time: Instant = Clock.System.now()
)