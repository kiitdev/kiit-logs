package kiit.logs

internal actual fun consoleWrite(entry: LogEntry, text: String, maxLength: Int) {
    println("${entry.time} [${entry.name}] ${entry.level.name} : $text")
}
