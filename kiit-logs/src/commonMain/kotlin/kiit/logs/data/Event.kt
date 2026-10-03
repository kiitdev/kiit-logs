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
import kiit.logs.Prefix

/**
 * Something that has happened, e.g. an order placed. It is logged as `EVENT: order_placed`.
 *
 *     log.info(Event("order_placed", "order_id" to id, "total" to 42))
 *
 * @param name the name, e.g. "order_placed"
 * @param fields key/value pairs
 * @param msg detail, logged as msg="..." after the name
 * @param ex the exception, if there is one
 */
class Event(
    val name: String,
    vararg fields: Pair<String, Any?>,
    override val msg: String = "",
    override val ex: Throwable? = null
) : LogData {
    override val prefix: Prefix = Prefix(Prefix.EVENT, name)
    override val fields: List<Pair<String, Any?>> = fields.asList()
}
