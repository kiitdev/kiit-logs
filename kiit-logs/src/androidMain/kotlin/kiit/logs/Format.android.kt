package kiit.logs

internal actual fun formatMessage(msg: String, args: Array<out Any?>): String = msg.format(*args)
