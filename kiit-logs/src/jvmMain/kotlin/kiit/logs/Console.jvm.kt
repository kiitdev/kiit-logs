package kiit.logs

import kotlinx.datetime.Instant

internal actual fun consoleWrite(level: LogLevel, tag: String, time: Instant, text: String, maxLength: Int) {
    println("$time [$tag] ${level.name} : $text")
}
