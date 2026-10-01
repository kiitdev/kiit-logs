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

package kiit.logs.internal

/**
 * Splits text into pieces of at most [max] characters, for outputs that cut long messages, e.g. logcat.
 * It breaks on line ends where it can, so a multi line entry stays readable, and only cuts inside a
 * line when that line alone is longer than [max]. It never splits a surrogate pair. A [max] of 0 or
 * less means no limit.
 */
internal fun chunkText(text: String, max: Int): List<String> {
    if (max <= 0 || text.length <= max) return listOf(text)
    val chunks = mutableListOf<String>()
    val current = StringBuilder()

    fun finishChunk() {
        if (current.isNotEmpty()) {
            chunks.add(current.toString())
            current.setLength(0)
        }
    }

    for (line in text.split('\n')) {
        var rest = line
        while (rest.length > max) {
            finishChunk()
            var cut = max
            if (cut > 1 && rest[cut - 1].isHighSurrogate()) cut--
            chunks.add(rest.substring(0, cut))
            rest = rest.substring(cut)
        }
        val length = if (current.isEmpty()) rest.length else current.length + 1 + rest.length
        if (length > max) finishChunk()
        if (current.isNotEmpty()) current.append('\n')
        current.append(rest)
    }
    finishChunk()
    return chunks
}
