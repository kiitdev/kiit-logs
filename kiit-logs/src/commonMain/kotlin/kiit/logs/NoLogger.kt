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

private object NoSink : LogSink {
    override fun emit(entry: LogEntry) = Unit
}

/**
 * A logger that discards everything. Use it where logging is optional, instead of a null logger,
 * e.g. class OrderService(private val log: Logger = NoLogger)
 */
val NoLogger: Logger = Logger(LogSettings.safe().copy(level = LogLevel.Off), "none", NoSink)
