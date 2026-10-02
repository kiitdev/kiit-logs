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

    /**
     * The logger for a name. No name and null both give the logger named [DEFAULT_NAME].
     */
    fun logger(name: String? = DEFAULT_NAME): Logger

    /**
     * The logger for a class, named by its qualified name. A class that has none, e.g. an anonymous object,
     * gets the logger named [DEFAULT_NAME].
     */
    fun logger(cls: KClass<*>): Logger

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

    companion object {
        /**
         * The name of the logger when there is no name, like the root logger of other logging libraries.
         */
        const val DEFAULT_NAME = "root"
    }
}

/**
 * [LogFactory.provider] as T, or null if it is a different type.
 */
inline fun <reified T> LogFactory.providerAs(): T? = provider as? T
