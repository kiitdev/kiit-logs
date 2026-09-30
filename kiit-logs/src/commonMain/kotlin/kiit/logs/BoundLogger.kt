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
 * A view of [parent] that adds fixed fields to every entry. See [Logger.with].
 */
internal class BoundLogger(
    private val parent: Logger,
    private val bound: List<Pair<String, Any?>>
) : Logger(parent.settings, parent.name, parent.logType) {

    // Reads and writes go to the parent, so runtime level changes apply here too
    override var settings: LogSettings
        get() = parent.settings
        set(value) {
            parent.settings = value
        }

    override val raw: Any? get() = parent.raw

    override fun emit(entry: LogEntry) {
        parent.emit(entry.copy(fields = settings.redaction.redact(bound) + entry.fields))
    }
}
