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
import kiit.logs.policies.ErrorHandler.Stage
import kiit.logs.sinks.LogSink
import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KClass

/**
 * Creates loggers that all send to one [LogSink], and caches them by name so getLogger returns the
 * same logger for the same name. Use it to plug in your own sink and keep [setLevel]:
 *
 *     val logFactory = DefaultLogFactory(LogSettings.safe(), MySink())
 *
 * @param provider what [LogFactory.provider] returns, the sink by default
 */
@OptIn(ExperimentalAtomicApi::class)
open class DefaultLogFactory(
    settings: LogSettings,
    private val sink: LogSink,
    override val provider: Any = sink
) : LogFactory {
    @Volatile
    final override var settings: LogSettings = settings
        private set

    // Copy-on-write, so reads and lookups need no lock
    private val loggers = AtomicReference<Map<String, Logger>>(emptyMap())

    override fun getLogger(cls: KClass<*>): Logger {
        val key = cls.qualifiedName ?: cls.simpleName ?: "console"
        return cached(key) { Logger(settings, key, sink) }
    }

    override fun getLogger(name: String?): Logger {
        val key = name ?: "console"
        return cached(key) { Logger(settings, key, sink) }
    }

    override fun setLevel(level: LogLevel) {
        settings = settings.copy(level = level)
        loggers.load().values.forEach { it.settings = it.settings.copy(level = level) }
    }

    override fun setLevel(name: String, level: LogLevel) {
        val levels = settings.levels + (name to level)
        settings = settings.copy(levels = levels)
        loggers.load().values.forEach { it.settings = it.settings.copy(levels = levels) }
    }

    override fun flush() {
        ErrorGuard.guard(settings.errors, Stage.Lifecycle, null) { sink.flush() }
    }

    override fun close() {
        ErrorGuard.guard(settings.errors, Stage.Lifecycle, null) { sink.close() }
    }

    private fun cached(key: String, create: () -> Logger): Logger {
        while (true) {
            val current = loggers.load()
            current[key]?.let { return it }
            val created = create()
            if (loggers.compareAndSet(current, current + (key to created))) {
                // setLevel may have run after this logger read the settings but before it was stored
                val latest = settings
                if (created.settings.level != latest.level || created.settings.levels != latest.levels) {
                    created.settings = created.settings.copy(level = latest.level, levels = latest.levels)
                }
                return created
            }
        }
    }
}
