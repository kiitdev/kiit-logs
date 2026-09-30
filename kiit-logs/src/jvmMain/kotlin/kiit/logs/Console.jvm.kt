package kiit.logs

import kotlinx.datetime.Instant

internal actual fun consoleWrite(level: LogLevel, tag: String, time: Instant, text: String) {
    println("$time [$tag] ${level.name} : $text")
}
