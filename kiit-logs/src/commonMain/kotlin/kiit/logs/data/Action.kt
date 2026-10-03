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
 * Something about to be done, e.g. placing an order. It is logged as `ACTION: place_order`.
 *
 *     log.info(Action("place_order", "order_id" to id))
 *
 * @param name the name, e.g. "place_order"
 * @param fields key/value pairs
 * @param msg detail, logged as msg="..." after the name
 * @param ex the exception, if there is one
 */
class Action(
    val name: String,
    vararg fields: Pair<String, Any?>,
    override val msg: String = "",
    override val ex: Throwable? = null
) : LogData {
    override val prefix: Prefix = Prefix(Prefix.ACTION, name)
    override val fields: List<Pair<String, Any?>> = fields.asList()
}
