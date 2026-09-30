package kiit.logs

/**
 * Formats a message template with printf-style args, e.g. formatMessage("id=%s", arrayOf(1)).
 * JVM and Android use String.format. iOS supports %s, %d, %b and %%, other specifiers are left as-is.
 */
internal expect fun formatMessage(msg: String, args: Array<out Any?>): String
