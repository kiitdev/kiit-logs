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
 * @param origin who owns the system that emits the logs, set once for the app, e.g. "shop.example.com".
 *               A domain or any other stable id. Same convention as origin in kiit-codes and
 *               kiit-service-id. Empty means unset
 * @param scope free-form label for where in the origin this is, e.g. "orders.checkout". Dots express
 *              hierarchy. Same convention as scope in kiit-codes and kiit-service-id. Empty means unset
 */
data class LogSettings(
    val level: LogLevel = LogLevel.Error,
    val stackTraces: StackTraces = StackTraces.Off,
    val redaction: Redaction = Redaction(),
    val origin: String = "",
    val scope: String = ""
)
