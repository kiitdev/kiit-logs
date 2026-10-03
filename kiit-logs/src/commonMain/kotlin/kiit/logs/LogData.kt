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
 * What is logged. A [Logger] turns any [LogData] into an entry, so this is the one input type of its log methods.
 * It is open, so another module can add its own, e.g. an adapter for a result type. Every member has a default,
 * so a type overrides only the ones it has.
 *
 * The built-in types are in `kiit.logs.data`: `Action` for something about to be done, `Event` for something that
 * has happened, and `Text` for free form text.
 */
interface LogData {
    /**
     * The start of the entry, e.g. `ACTION: place_order`. Null when there is none, as for free text.
     */
    val prefix: Prefix? get() = null

    /**
     * Free text, or detail for the prefix. Empty when there is none.
     */
    val msg: String get() = ""

    /**
     * The exception, if there is one.
     */
    val ex: Throwable? get() = null

    /**
     * Key/value pairs, redacted by the logger's policies before the entry is delivered.
     */
    val fields: List<Pair<String, Any?>> get() = emptyList()
}
