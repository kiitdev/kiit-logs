<div align="center">

# kiit-logs

**Structured logging for Kotlin Multiplatform apps. Small, mobile first, with safe defaults.**

[![Build](https://img.shields.io/github/actions/workflow/status/kiitdev/kiit-logs/ci.yml?branch=main)](https://github.com/kiitdev/kiit-logs/actions/workflows/ci.yml)
[![License](https://img.shields.io/github/license/kiitdev/kiit-logs)](./LICENSE)
[![Kotlin](https://img.shields.io/badge/kotlin-multiplatform-purple.svg)](https://kotlinlang.org)

Part of [Kiit](https://www.kiit.dev)

</div>

## Table of Contents

- [Why](#why)
- [Start](#start)
- [Concepts](#concepts)
- [Usage](#usage)
- [Limits](#limits)
- [Requirements](#requirements)
- [License](#license)

## Why

A log line is usually a sentence with values glued into it. Searching for it later means guessing the wording. kiit-logs logs the action and its inputs instead: a name for what was attempted, then the key/value pairs that matter.

```kotlin
log.info("place", "order_id" to "abc", "total" to 42)
```

```
2026-09-30T05:13:42.874636Z [OrderService] Info : place, order_id=abc, total=42
```

It's a small API for apps that run on Android, iOS and the JVM, from one codebase. It doesn't try to be a full logging framework. There's no file output, rotation or JSON here. The console logger is meant for development, tests and small apps, and on Android it writes to logcat. A production server should use a provider such as SLF4J and Logback behind the same calls, and that wrapper isn't built yet.

The defaults lean safe. Only errors are logged, stack traces are off, and keys like `password` and `email` are masked before an entry exists. Those are guardrails, not guarantees, and [Limits](#limits) says where they stop.

```
log.info(...)  ->  level check  ->  redact fields  ->  LogEntry  ->  emit  ->  console or provider
```

## Start

kiit-logs hasn't been published to Maven Central yet. For now, publish it to your local Maven repository and use it from there:

```bash
./gradlew :kiit-logs:publishToMavenLocal
```

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("dev.kiit:kiit-logs:0.7.0")
}
```

Create a factory once for the app, then ask it for loggers by name:

```kotlin
import kiit.logs.*

val logs = ConsoleLogFactory(
    LogSettings.safe(origin = "shop.example.com").copy(level = LogLevel.Info)
)
val log = logs.getLogger("OrderService")

log.info("place", "order_id" to "abc", "total" to 42)
```

`LogSettings.safe()` starts from the safe defaults, and `copy` changes one thing. The level is `Error` by default, so the `.copy(level = LogLevel.Info)` above is what lets `info` calls print. Without it, this example prints nothing.

## Concepts

1. **Action and fields.** The first argument of `debug`, `info`, `warn`, `error` and `fatal` is the action, what was attempted. The rest are key/value pairs. This is the style to reach for first.
2. **Origin and scope.** `origin` says who owns the system, set once for the app. `scope` says where inside it, like `accounts.signup`. They mean the same thing as in kiit-codes and kiit-service-id.
3. **Levels.** `Trace`, `Debug`, `Info`, `Warn`, `Error`, `Fatal`, and `Off`. Set the level to `Off` and nothing is logged. `Trace` is the finest level and has no shortcut method, use `logAction(LogLevel.Trace, ...)`.
4. **Settings.** One `LogSettings` value holds the level, stack trace mode, redaction, origin, scope, error policy and clock. Level, stack traces and redaction have no defaults on the constructor, so `LogSettings.safe()` is the way in.
5. **Redaction.** By default, fields whose key matches a sensitive word get their value replaced with `***`, or are dropped. Keys are compared without case, spaces, `_`, `-` or `.`, so `api_key` and `apiKey` are the same. To use your own rule instead, pass a `Redactor`.
6. **Stack traces.** `Off` (the default), `Summary` (type and message, then each cause) or `Full` (cut to 50 lines by default). The exception message is always part of the log line.
7. **Sinks.** A logger sends every entry that passes the level check and the filter to a `LogSink`. `ConsoleSink` prints it. A provider implements `LogSink` and does something else with it, so it never has to redo the level check, the filter or redaction.

## Usage

**Fields are masked by their key**, before the entry is created:

```kotlin
val signup = ConsoleLogFactory(
    LogSettings.safe(origin = "shop.example.com", scope = "accounts.signup").copy(level = LogLevel.Info)
).getLogger("Signup")

signup.info("signup", "email" to "a@b.com", "plan" to "pro")
```

```
2026-09-30T05:13:42.884636Z [Signup] Info : accounts.signup signup, email=***, plan=pro
```

**Pass an exception**, and choose how much of it prints:

```kotlin
val settings = LogSettings.safe().copy(level = LogLevel.Info, stackTraces = StackTraces.Summary)
val payments = ConsoleLogFactory(settings).getLogger("Payments")

payments.error("charge", IllegalStateException("card declined"), "order_id" to "abc")
```

```
2026-09-30T05:13:42.884782Z [Payments] Error : charge, card declined, order_id=abc
IllegalStateException: card declined
```

**Build expensive fields lazily.** The lambda only runs if the level is enabled:

```kotlin
log.debug("place") { listOf("total" to expensiveTotal()) }
```

**Set levels by logger name.** A name covers the names under it, and the longest match wins:

```kotlin
val settings = LogSettings.safe().copy(
    levels = mapOf("com.shop.orders" to LogLevel.Debug, "com.shop.orders.audit" to LogLevel.Warn),
)
// com.shop.orders.checkout -> Debug, com.shop.orders.audit.x -> Warn, com.shop.ordersX -> Error
```

**Drop entries with a filter.** Return `false` to drop one before it's emitted. It sees fields added by `with` too:

```kotlin
val settings = LogSettings.safe().copy(filter = { entry -> entry.action != "noisy" })
```

**Change the level while the app runs**, for diagnostics. It applies to loggers that already exist:

```kotlin
logs.setLevel(LogLevel.Debug)
```

Or one name and the names under it, or one logger:

```kotlin
logs.setLevel("com.shop.orders", LogLevel.Debug)
log.settings = log.settings.copy(level = LogLevel.Debug)
```

**Add fields to every entry of a logger**, such as an id for one request:

```kotlin
val requestLog = log.with("trace_id" to traceId)

requestLog.info("place", "order_id" to id)   // fields: trace_id, order_id
```

The new logger shares the settings of the one it came from, so a level change applies to it too. The fields are redacted like any others.

**Log free text** when there's no action to name:

```kotlin
log.log(LogLevel.Warn, "payment slow")
```

**Hold a logger in a class**, and call it like any other object:

```kotlin
class OrderService(private val log: Logger) {
    fun place(id: String) {
        log.info("place", "order_id" to id)
    }
}
```

**Turn logging off** by passing `NoLogger`:

```kotlin
OrderService(NoLogger).place("xyz")   // prints nothing
```

**Choose your own sensitive keys**, and drop them instead of masking:

```kotlin
val redaction = Redaction(
    keys = Redaction.defaults + "ssn",
    match = KeyMatch.Contains,
    action = RedactAction.Drop,
)
val settings = LogSettings.safe().copy(redaction = redaction)
```

**Replace redaction entirely** with a `Redactor`, for example to look at values:

```kotlin
val redactor = Redactor { fields ->
    fields.map { (key, value) -> if (value is String && "@" in value) key to "<email>" else key to value }
}
val settings = LogSettings.safe().copy(redaction = redactor)
```

**Write a provider** by implementing `LogSink`. `entry.text` is a ready to print line, `entry.fields` are the redacted pairs, and `entry.trace` is the exception rendered by the stack trace setting:

```kotlin
class ListSink : LogSink {
    val lines = mutableListOf<String>()

    override fun emit(entry: LogEntry) {
        lines.add(entry.text)
    }
}

val log = Logger(LogSettings.safe().copy(level = LogLevel.Info), "list", ListSink())
```

A sink can also override `flush()` and `close()` if it buffers or holds resources. Both do nothing by default. Call `flush()` when the app goes to the background and `close()` once at shutdown:

```kotlin
log.flush()      // this logger's sink
logs.flush()     // the factory's sink
logs.close()     // flush and release it, all loggers of the factory share it
```

`ConsoleSink` has nothing to flush or close, because it doesn't buffer.

The level check happens before `emit`, so a sink that wraps another library should leave that library's own level wide open. Otherwise it may drop entries that already passed. `Logger.raw` and `LogFactory.provider` give you the wrapped objects if you need them.

**Use your sink through a factory**, to keep caching by name and `setLevel`:

```kotlin
val logs = SinkLogFactory(LogSettings.safe(origin = "shop.example.com"), ListSink())
val log = logs.getLogger("OrderService")
```

**Send to more than one place.** `CompositeSink` gives each entry to every sink. A sink that throws doesn't stop the others:

```kotlin
val sink = CompositeSink(ConsoleSink(), crashReporter.minLevel(LogLevel.Error))
val logs = SinkLogFactory(LogSettings.safe().copy(level = LogLevel.Debug), sink)
```

`minLevel` and `filtered { }` narrow one sink. The logger's own level still decides what is logged at all, so here the console gets everything from `Debug` up and the crash reporter only gets errors.

**Decide what happens when logging fails.** A sink can be down, and a filter, a redactor or a lazy message can have a bug. `LogSettings.errors` says what to do about it:

```kotlin
val quiet = LogSettings.safe()                                            // Swallow, the default
val strict = LogSettings.safe().copy(errors = ErrorPolicy.Propagate)      // throw to the caller
val report = LogSettings.safe().copy(
    errors = ErrorPolicy.Handle { stage, error, entry ->
        System.err.println("logging failed at $stage: ${error.message}")
    },
)
```

`Swallow` means logging never throws into your code, which is the safe choice on a phone. `Propagate` is for tests and development. A handler is told the stage (`Build`, `Filter`, `Sink` or `Lifecycle`), the error, and the entry if it was built. It runs on the calling thread, so keep it quick, and if it throws that is ignored. With a `CompositeSink`, the other sinks still run, and then the first error goes to the policy with the rest attached to it.

**Test with a fixed time** by replacing `LogSettings.clock` with your own `kotlinx.datetime.Clock`.

## Limits

This is a small logger. Here is what it doesn't do.

1. **Console only.** Android writes to logcat with the real level and tag, which is fine for a real app. The JVM and iOS use `println`, so the console logger is for development and tests there. iOS doesn't use `os_log` yet.
2. **No files, rotation, async or JSON.** That's the provider's job. No provider ships yet.
3. **Best effort redaction.** The default matches on the field key. It doesn't look inside values, message text or an object's `toString()`. A custom `Redactor` can look at values, but not the message text.
4. **Trace ids come from elsewhere.** If you use a tracing agent, it puts the ids in the logging context and a provider such as SLF4J passes them along. kiit-logs doesn't create them or read that context. Use `log.with(...)` to attach an id yourself.
5. **Flat fields.** Values are plain key/value pairs. There's no nesting and no schema for action names.
6. **Levels are fixed.** There are no custom levels.
7. **Names are strings.** A level applies to a name and the names under it, but there are no logger objects with parents.
8. **Not 1.0.** The API is still moving.

## Requirements

- Kotlin Multiplatform
- JVM, Android, iOS (arm64, simulator arm64, x64)
- Depends on `kotlinx-datetime` (transitively available to consumers via `api`)

## License

[Apache License 2.0](./LICENSE)

---

<div align="center">

**kiit-logs** is one module of [Kiit](https://www.kiit.dev), a lightweight, modular
Kotlin toolkit for building server applications, APIs, CLIs, and jobs.

**Adopt one module at a time.**

</div>
