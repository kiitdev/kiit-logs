# Changelog

All notable changes to kiit-logs are documented here. Format follows
[Keep a Changelog](https://keepachangelog.com/), versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

### Added
- Extracted from the Kiit monorepo (`kiit.common.log`, commit `c2bf883`) as its own standalone
  Kotlin Multiplatform module for JVM, Android and iOS. Package `kiit.logs`, artifact `dev.kiit:kiit-logs:0.7.0`.
  It depends only on the Kotlin standard library.
- Structured logging as the default style: `info("place", "order_id" to id)`. Entries carry `action`, `origin`,
  `scope`, redacted `fields` and `trace`. Lazy variants only build their fields when the level is enabled.
- Free text with `log(level, msg, ex)` and a lazy `log(level, ex) { text }`. An exception's message goes after the
  text on the same line: `payment failed: card declined`.
- `LogSettings` and `LogSettings.safe()`: level, levels by logger name, stack trace mode, policies, origin, scope,
  error handler, clock and trace line cap.
- Policies in `kiit.logs.policies`: `Policy` (an entry in, the entry, a changed copy or null out), `RedactPolicy`
  (`KeyMatch`, `RedactAction`) and `FilterPolicy`. `LogSettings.policies` runs them in order.
- `StackTraces` (`Off`, `Summary` with the cause chain, `Full` with a line cap).
- `ErrorHandler` with `ErrorHandler.printing()` as the default (prints the first 3 errors, never throws) and
  `ErrorHandler.Throw` for tests. `ErrorHandler.Stage` says where something failed.
- Sinks in `kiit.logs.sinks`: `LogSink` (`emit`, `flush`, `close`, `rawFor`), `ConsoleSink` and `CompositeSink`.
- `MemorySink` in `kiit.logs.sinks`, for testing an app's logging. It keeps the entries it receives, is safe to use from
  several threads, and has `entries`, `find(action)`, `clear()` and the `flushed` and `closed` call counts.
- `Logs`, the `LogFactory` you use: `Logs.console(settings)` and `Logs(settings, sink)`. It creates loggers with
  `logger(name)` and `logger(cls)`, caches them by name, and has `setLevel` (global or by name), `flush` and `close`.
  `Logs.console()` uses `LogSettings.safe()`. A logger without a name is named `root` (`LogFactory.DEFAULT_NAME`).
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
- The logging methods take an action and fields. Printf-style messages (`info("id=%s", id)`), the exception
  first overloads and the key/value list overloads are removed. Free text is `log(level, msg, ex)`.
- Sensitive keys are masked (`password=***`) and configurable. They used to be dropped, from a fixed list. The
  default matching is `Contains`, which hides more rather than less, for example `token_count`.
- Console output is `<time> [name] Level : scope action, msg, k=v`, comma separated. It used to print the logger's
  level, not the entry's, and had literal `+ : +` text. Field keys are printed as written.
- Stack traces are off by default. The exception message is always part of the log line.
- Field key case: `LogUtils.toKey` lowercased keys in the output, so `orderId` printed as `orderid`. It no longer does.

- Apple console output uses `NSLog` instead of `println`. `NSLog` adds its own time and process prefix, so the line
  is `[name] Level : text` with no timestamp of ours.
- `LogEntry` has a new last constructor parameter, `thread`. Code that builds entries by name is not affected. Code
  that destructures one with `componentN` or calls the constructor positionally past `trace` needs a look.

### Removed
- `LogLevel.parse` (it always returned `Debug`, because of a case bug), `LogSupport.trace`, `LogUtils`,
  `@Ignore` on the logging methods, and the dependency on `kiit-common`.

### Fixed
- `LoggerConsole` printed the logger's level instead of the entry's level.
- The key/value overload that took `Pair<String, String>` did not redact sensitive keys.
