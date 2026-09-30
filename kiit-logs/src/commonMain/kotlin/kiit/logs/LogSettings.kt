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
 * Settings for a [Logger]. Passed as the primary constructor argument so new options
 * can be added here without changing every logger's constructor.
 *
 * @param level minimum level that is logged
 * @param stackTraces how exceptions are rendered by loggers that print them, e.g. the console
 * @param redaction which key/value fields are masked or dropped before an entry is created
 * @param origin which system emits the logs, set once for the app, e.g. "shop.example.com".
 *               Same meaning as origin in kiit-codes. Empty means unset
 * @param scope namespace within the origin, e.g. "orders.checkout". Empty means unset
 */
data class LogSettings(
    val level: LogLevel = LogLevel.Error,
    val stackTraces: StackTraces = StackTraces.Off,
    val redaction: Redaction = Redaction(),
    val origin: String = "",
    val scope: String = ""
)
