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

It's a small API for apps that run on Android, iOS and the JVM, from one codebase, with no dependencies beyond the Kotlin standard library. It doesn't try to be a full logging framework. There's no file output, rotation or JSON here. The console logger is meant for development, tests and small apps, and on Android it writes to logcat. A production server should use a provider such as SLF4J and Logback behind the same calls, and that wrapper isn't built yet.

The defaults lean safe. Only errors are logged, stack traces are off, and keys like `password` and `email` are masked before an entry reaches a sink. When the masking has to guess, it hides more, not less. Those are guardrails, not guarantees, and [Limits](#limits) says where they stop.

```
log.info(...)  ->  level check  ->  LogEntry  ->  policies (redact, filter)  ->  LogSink  ->  console or provider
```

## Start

kiit-logs hasn't been published to Maven Central yet. For now, publish it to your local Maven repository and use it from there:

```bash
./gradlew :kiit-logs:publishToMavenLocal
```

This doesn't sign the artifacts, so it works without a gpg key.

```kotlin
repositories {
    mavenLocal()
}

dependencies {
    implementation("dev.kiit:kiit-logs:0.7.0")
}
```

Create a `Logs` once for the app, then ask it for loggers by name:

```kotlin
import kiit.logs.LogLevel
import kiit.logs.LogSettings
import kiit.logs.Logs

val logs = Logs.console(LogSettings.safe(origin = "shop.example.com").copy(level = LogLevel.Info))
val log = logs.logger("OrderService")

log.info("place", "order_id" to "abc", "total" to 42)
```

`LogSettings.safe()` starts from the safe defaults, and `copy` changes one thing. The level is `Error` by default, so the `.copy(level = LogLevel.Info)` above is what lets `info` calls print. Without it, this example prints nothing. `Logs.console()` with no arguments uses `LogSettings.safe()`, which is enough for `Logs.console().logger("app")` when errors are all you need.

There's a runnable example in [`samples/sample-kotlin`](./samples/sample-kotlin). Run it with `./gradlew :samples:sample-kotlin:run`.

## Concepts

1. **Action and fields.** The first argument of `debug`, `info`, `warn`, `error` and `fatal` is the action, what was attempted. The rest are key/value pairs. This is the style to reach for first.
2. **Origin and scope.** `origin` says who owns the system, set once for the app. `scope` says where inside it, like `accounts.signup`. They mean the same thing as in kiit-codes and kiit-service-id.
3. **Levels.** `Verbose`, `Debug`, `Info`, `Warn`, `Error`, `Fatal`, and `Off`. Set the level to `Off` and nothing is logged. `Verbose` is the finest level, and it has its own `verbose(...)` method like the others. It has the same name as the lowest level on Android, and it isn't called trace, since trace already means a stack trace here and a request trace in monitoring tools.
4. **Settings.** One `LogSettings` value holds the level, levels by name, stack trace mode, policies, origin, scope, error handler and clock. Level, stack traces and policies have no defaults on the constructor, so `LogSettings.safe()` is the way in.
5. **Policies.** A `Policy` gets each entry before a sink sees it. It returns the entry, a changed copy, or null to drop it. `LogSettings.policies` is a list, and the policies run in order. The default list is one `RedactPolicy`, and `FilterPolicy` drops entries you don't want.
6. **Redaction.** By default, fields whose key contains a sensitive word get their value replaced with `***`, or are dropped. Keys are compared without case, spaces, `_`, `-` or `.`, so `api_key` and `apiKey` are the same. The default matching is `Contains` on purpose. It hides `user_password` and `password_confirm`, and it also hides harmless keys such as `token_count`. Hiding too much is the safer mistake. Use `KeyMatch.Suffix` or `KeyMatch.Exact` for a precise list.
7. **Stack traces.** `Off` (the default), `Summary` (type and message, then each cause) or `Full` (cut to 50 lines by default). The exception message is always part of the log line.
8. **Logs and sinks.** `Logs` creates the loggers and keeps one logger per name. `Logs.console(settings)` prints, and `Logs(settings, sink)` sends everything to your own `LogSink`. A provider implements `LogSink` and does something else with an entry, so it never has to redo the level check or the policies.
9. **Packages.** The main types are in `kiit.logs`: `Logger`, `Logs`, `LogFactory`, `LogSettings`, `LogEntry`, `LogLevel`, `StackTraces` and `NoLogger`. Sinks are in `kiit.logs.sinks` (`LogSink`, `ConsoleSink`, `CompositeSink`), and policies are in `kiit.logs.policies` (`Policy`, `FilterPolicy`, `RedactPolicy`, `ErrorHandler`).

## Usage

**Fields are masked by their key**, before an entry reaches a sink:

```kotlin
val signup = Logs.console(
    LogSettings.safe(origin = "shop.example.com", scope = "accounts.signup").copy(level = LogLevel.Info)
).logger("Signup")

signup.info("signup", "email" to "a@b.com", "plan" to "pro")
```

```
2026-09-30T05:13:42.884636Z [Signup] Info : accounts.signup signup, email=***, plan=pro
```

**Pass an exception**, and choose how much of it prints:

```kotlin
val settings = LogSettings.safe().copy(level = LogLevel.Info, stackTraces = StackTraces.Summary)
val payments = Logs.console(settings).logger("Payments")

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

**Drop entries with a filter.** Add a `FilterPolicy` to the list, after the redaction. It returns `false` to drop an entry, and it sees fields added by `with` too:

```kotlin
val settings = LogSettings.safe().let {
    it.copy(policies = it.policies + FilterPolicy { entry -> entry.action != "noisy" })
}
```

**Change the level while the app runs**, for diagnostics. It applies to every logger the `Logs` made, including the ones that exist already:

```kotlin
logs.setLevel(LogLevel.Debug)
```

Or one name and the names under it:

```kotlin
logs.setLevel("com.shop.orders", LogLevel.Debug)
```

The level is the only setting that changes at runtime, and it changes through the `Logs`. A level set for a name stays until you set it again. A logger's settings can be read (`log.settings`, `log.level`) but not replaced.

**Add fields to every entry of a logger**, such as an id for one request:

```kotlin
val requestLog = log.with("trace_id" to traceId)

requestLog.info("place", "order_id" to id)   // fields: trace_id, order_id
```

The new logger shares the settings of the one it came from, so a level change applies to it too. The fields are redacted like any others.

**Log free text** when there's no action to name. An exception's message goes after the text on the same line:

```kotlin
log.log(LogLevel.Warn, "payment slow")
log.log(LogLevel.Error, "payment failed", IllegalStateException("card declined"))
log.log(LogLevel.Debug) { "cache hits=${hits()}" }     // built only if Debug is enabled
```

```
2026-09-30T05:13:42.855873Z [Free] Warn : payment slow
2026-09-30T05:13:42.856070Z [Free] Error : payment failed: card declined
2026-09-30T05:13:42.856278Z [Free] Debug : cache hits=42
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
val redaction = RedactPolicy(
    keys = RedactPolicy.defaults + "ssn",
    match = KeyMatch.Contains,
    action = RedactAction.Drop,
)
val settings = LogSettings.safe().copy(policies = listOf(redaction))
```

**Add your own rule** with a `Policy`, for example to look at values. Put it after `RedactPolicy` in the list. If you replace the list, the default redaction goes with it, so include `RedactPolicy()` unless you mean to drop it:

```kotlin
val noEmails = Policy { entry ->
    entry.copy(fields = entry.fields.map { (key, value) ->
        if (value is String && "@" in value) key to "<email>" else key to value
    })
}
val settings = LogSettings.safe().copy(policies = listOf(RedactPolicy(), noEmails))
```

**Write a provider** by implementing `LogSink`. `entry.text` is a ready to print line, `entry.fields` are the redacted pairs, and `entry.trace` is the exception rendered by the stack trace setting:

```kotlin
class ListSink : LogSink {
    val lines = mutableListOf<String>()

    override fun emit(entry: LogEntry) {
        lines.add(entry.text)
    }
}

val logs = Logs(LogSettings.safe(origin = "shop.example.com"), ListSink())
val log = logs.logger("OrderService")
```

Going through `Logs` keeps the logger cache by name and `setLevel`. A sink can also override `flush()` and `close()` if it buffers or holds resources. Both do nothing by default. Call `flush()` when the app goes to the background and `close()` once at shutdown:

```kotlin
log.flush()      // this logger's sink
logs.flush()     // the same sink, from the Logs
logs.close()     // flush and release it, all loggers of the Logs share it
```

`ConsoleSink` has nothing to flush or close, because it doesn't buffer.

The level check and the policies happen before `emit`, so a sink that wraps another library should leave that library's own level wide open. Otherwise it may drop entries that already passed. `Logger.raw` and `LogFactory.raw` give you the wrapped objects if you need them, and `rawAs<T>()` casts them for you.

**Send to more than one place.** `CompositeSink` gives each entry to every sink. A sink that throws doesn't stop the others:

```kotlin
val sink = CompositeSink(ConsoleSink(), crashReporterSink)
val logs = Logs(LogSettings.safe().copy(level = LogLevel.Debug), sink)
```

Every sink gets what the logger lets through. To send only some entries to one sink, wrap it in a small sink of your own:

```kotlin
class ErrorsOnly(private val inner: LogSink) : LogSink {
    override fun emit(entry: LogEntry) {
        if (entry.level >= LogLevel.Error) inner.emit(entry)
    }

    override fun flush() = inner.flush()

    override fun close() = inner.close()
}

val sink = CompositeSink(ConsoleSink(), ErrorsOnly(crashReporterSink))
```

The logger's own level still decides what is logged at all, so here the console gets everything from `Debug` up and the crash reporter only gets errors.

**Decide what happens when logging fails.** A sink can be down, and a policy or a lazy message can have a bug. `LogSettings.errors` says what to do about it:

```kotlin
val defaults = LogSettings.safe()                                                 // print the first 3 errors, never throw
val strict = LogSettings.safe().copy(errors = ErrorHandler.Throw)                 // throw to the caller
val report = LogSettings.safe().copy(
    errors = ErrorHandler { stage, error, entry -> crashReporter.record(error) },
)
val silent = LogSettings.safe().copy(errors = ErrorHandler { _, _, _ -> })
```

By default logging never throws into your code, which is the safe choice on a phone. The first 3 errors are printed, with the stage, the logger name and action, and the error, but never the field values. After that it stays quiet, so a broken sink or policy doesn't go unnoticed and doesn't flood the output. `ErrorHandler.Throw` is for tests and development. A handler is told the stage (`Build`, `Policy`, `Sink` or `Lifecycle`), the error, and the entry if there is one. It runs on the calling thread, so keep it quick, and if it throws that is ignored.

What is logged when something fails depends on the stage. A policy that throws, such as a redaction with a bug, drops the entry, so nothing unredacted gets out. In that case the handler gets no entry, because the one in flight may not be redacted yet. A lazy message that throws drops the entry too. With a `CompositeSink`, the other sinks still run, and then the first error goes to the handler with the rest attached to it.

**Test your logging** with `MemorySink`. It keeps the entries it receives and is safe to use from several threads. The policies run before a sink, so the fields it holds are already redacted:

```kotlin
val sink = MemorySink()
val logs = Logs(LogSettings.safe().copy(level = LogLevel.Info), sink)

logs.logger("orders").info("place", "order_id" to "abc")

assertEquals("place", sink.entries.single().action)
assertEquals(listOf("order_id" to "abc"), sink.find("place").single().fields)
```

`entries` is a snapshot in the order logged, `find(action)` returns the entries with that action, and `clear()` empties it between tests. `flushed` and `closed` count the calls to `flush()` and `close()`.

**Test with a fixed time** by replacing `LogSettings.clock` with your own `kotlin.time.Clock`.

## Limits

This is a small logger. Here is what it doesn't do.

1. **Console only.** Android writes to logcat with the real level and tag, which is fine for a real app. Logcat cuts a message at about 4000 bytes, so longer entries are written as several calls, on line ends where possible. The limit is 4000 characters by default, and `Logs.console(settings, maxLength = 3000)` changes it. The JVM and iOS use `println`, so the console logger is for development and tests there. iOS doesn't use `os_log` yet.
2. **No files, rotation, async or JSON.** That's the provider's job. No provider ships yet.
3. **Best effort redaction.** The default matches on the field key. It doesn't look inside values, message text or an object's `toString()`. A custom `Policy` can look at values, but not the message text.
4. **Trace ids come from elsewhere.** If you use a tracing agent, it puts the ids in the logging context and a provider such as SLF4J passes them along. kiit-logs doesn't create them or read that context. Use `log.with(...)` to attach an id yourself.
5. **Flat fields.** Values are plain key/value pairs. There's no nesting and no schema for action names.
6. **Levels are fixed.** There are no custom levels.
7. **Names are strings.** A level applies to a name and the names under it, but there are no logger objects with parents, and a level set for a name can't be removed, only set again.
8. **Not 1.0.** The API is still moving.

## Requirements

- Kotlin Multiplatform
- JVM, Android, iOS (arm64, simulator arm64, x64)
- No dependencies except the Kotlin standard library

## License

[Apache License 2.0](./LICENSE)

---

<div align="center">

**kiit-logs** is one module of [Kiit](https://www.kiit.dev), a lightweight, modular
Kotlin toolkit for building server applications, APIs, CLIs, and jobs.

**Adopt one module at a time.**

</div>
