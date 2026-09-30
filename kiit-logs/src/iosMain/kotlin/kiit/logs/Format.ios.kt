package kiit.logs

internal actual fun formatMessage(msg: String, args: Array<out Any?>): String {
    val out = StringBuilder()
    var next = 0
    var i = 0
    while (i < msg.length) {
        val c = msg[i]
        val spec = if (c == '%' && i + 1 < msg.length) msg[i + 1] else null
        when {
            spec == '%' -> {
                out.append('%')
                i += 2
            }
            (spec == 's' || spec == 'd' || spec == 'b') && next < args.size -> {
                out.append(args[next++].toString())
                i += 2
            }
            else -> {
                out.append(c)
                i++
            }
        }
    }
    return out.toString()
}
