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
 * Decides which key/value fields are masked or dropped before an entry is created. Every logger
 * and provider only ever sees the fields it returns.
 *
 * [Redaction] is the default, it matches on the field key. To use your own rule instead, e.g. one
 * that also looks at values, pass a Redactor to [LogSettings]:
 *
 *     LogSettings.safe().copy(redaction = Redactor { fields -> fields.filter { it.first != "card" } })
 */
fun interface Redactor {
    fun redact(fields: List<Pair<String, Any?>>): List<Pair<String, Any?>>
}
