package kiit.logs.internal

import kiit.logs.LogEntry

internal actual fun consoleWrite(entry: LogEntry, text: String, maxLength: Int) {
    println("${entry.time} [${entry.source.text}] ${entry.level.name} : $text")
}
