package kiit.logs.factories

import kiit.logs.Logger

/**
 * Java-friendly overload of [LogFactory.getLogger] that takes a [Class].
 */
fun LogFactory.getLogger(cls: Class<*>): Logger = getLogger(cls.kotlin)
