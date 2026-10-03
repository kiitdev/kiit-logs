# Changelog

All notable changes to kiit-logs are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/), versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Extracted from the Kiit monorepo (`kiit.common.log`, commit `c2bf883`) as its own standalone
  Kotlin Multiplatform module for JVM, Android and iOS. Package `kiit.logs`, artifact `dev.kiit:kiit-logs:0.7.0`.
  It depends only on the Kotlin standard library.
- Logging through one input type, `LogData`, an open interface with `prefix`, `msg`, `ex` and `fields`, each with a
  default. `Logger` has one general method, `log(level, data)`, and for each level an eager and a lazy form:
  `info(Action("place_order", "order_id" to id))` and `info { Action(...) }`. The lambda only runs when the level is
  enabled. Entries carry `prefix`, `source`, redacted `fields` and `trace`.
- The built-in `LogData` types in `kiit.logs.data`: `Action` (something about to be done, `ACTION: place_order`),
  `Event` (something that has happened, `EVENT: order_placed`) and `Text` (free form). `Action` and `Event` take a name,
  fields, and an optional `msg` and `ex`. An exception's message goes after the text on the same line:
  `payment failed: card declined`.
- `Prefix(label, value)` and `Source(origin, scope = "")` in `kiit.logs`. `Source.text` is `origin:scope`, the same form
  as a service id in kiit-service-id.
- `LogSettings` and `LogSettings.safe()`: level, levels by logger name, stack trace mode, policies, source,
  error handler, clock and trace line cap. `LogSettings.safe()` uses the source `app:` when it is given none.
- Policies in `kiit.logs.policies`: `Policy` (an entry in, the entry, a changed copy or null out), `RedactPolicy`
  (`KeyMatch`, `RedactAction`) and `FilterPolicy`. `LogSettings.policies` runs them in order.
- `StackTraces` (`Off`, `Summary` with the cause chain, `Full` with a line cap).
- `ErrorHandler` with `ErrorHandler.printing()` as the default (prints the first 3 errors, never throws) and
  `ErrorHandler.Throw` for tests. `ErrorHandler.Stage` says where something failed.
- Sinks in `kiit.logs.sinks`: `LogSink` (`emit`, `flush`, `close`, `rawFor`), `ConsoleSink` and `CompositeSink`.
- `MemorySink` in `kiit.logs.sinks`, for testing an app's logging. It keeps the entries it receives, is safe to use from
  several threads, and has `entries`, `find(value)`, `clear()` and the `flushed` and `closed` call counts.
- `Logs`, the `LogFactory` you use: `Logs.console(settings)` and `Logs(settings, sink)`. It creates loggers with
  `logger(name)` and `logger(cls)`, caches them by name and scope, and has `setLevel` (global or by name), `flush` and `close`.
  `Logs.console()` uses `LogSettings.safe()`. A logger without a name is named `root` (`LogFactory.DEFAULT_NAME`).
- `logger(name, scope)` and `logger(cls, scope)` give a logger its own scope, in place of the scope of the settings.
  The origin stays, the same name with another scope is another logger, and levels are still set by name.
- `setLevel(name, null)` removes the level of a name, atomically. The name then follows the longest remaining prefix,
  or the global level. `setLevel(cls, level)` sets the level for a class, named the way `logger(cls)` names it.
  `setLevel(name, level)` now takes a `LogLevel?`, so a `LogFactory` implementation that overrides it must change its
  parameter to `LogLevel?`.
- `Logger.with(...)` to add fields to every entry, `NoLogger`, `LogLevel.Verbose` (the finest level, with `verbose(...)` methods) and `LogLevel.Off`.
- Android writes to logcat with the real priority and tag, and splits long entries (default 4000 characters).
- macOS targets (`macosArm64`, `macosX64`). iOS and macOS share the console writer in `appleMain`.
- `LogEntry.thread`: the name of the thread that created the entry. The thread name on the JVM and Android, the
  NSThread name on Apple, and `"main"` for the Apple main thread when it has no name. It is not part of `text`.
