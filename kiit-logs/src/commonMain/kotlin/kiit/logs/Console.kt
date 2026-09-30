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
 * Writes an entry to the platform console. JVM and iOS print "time [name] Level : text".
 * Android writes to logcat with the level and the name as the tag, since logcat adds its own time and level.
 * The entry is passed whole, so a platform can use more of it, e.g. the scope or the fields.
 *
 * @param entry the entry being written
 * @param text the display text to write, already built from the entry and its trace
 * @param maxLength longest piece written in one call, in characters. Only Android uses it, because
 *                  logcat cuts a message at about 4000 characters, so a longer text is written as
 *                  several pieces. The JVM and iOS ignore it. 0 means no limit
 */
internal expect fun consoleWrite(entry: LogEntry, text: String, maxLength: Int)
