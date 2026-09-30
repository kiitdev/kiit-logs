# Changelog

All notable changes to kiit-logs are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/), versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Extracted from the Kiit monorepo (`kiit.common.log`, commit `c2bf883`) as its own standalone
  Kotlin Multiplatform module for JVM, Android and iOS. Package `kiit.logs`, artifact `dev.kiit:kiit-logs:0.7.0`.
- Structured logging as the default style: `info("place", "order_id" to id)`. Entries carry `action`, `origin`,
  `scope`, redacted `fields` and `trace`. Lazy variants only build their fields when the level is enabled.
- `LogSettings` and `LogSettings.safe()`: level, levels by logger name, stack trace mode, redaction, filter,
  origin, scope, error policy, clock and trace line cap.
- `Redaction` (`KeyMatch`, `RedactAction`) and the `Redactor` interface to replace it.
- `StackTraces` (`Off`, `Summary` with the cause chain, `Full` with a line cap).
- `ErrorPolicy` (`Propagate`, `Handle`) and `LogErrorHandler`. The default prints the first 3 errors and never throws.
- `LogSink` (`emit`, `flush`, `close`, `rawFor`), `ConsoleSink`, `CompositeSink`, `minLevel` and `filtered`.
- `SinkLogFactory` (any sink) and `ConsoleLogFactory`, with caching by name, `setLevel` (global or by name),
  `flush` and `close`.
- `Logger.with(...)` to add fields to every entry, `NoLogger`, `LogLevel.Trace` and `LogLevel.Off`.
- Android writes to logcat with the real priority and tag, and splits long entries (default 4000 characters).

### Changed (from `kiit.common.log`, for anyone moving over)
- Package `kiit.common.log` is now `kiit.logs`, and the artifact is `kiit-logs` (was part of `kiit-common`).
- `LogSupport` is gone. Its methods are on `Logger`, so a class holds a `Logger` and calls it, instead of
  mixing in an interface. A nullable `Logger?` becomes `NoLogger`.
- `Logger` is a final class, `Logger(settings, name, sink)`. A provider no longer extends it. It implements
  `LogSink.emit`, and the level check, filter and redaction are done before `emit`.
- `Logs` is now `LogFactory`, and `LogsDefault` (an object) is `ConsoleLogFactory(settings)` (a class).
  `Provider` is no longer inherited, `provider` is a property of `LogFactory`. `getLogger(Class)` takes a `KClass`,
  with a JVM extension for `Class`.
- The default level is `Error`. The console default was `Debug` and `Logger` was `Warn`.
- `LogEntry.time` is a `kotlinx.datetime.Instant` (was a `ZonedDateTime`). `LogEntry.tag` is removed.
- The logging methods take an action and fields. Printf-style messages (`info("id=%s", id)`), the exception
  first overloads and the key/value list overloads are removed. Free text is `log(level, msg, ex)`.
- Sensitive keys are masked (`password=***`) and configurable. They used to be dropped, from a fixed list.
- Console output is `<time> [name] Level : scope action, msg, k=v`, comma separated. It used to print the logger's
  level, not the entry's, and had literal `+ : +` text. Field keys are printed as written.
- Stack traces are off by default. The exception message is always part of the log line.
- Field key case: `LogUtils.toKey` lowercased keys in the output, so `orderId` printed as `orderid`. It no longer does.

### Removed
- `LogLevel.parse` (it always returned `Debug`, because of a case bug), `LogSupport.trace`, `LogUtils`,
  `@Ignore` on the logging methods, and the dependency on `kiit-common`.

### Fixed
- `LoggerConsole` printed the logger's level instead of the entry's level.
- The key/value overload that took `Pair<String, String>` did not redact sensitive keys.
