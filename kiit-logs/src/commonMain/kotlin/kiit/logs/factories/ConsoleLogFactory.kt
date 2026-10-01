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

package kiit.logs.factories

import kiit.logs.LogSettings
import kiit.logs.sinks.ConsoleSink

/**
 * Creates loggers that print to the console. Simple default, use [SinkLogFactory] with your own
 * sink when you need more.
 *
 * kiit-logs has only 1 dependency (kotlinx-datetime).
 *
 *     val logFactory = ConsoleLogFactory(LogSettings.safe(origin = "shop.example.com"))
 *
 * @param maxLength see [ConsoleSink]
 */
class ConsoleLogFactory(
    settings: LogSettings,
    maxLength: Int = ConsoleSink.DEFAULT_MAX_LENGTH
) : SinkLogFactory(settings, ConsoleSink(maxLength), "console")
