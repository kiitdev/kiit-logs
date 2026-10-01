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

package kiit.logs.policies

import kiit.logs.LogEntry
import kiit.logs.LogSettings

/**
 * How a key is compared to the sensitive [RedactPolicy.keys]. Both are normalized first: lowercased, with
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
    /** Keep the key, replace the value with [RedactPolicy.replacement]. */
    Mask,

    /** Remove the field. */
    Drop
}

/**
 * A [Policy] that redacts the sensitive key/value fields of an entry. It is the first of the default
 * [LogSettings.policies], so every policy after it and every sink only ever sees redacted fields. To use your
 * own rule instead, e.g. one that also looks at values, return a copy of the entry with other fields:
 *
 *     Policy { entry -> entry.copy(fields = entry.fields.map { (k, v) -> if (v is String && "@" in v) k to "<email>" else k to v }) }
 *
 * NOTE: This is key-based only. Values inside message text or format arguments are not scanned.
 *
 * @param keys sensitive words, e.g. RedactPolicy.defaults + "ssn"
 * @param match how a field key is compared to [keys]
 * @param action mask the value or drop the field
 * @param replacement value used by [RedactAction.Mask]
 */
data class RedactPolicy(
    val keys: Set<String> = defaults,
    val match: KeyMatch = KeyMatch.Contains,
    val action: RedactAction = RedactAction.Mask,
    val replacement: String = "***"
) : Policy {
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

    /**
     * The same entry when no field is sensitive, otherwise a copy with the redacted fields.
     */
    override fun apply(entry: LogEntry): LogEntry? {
        if (entry.fields.none { isSensitive(it.first) }) return entry
        return entry.copy(fields = redact(entry.fields))
    }

    fun redact(fields: List<Pair<String, Any?>>): List<Pair<String, Any?>> =
        when (action) {
            RedactAction.Mask -> fields.map { (k, v) -> if (isSensitive(k)) k to replacement else k to v }
            RedactAction.Drop -> fields.filterNot { isSensitive(it.first) }
        }

    companion object {
        val defaults: Set<String> =
            setOf(
                "username", "email", "phone", "password", "pswd", "firstname", "lastname",
                "token", "secret", "apikey", "authorization", "cookie", "ssn",
            )

        private fun normalize(key: String): String = key.lowercase().filter { it.isLetterOrDigit() }
    }
}
