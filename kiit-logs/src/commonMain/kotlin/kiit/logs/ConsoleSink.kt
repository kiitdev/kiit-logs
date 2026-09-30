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
 */
class ConsoleSink : LogSink {

    override fun emit(entry: LogEntry) {
        // The trace is part of the same write, so multi-line output stays together
        val text = if (entry.trace == null) entry.text else "${entry.text}\n${entry.trace}"
        consoleWrite(entry.level, entry.name, entry.time, text)
    }
}
