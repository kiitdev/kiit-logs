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
 * The start of what an entry says, as a label and a value, e.g. `ACTION: place_order`. The label says what kind
 * of thing the value is, so a search for the label finds all of that kind.
 *
 * @param label what the value is, e.g. "ACTION" or "EVENT"
 * @param value the name, e.g. "place_order"
 */
data class Prefix(val label: String, val value: String)
