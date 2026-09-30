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
 *
 * @param maxLength longest piece written in one call, in characters. Only Android uses it, because
 *                  logcat cuts a message at about 4000 characters, so a longer text is written as
 *                  several pieces. The JVM and iOS ignore it. 0 means no limit
 */
internal expect fun consoleWrite(level: LogLevel, tag: String, time: Instant, text: String, maxLength: Int)
