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

import kiit.logs.sinks.ConsoleSink
import kiit.logs.sinks.LogSink

/**
 * One import to get a [LogFactory]. It holds no state, each call returns a new factory, and the
 * settings are always passed in:
 *
 *     val logFactory = Logs.console(LogSettings.safe(origin = "shop.example.com"))
 *     val log = logFactory.getLogger(OrderService::class)
 */
object Logs {
    /**
     * Loggers that print to the console. [LogFactory.provider] is the [ConsoleSink].
     * @param maxLength see [ConsoleSink]
     */
    fun console(settings: LogSettings, maxLength: Int = ConsoleSink.DEFAULT_MAX_LENGTH): LogFactory =
        SinkLogFactory(settings, ConsoleSink(maxLength))

    /**
     * Loggers that send every entry to [sink], see [SinkLogFactory].
     */
    fun sink(settings: LogSettings, sink: LogSink): LogFactory = SinkLogFactory(settings, sink)
}
