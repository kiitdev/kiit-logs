package kiit.logs

object LogUtils {
    /**
     * Redacts then renders key/value pairs into "structured value"
     * e.g. a=1, b=2, c=3 etc for easier searches in logs
     * NOTE: Logs can be configured to output JSON and/or provide structured arguments.
     * This varies from logging provider so this is an easier text/classic only way to do ( for now )
     */
    fun format(pairs:List<Pair<String, Any?>>, redaction:Redactor = Redaction()):String =
        render(redaction.redact(pairs))

    /**
     * Renders already redacted key/value pairs as a=1, b=2
     */
    fun render(fields:List<Pair<String, Any?>>):String =
        fields.joinToString(", ") { "${toKey(it.first)}=${it.second}" }

    @Suppress("NOTHING_TO_INLINE")
    inline fun toKey(key:String):String = key.trim().lowercase().replace(" ", "-")
}
