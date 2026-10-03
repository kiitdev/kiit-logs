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
    val raw: Any

    /**
     * The logger for a name. No name and null both give the logger named [DEFAULT_NAME].
     *
     * @param scope where in the origin this logger's entries come from, e.g. "orders.payment", in place of the
     *              scope of [LogSettings.source]. Null, the default, keeps that scope. The same name with a
     *              different scope is a different logger. Levels are still set by name.
     */
    fun logger(name: String? = DEFAULT_NAME, scope: String? = null): Logger

    /**
     * The logger for a class, named by its qualified name. A class that has none, e.g. an anonymous object,
     * gets the logger named [DEFAULT_NAME].
     *
     * @param scope as for the logger of a name. The same scope on every platform keeps the entries of shared code
     *              the same, where class names can differ
     */
    fun logger(cls: KClass<*>, scope: String? = null): Logger

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
     *
     * A null level removes the level of that name, atomically. The name then follows the longest remaining
     * name that is a prefix of it, or the global level if there is none. Clearing a name that was
     * never set changes nothing.
     */
    fun setLevel(name: String, level: LogLevel?)

    /**
     * Same as [setLevel] by name, for a class named the way [logger] names it. A class that has no name,
     * e.g. an anonymous object, sets the name [DEFAULT_NAME] and the names under it, not the level of every logger.
     */
    fun setLevel(cls: KClass<*>, level: LogLevel?) = setLevel(nameOf(cls), level)

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
 * The logger name of a class: its qualified name, else its simple name, else [LogFactory.DEFAULT_NAME].
 */
internal fun nameOf(cls: KClass<*>): String = cls.qualifiedName ?: cls.simpleName ?: LogFactory.DEFAULT_NAME

/**
 * [LogFactory.raw] as T, or null if it is a different type.
 */
inline fun <reified T> LogFactory.rawAs(): T? = raw as? T
