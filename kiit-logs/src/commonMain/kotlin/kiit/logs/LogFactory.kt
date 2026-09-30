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

import kotlin.concurrent.Volatile
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi
import kotlin.reflect.KClass

/**
 * Creates and caches loggers, all with the same [settings].
 */
interface LogFactory {

    /**
     * Settings given to every logger this creates. Required, use [LogSettings.safe] for safe defaults.
     */
    val settings: LogSettings

    /**
     * Escape hatch to the wrapped library's root object, e.g. Logback's LoggerContext, so it can be
     * reconfigured or shut down. It is Any because the wrapped types are platform specific and
     * can't be named in common code. Use [Logger.raw] for the wrapped logger itself.
     */
    val provider: Any

    fun getLogger(name: String? = ""): Logger
    fun getLogger(cls: KClass<*>): Logger

    /**
     * Changes the level at runtime, e.g. to Debug for diagnostics. Applies to loggers already
     * created and to loggers created later.
     *
     * The level here is the only gate. A provider that wraps another library, e.g. Logback,
     * should leave the wrapped library's own level wide open so it doesn't drop what passes here.
     */
    fun setLevel(level: LogLevel)

    /**
     * Changes the level for a logger name and the names under it, e.g. "com.shop.orders" also
     * covers "com.shop.orders.checkout". A more specific name wins over a shorter one.
     */
    fun setLevel(name: String, level: LogLevel)

    /**
     * Pushes out anything the sink has buffered, e.g. when the app goes to the background.
     */
    fun flush()

    /**
     * Flushes and releases the sink. Call it once, when the app shuts down. Sinks are shared by all
     * the loggers of a factory, so this is here and not on a single logger.
     */
    fun close()
}

/**
 * [LogFactory.provider] as T, or null if it is a different type.
 */
inline fun <reified T> LogFactory.providerAs(): T? = provider as? T

/**
 * Creates console loggers. Simple default, use a provider factory such as one for Logback
 * when you need more.
 *
 * kiit-logs has only 1 dependency (kotlinx-datetime).
 *
 *     val logFactory = ConsoleLogFactory(LogSettings.safe(origin = "shop.example.com"))
 *
 * Loggers are cached by name, so getLogger returns the same logger for the same name.
 */
@OptIn(ExperimentalAtomicApi::class)
class ConsoleLogFactory(settings: LogSettings) : LogFactory {

    @Volatile
    override var settings: LogSettings = settings
        private set

    // Copy-on-write, so reads and lookups need no lock
    private val loggers = AtomicReference<Map<String, Logger>>(emptyMap())

    /**
     * Can't return an singleton of Console
     */
    override val provider: Any = "console"

    private val sink = ConsoleSink()

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

    override fun flush() = sink.flush()

    override fun close() = sink.close()

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
