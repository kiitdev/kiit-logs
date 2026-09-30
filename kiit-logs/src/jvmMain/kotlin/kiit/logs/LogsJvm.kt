package kiit.logs

/**
 * Java-friendly overload of [Logs.getLogger] that takes a [Class].
 */
fun Logs.getLogger(cls: Class<*>): Logger = getLogger(cls.kotlin)
