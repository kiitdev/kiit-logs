/**
 *  <kiit_header>
 * url: www.kiit.dev
 * git: www.github.com/slatekit/kiit
 * org: www.codehelix.co
 * author: Kishore Reddy
 * copyright: 2016 CodeHelix Solutions Inc.
 * license: refer to website and/or github
 *  </kiit_header>
 */

package kiit.logs

import kotlin.time.Clock
import kotlin.time.Instant

/**
 * @param name logger name, e.g. the class
 * @param prefix the start of the entry, e.g. `ACTION: place_order`. Null for free text
 * @param source where the entry comes from: the origin from [LogSettings.source] and the scope, which is the
 *               logger's own scope when it has one, e.g. "orders.checkout"
 * @param fields key/value pairs, already redacted
 * @param msg free text, used when there is no prefix or as detail for one
 * @param time when the entry was created, from [LogSettings.clock]
 * @param trace the exception rendered per [LogSettings.stackTraces], null when there is none or it is Off.
 *              A sink that prints exceptions should use this, so the setting applies to it too
 * @param thread name of the thread that created the entry: the thread name on the JVM and Android, the
 *               NSThread name on Apple, or "main" for the main thread. Empty when the thread has no name.
 *               Not part of [text]
 */
data class LogEntry(
    val name: String = "",
    val level: LogLevel,
    val msg: String = "",
    val ex: Throwable? = null,
    val prefix: Prefix? = null,
    val source: Source = Source.DEFAULT,
    val fields: List<Pair<String, Any?>> = emptyList(),
    val time: Instant = Clock.System.now(),
    val trace: String? = null,
    val thread: String = ""
) {
    /**
     * Display form of the body of a console line, built from the parts that are set and joined with ", ":
     *
     * 1. The prefix as "label: value", e.g. "ACTION: place_order"
     * 2. The message. With no prefix it is the text as it is. With one it is detail, as msg="low stock"
     * 3. The fields as k=v
     * 4. The logger name as logger=name, when there is one
     *
     * e.g. `ACTION: place_order, msg="low stock", order_id=abc, total=42, logger=OrderService`. The source is the
     * header of a line, see [Source.text], and is not part of this. The keys msg and logger are reserved here, so a
     * field with one of those keys looks the same as the part. The fields are built for the console only, a sink
     * that wants the parts reads them from the entry.
     */
    val text: String
        get() {
            val head = prefix?.let { "${it.label}: ${it.value}" }
            val detail =
                when {
                    msg.isEmpty() -> null
                    prefix == null -> msg
                    else -> "msg=\"${escaped(msg)}\""
                }
            val pairs = fields.map { "${it.first}=${it.second}" }
            val logger = if (name.isEmpty()) null else "logger=$name"
            return (listOfNotNull(head, detail) + pairs + listOfNotNull(logger)).joinToString(", ")
        }

    private fun escaped(value: String): String = value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")
}
