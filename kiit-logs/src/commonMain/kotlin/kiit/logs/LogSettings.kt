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

/**
 * Settings for a [Logger]. Passed as the primary constructor argument so new options
 * can be added here without changing every logger's constructor.
 *
 * @param level minimum level that is logged
 */
data class LogSettings(
    val level: LogLevel = LogLevel.Warn
)
