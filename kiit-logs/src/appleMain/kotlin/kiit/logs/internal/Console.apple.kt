package kiit.logs.internal

import kiit.logs.LogEntry
import kotlinx.cinterop.BetaInteropApi
import platform.Foundation.NSLog
import platform.Foundation.NSString
import platform.Foundation.create

// NSLog adds its own time and process prefix, so the line has no timestamp of ours.
// The text is an argument to "%@", so a "%" in a message is not read as a format specifier.
// A Kotlin String passed straight to a C vararg is not an NSString, and crashes, so it is wrapped.
@OptIn(BetaInteropApi::class)
internal actual fun consoleWrite(entry: LogEntry, text: String, maxLength: Int) {
    NSLog("%@", NSString.create(string = "[${entry.name}] ${entry.level.name} : $text"))
}
