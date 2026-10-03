package kiit.logs.internal

import platform.Foundation.NSThread

internal actual fun currentThreadName(): String {
    val name = NSThread.currentThread.name
    return when {
        !name.isNullOrEmpty() -> name
        NSThread.isMainThread -> "main"
        else -> ""
    }
}
