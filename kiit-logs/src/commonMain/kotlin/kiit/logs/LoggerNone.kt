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
 * A logger that discards everything. Use it where logging is optional, instead of a null logger,
 * e.g. override val logger: Logger = LoggerNone
 */
object LoggerNone : Logger(LogSettings.safe().copy(level = LogLevel.Off), "none") {

    override fun log(entry: LogEntry) = Unit
}
