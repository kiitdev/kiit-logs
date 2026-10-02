package kiit.logs

/**
 * Java-friendly overload of [LogFactory.logger] that takes a [Class].
 */
fun LogFactory.logger(cls: Class<*>): Logger = logger(cls.kotlin)
