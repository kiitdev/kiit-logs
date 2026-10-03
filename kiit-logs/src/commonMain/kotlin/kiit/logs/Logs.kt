/**
 *  <kiit_header>
 * url: www.kiit.dev
 * git: www.github.com/slatekit/kiit
 * org: www.codehelix.co
 * author: Kishore Reddy
 * copyright: 2016 CodeHelix Solutions Inc.
 * license: refer to website and/or github
 *
 *
 *  </kiit_header>
 */

package kiit.logs

import kiit.logs.internal.ErrorGuard
import kiit.logs.internal.SettingsRef
import kiit.logs.policies.ErrorHandler.Stage
import kiit.logs.sinks.ConsoleSink
import kiit.logs.sinks.LogSink
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KClass

/**
 * The [LogFactory] you use: creates loggers that all send to one [LogSink], and caches them by name and scope so
 * logger(name) returns the same logger for the same name. Settings are always passed in, there is no global state.
 *
 *     val logs = Logs.console(LogSettings.safe(Source("shop.example.com")))   // print to the console
 *     val logs = Logs(LogSettings.safe(), MySink())                            // or use your own sink
 *     val log = logs.logger(OrderService::class)
 *     val pay = logs.logger(PaymentService::class, scope = "orders.payment")   // entries say orders.payment
 *     logs.setLevel(LogLevel.Debug)                                            // changes every logger, at runtime
 *
 * A provider that wraps another library, e.g. Logback, supplies a [LogSink] and the wrapped library's root
 * object as [raw], it doesn't extend this class.
 *
 * @param settings given to every logger this creates, use [LogSettings.safe] for safe defaults
 * @param sink where every logger sends its entries
 * @param raw what [LogFactory.raw] returns, the sink by default
 */
@OptIn(ExperimentalAtomicApi::class)
class Logs(
    settings: LogSettings,
    private val sink: LogSink,
    override val raw: Any = sink
) : LogFactory {
    private val current = SettingsRef(settings)

    override val settings: LogSettings get() = current.get()

    // Copy-on-write, so reads and lookups need no lock
    private val loggers = AtomicReference<Map<LoggerKey, Logger>>(emptyMap())

    override fun logger(cls: KClass<*>, scope: String?): Logger = cached(LoggerKey(nameOf(cls), scope))

    override fun logger(name: String?, scope: String?): Logger = cached(LoggerKey(name ?: LogFactory.DEFAULT_NAME, scope))

    // The loggers read the settings held here, so changing them is one atomic step for all of them
    override fun setLevel(level: LogLevel) {
        current.update { it.copy(level = level) }
    }

    override fun setLevel(name: String, level: LogLevel?) {
        current.update { settings ->
            val levels =
                if (level == null) {
                    // a copy of the map without this name
                    settings.levels - name
                } else {
                    // a copy with this name added, or replaced
                    settings.levels + (name to level)
                }
            settings.copy(levels = levels)
        }
    }

    override fun flush() {
        ErrorGuard.guard(settings.errors, Stage.Lifecycle, null) { sink.flush() }
    }

    override fun close() {
        ErrorGuard.guard(settings.errors, Stage.Lifecycle, null) { sink.close() }
    }

    // A logger is for a name and a scope, so the same name with another scope is another logger
    private data class LoggerKey(val name: String, val scope: String?)

    private fun cached(key: LoggerKey): Logger {
        while (true) {
            val known = loggers.load()
            known[key]?.let { return it }
            val created = Logger(current, key.name, sink, key.scope)
            if (loggers.compareAndSet(known, known + (key to created))) return created
        }
    }

    companion object {
        /**
         * Loggers that print to the console. [LogFactory.raw] is the [ConsoleSink].
         * @param maxLength see [ConsoleSink]
         */
        fun console(settings: LogSettings = LogSettings.safe(), maxLength: Int = ConsoleSink.DEFAULT_MAX_LENGTH): Logs =
            Logs(settings, ConsoleSink(maxLength))
    }
}
