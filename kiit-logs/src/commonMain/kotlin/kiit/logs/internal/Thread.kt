package kiit.logs.internal

/**
 * Name of the calling thread, for [kiit.logs.LogEntry.thread]. The thread name on the JVM and Android,
 * the NSThread name on Apple, "main" for the Apple main thread when it has no name. Empty when there is no name.
 */
internal expect fun currentThreadName(): String
