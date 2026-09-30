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
 * How a key is compared to the sensitive [Redaction.keys]. Both are normalized first: lowercased, with
 * spaces, `_`, `-` and `.` removed, so "api_key", "api-key" and "apiKey" are the same key.
 */
enum class KeyMatch {
    /** Key contains a sensitive word, e.g. "user_password" matches "password". */
    Contains,

    /** Key equals a sensitive word. */
    Exact,

    /** Key ends with a sensitive word, e.g. "access_token" matches "token". */
    Suffix
}

/** What happens to a sensitive field. */
enum class RedactAction {
    /** Keep the key, replace the value with [Redaction.replacement]. */
    Mask,

    /** Remove the field. */
    Drop
}

/**
 * Redacts sensitive key/value fields before a [LogEntry] is created, so every logger and
 * provider only ever sees redacted fields.
 * NOTE: This is key-based only. Values inside message text or format arguments are not scanned.
 *
 * @param keys sensitive words, e.g. Redaction.defaults + "ssn"
 * @param match how a field key is compared to [keys]
 * @param action mask the value or drop the field
 * @param replacement value used by [RedactAction.Mask]
 */
data class Redaction(
    val keys: Set<String> = defaults,
    val match: KeyMatch = KeyMatch.Contains,
    val action: RedactAction = RedactAction.Mask,
    val replacement: String = "***"
) {
    private val normalized: List<String> = keys.map { normalize(it) }

    fun isSensitive(key: String): Boolean {
        val k = normalize(key)
        return normalized.any {
            when (match) {
                KeyMatch.Contains -> k.contains(it)
                KeyMatch.Exact -> k == it
                KeyMatch.Suffix -> k.endsWith(it)
            }
        }
    }

    fun redact(fields: List<Pair<String, Any?>>): List<Pair<String, Any?>> = when (action) {
        RedactAction.Mask -> fields.map { (k, v) -> if (isSensitive(k)) k to replacement else k to v }
        RedactAction.Drop -> fields.filterNot { isSensitive(it.first) }
    }

    companion object {
        val defaults: Set<String> = setOf(
            "username", "email", "phone", "password", "pswd", "firstname", "lastname",
            "token", "secret", "apikey", "authorization", "cookie", "ssn"
        )

        private fun normalize(key: String): String = key.lowercase().filter { it.isLetterOrDigit() }
    }
}
