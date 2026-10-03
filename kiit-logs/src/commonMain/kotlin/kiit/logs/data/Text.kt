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

package kiit.logs.data

import kiit.logs.LogData

/**
 * Free form text. Prefer an [Action] or an [Event], which log a name and fields that can be searched.
 *
 *     log.info(Text("cache warmed"))
 *     log.error(Text("charge failed", ex))
 *
 * @param msg the text
 * @param ex the exception, if there is one. Its message is added after the text
 */
class Text(
    override val msg: String,
    override val ex: Throwable? = null
) : LogData
