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

import kotlinx.datetime.Instant

/**
 * Writes a line to the platform console. JVM and iOS print "time [tag] Level : text".
 * Android writes to logcat with the level and tag, since logcat adds its own time and level.
 */
internal expect fun consoleWrite(level: LogLevel, tag: String, time: Instant, text: String)
