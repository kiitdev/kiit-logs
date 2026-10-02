package kiit.logs

import kiit.logs.policies.ErrorHandler
import kiit.logs.sinks.LogSink
import kotlin.time.Clock
import kotlin.time.Instant

class MemorySink : LogSink {
    val entries = mutableListOf<LogEntry>()
    var flushed = 0
    var closed = 0

    override fun emit(entry: LogEntry) {
        entries.add(entry)
    }

    override fun flush() {
        flushed++
    }

    override fun close() {
        closed++
    }
}

class FailingSink(private val message: String = "sink down") : LogSink {
    override fun emit(entry: LogEntry) = throw IllegalStateException(message)

    override fun flush() = throw IllegalStateException(message)

    override fun close() = throw IllegalStateException(message)
}

val fixedTime: Instant = Instant.parse("2026-01-01T00:00:00Z")

val fixedClock: Clock =
    object : Clock {
        override fun now(): Instant = fixedTime
    }

/**
 * Settings for tests: everything is logged from [level] up, time is fixed, and an error in logging is
 * thrown so a test notices it.
 */
fun testSettings(level: LogLevel = LogLevel.Debug): LogSettings =
    LogSettings.safe().copy(level = level, clock = fixedClock, errors = ErrorHandler.Throw)

fun fields(vararg pairs: Pair<String, Any?>): List<Pair<String, Any?>> = pairs.asList()

/**
 * A logger for a test, made the way users make one: from a [Logs]. It follows that factory's settings.
 */
fun testLogger(settings: LogSettings, sink: LogSink, name: String = "L"): Logger = Logs(settings, sink).logger(name)
