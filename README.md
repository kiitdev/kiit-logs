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
4. **Settings.** One `LogSettings` value holds the level, stack trace mode, redaction, origin, scope and clock. Level, stack traces and redaction have no defaults on the constructor, so `LogSettings.safe()` is the way in.
5. **Redaction.** Fields whose key matches a sensitive word get their value replaced with `***`, or are dropped. Keys are compared without case, spaces, `_`, `-` or `.`, so `api_key` and `apiKey` are the same.
6. **Stack traces.** `Off` (the default), `Summary` (type and message, then each cause) or `Full` (cut to 50 lines by default). The exception message is always part of the log line.
7. **Providers.** A logger sends every entry that passes the level check to `emit`. The console logger prints it. A provider extends `Logger` and does something else with it.

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

**Change the level while the app runs**, for diagnostics. It applies to loggers that already exist:

```kotlin
logs.setLevel(LogLevel.Debug)
```

Or change one logger:

```kotlin
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

**Mix it into a class.** Implement `LogSupport` and the level methods are available directly:

```kotlin
class OrderService(override val logger: Logger) : LogSupport {
    fun place(id: String) {
        info("place", "order_id" to id)
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

**Write a provider** by extending `Logger` and implementing `emit`. `entry.text` is a ready to print line, and `entry.fields` are the redacted pairs:

```kotlin
class ListLogger(settings: LogSettings) : Logger(settings, "list") {
    val lines = mutableListOf<String>()

    override fun emit(entry: LogEntry) {
        lines.add(entry.text)
    }
}
```

The level check happens before `emit`, so a provider that wraps another library should leave that library's own level wide open. Otherwise it may drop entries that already passed. `Logger.raw` and `LogFactory.provider` give you the wrapped objects if you need them.

**Test with a fixed time** by replacing `LogSettings.clock` with your own `kotlinx.datetime.Clock`.

## Limits

This is a small logger. Here is what it doesn't do.

1. **Console only.** Android writes to logcat with the real level and tag, which is fine for a real app. The JVM and iOS use `println`, so the console logger is for development and tests there. iOS doesn't use `os_log` yet.
2. **No files, rotation, async or JSON.** That's the provider's job. No provider ships yet.
3. **Best effort redaction.** It matches on the field key. It doesn't look inside values, message text or an object's `toString()`.
4. **Trace ids come from elsewhere.** If you use a tracing agent, it puts the ids in the logging context and a provider such as SLF4J passes them along. kiit-logs doesn't create them or read that context. Use `log.with(...)` to attach an id yourself.
5. **Flat fields.** Values are plain key/value pairs. There's no nesting and no schema for action names.
6. **Levels are fixed.** There are no custom levels.
7. **Levels are per logger.** There's no configuration by package name and no inheritance from a parent logger.
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