- A Kotlin sample app in `samples/sample-kotlin`, run with `./gradlew :samples:sample-kotlin:run`.

### Changed (from `kiit.common.log`, for anyone moving over)
- Package `kiit.common.log` is now `kiit.logs`, and the artifact is `kiit-logs` (was part of `kiit-common`).
- `LogSupport` is gone. Its methods are on `Logger`, so a class holds a `Logger` and calls it, instead of
  mixing in an interface. A nullable `Logger?` becomes `NoLogger`.
- `Logger` is a final class with no public constructor. A `Logs` creates it, so every logger follows the settings
  of the `Logs` that made it. A provider no longer extends `Logger`. It implements `LogSink.emit`, and the level
  check and the policies are done before `emit`.
- `Logs` (the old interface) is now `LogFactory`, and `LogsDefault` (an object) is `Logs.console(settings)`, with
  `Logs` as the class that implements `LogFactory`. `Provider` is no longer inherited, the wrapped library's root object is a property
  of `LogFactory`. `getLogger(Class)` is `logger(KClass)`, and there is no overload that takes a `Class`.
- The level changes at runtime through the factory, `logs.setLevel(...)`, and the change is atomic. A logger's
  `settings` can be read, not replaced.
- The default level is `Error`. The console default was `Debug` and `Logger` was `Warn`.
- `LogEntry.time` is a `kotlin.time.Instant` and `LogSettings.clock` is a `kotlin.time.Clock` (the time was a
  `ZonedDateTime`). `LogEntry.tag` is removed.
- The logging methods take a `LogData`. Printf-style messages (`info("id=%s", id)`), the exception
  first overloads and the key/value list overloads are removed. Free text is `Text(msg, ex)`.
- `LogEntry.action` is `LogEntry.prefix`, a `Prefix(label, value)`, e.g. `Prefix("ACTION", "place_order")`. `LogEntry.origin`
  and `LogEntry.scope` are `LogEntry.source`, and `LogSettings.origin` and `LogSettings.scope` are `LogSettings.source`.
  `LogSettings.safe(origin, scope)` is `LogSettings.safe(source)`.
- `LogFactory.logger(name)` and `logger(cls)` take an optional `scope`, so a `LogFactory` implementation must add the
  parameter.
- `MemorySink.find(action)` is `find(value)`, and it matches the value of the prefix under any label.
- Sensitive keys are masked (`password=***`) and configurable. They used to be dropped, from a fixed list. The
  default matching is `Contains`, which hides more rather than less, for example `token_count`.
- Console output is `<time> [origin:scope] Level : prefix, k=v, logger=name`, comma separated, e.g.
  `ACTION: place_order, order_id=abc, logger=OrderService`. The `msg` of an action or event prints as `msg="..."` after
  the prefix, and the message of a `Text` prints as it is. The old output printed the logger's level, not the entry's,
  and had literal `+ : +` text. Field keys are printed as written.
- Stack traces are off by default. The exception message is always part of the log line.
- Field key case: `LogUtils.toKey` lowercased keys in the output, so `orderId` printed as `orderid`. It no longer does.

- Apple console output uses `NSLog` instead of `println`. `NSLog` adds its own time and process prefix, so the line
  is `[origin:scope] Level : text` with no timestamp of ours. Android writes `[origin:scope] text` to logcat, with the
  logger name as the tag.
- `LogEntry` has a new last constructor parameter, `thread`. Code that builds entries by name is not affected. Code
  that destructures one with `componentN` or calls the constructor positionally past `trace` needs a look.

### Removed
- The per-level overloads with fields, an exception or a lambda of fields, `logAction`, and the `log(level, msg, ex)`
  and `log(level, ex) { text }` forms. `Logger` no longer needs the `TooManyFunctions` suppression.
- `LogLevel.parse` (it always returned `Debug`, because of a case bug), `LogSupport.trace`, `LogUtils`,
  `@Ignore` on the logging methods, and the dependency on `kiit-common`.

### Fixed
- `LoggerConsole` printed the logger's level instead of the entry's level.
- The key/value overload that took `Pair<String, String>` did not redact sensitive keys.
