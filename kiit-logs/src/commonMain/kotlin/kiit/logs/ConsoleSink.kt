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
 * Prints entries to the console: logcat on Android, standard output on the JVM and iOS.
 * For development, tests and small apps. Nothing is buffered here, so flush and close do nothing.
 *
 * @param maxLength on Android, the longest text written in one logcat call, in characters. Longer entries
 *                  are written as several calls, on line ends where possible. Logcat cuts a message at
 *                  about 4000 bytes, so lower this for text with many non Latin characters. Other
 *                  platforms ignore it. 0 means no limit
 */
class ConsoleSink(private val maxLength: Int = DEFAULT_MAX_LENGTH) : LogSink {

    override fun emit(entry: LogEntry) {
        // The trace is part of the same write, so multi-line output stays together
        val text = if (entry.trace == null) entry.text else "${entry.text}\n${entry.trace}"
        consoleWrite(entry.level, entry.name, entry.time, text, maxLength)
    }

    companion object {
        const val DEFAULT_MAX_LENGTH = 4000
    }
}
