package kiit.logs

import android.util.Log
import kotlinx.datetime.Instant

// Android before API 26 limits tags to 23 characters
private const val MAX_TAG = 23

internal actual fun consoleWrite(level: LogLevel, tag: String, time: Instant, text: String) {
    val priority = when (level) {
        LogLevel.Debug -> Log.DEBUG
        LogLevel.Info -> Log.INFO
        LogLevel.Warn -> Log.WARN
        LogLevel.Error -> Log.ERROR
        LogLevel.Fatal -> Log.ASSERT
        LogLevel.Off -> Log.INFO
    }
    Log.println(priority, tag.take(MAX_TAG), text)
}
