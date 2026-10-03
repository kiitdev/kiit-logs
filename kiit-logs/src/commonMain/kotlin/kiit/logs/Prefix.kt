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
 * The label is a plain string, so a type of your own can use any label. The labels of the built-in types are
 * constants here, for code that treats them differently:
 *
 *     when (entry.prefix?.label) {
 *         Prefix.ACTION -> ...
 *         Prefix.EVENT -> ...
 *         else -> ...
 *     }
 *
 * @param label what the value is, e.g. [ACTION] or [EVENT]
 * @param value the name, e.g. "place_order"
 */
data class Prefix(val label: String, val value: String) {
    companion object {
        /** The label of an `Action`, something about to be done. */
        const val ACTION = "ACTION"

        /** The label of an `Event`, something that has happened. */
        const val EVENT = "EVENT"
    }
}
