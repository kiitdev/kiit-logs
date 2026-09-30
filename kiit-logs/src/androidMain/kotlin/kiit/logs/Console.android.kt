package kiit.logs

import android.util.Log

// Android before API 26 limits tags to 23 characters
private const val MAX_TAG = 23

internal actual fun consoleWrite(entry: LogEntry, text: String, maxLength: Int) {
    val priority = when (entry.level) {
        LogLevel.Trace -> Log.VERBOSE
        LogLevel.Debug -> Log.DEBUG
        LogLevel.Info -> Log.INFO
        LogLevel.Warn -> Log.WARN
        LogLevel.Error -> Log.ERROR
        LogLevel.Fatal -> Log.ASSERT
        LogLevel.Off -> Log.INFO
    }
    // Names can be long ( com.shop.orders.OrderService ), logcat only needs the class part
    val androidTag = entry.name.substringAfterLast('.').take(MAX_TAG)
    chunkText(text, maxLength).forEach { Log.println(priority, androidTag, it) }
}
