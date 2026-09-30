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

interface Logs {

    /**
     * The underlying logging implementation, e.g. "console" or a Logback instance,
     * to access the raw provider.
     */
    val provider: Any

    @Suppress("UNCHECKED_CAST")
    fun <T> providerAs(): T = provider as T

    fun getLogger(name: String? = ""): Logger
    fun getLogger(cls: KClass<*>): Logger
}

/**
 * Simple console logger as a default.
 * Use kiit.providers.logs.LogbackLogs as provider for LogBack
 *
 * kiit-logs has only 1 dependency (kotlinx-datetime).
 */
object LogsDefault : Logs {

    /**
     * Can't return an singleton of Console
     */
    override val provider: Any = "console"

    override fun getLogger(cls: KClass<*>): Logger {
        return LoggerConsole(name = cls.simpleName ?: "console", logType = cls)
    }

    override fun getLogger(name: String?): Logger {
        return LoggerConsole(name = name ?: "console")
    }
}
