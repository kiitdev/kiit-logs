package kiit.logs.internal

import android.util.Log
import kiit.logs.LogEntry
import kiit.logs.LogLevel

// Android before API 26 limits tags to 23 characters
private const val MAX_TAG = 23

internal actual fun consoleWrite(entry: LogEntry, text: String, maxLength: Int) {
    val priority =
        when (entry.level) {
            LogLevel.Verbose -> Log.VERBOSE
            LogLevel.Debug -> Log.DEBUG
            LogLevel.Info -> Log.INFO
            LogLevel.Warn -> Log.WARN
            LogLevel.Error -> Log.ERROR
            LogLevel.Fatal -> Log.ASSERT
            LogLevel.Off -> Log.INFO
        }
    // Names can be long ( com.shop.orders.OrderService ), logcat only needs the class part
    val androidTag = entry.name.substringAfterLast('.').take(MAX_TAG)
    // The tag has the name, logcat adds the time and the level, so the message starts with the source
    chunkText("[${entry.source.text}] $text", maxLength).forEach { Log.println(priority, androidTag, it) }
}
