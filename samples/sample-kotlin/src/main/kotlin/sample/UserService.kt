package sample

import kiit.logs.Logger

/**
 * A tiny service that holds a [Logger] and logs what it does as an action plus key/value fields. Sensitive
 * keys such as `email` and `password` are masked by the logger's default policies, so they can be passed as is.
 */
class UserService(private val log: Logger) {
    private val users = mutableMapOf<String, String>()

    fun register(email: String, password: String): Boolean {
        if (email in users) {
            log.warn("register", "email" to email, "reason" to "already registered")
            return false
        }
        users[email] = password
        log.info("register", "email" to email, "password" to password, "plan" to "free")
        return true
    }

    fun login(email: String, password: String): Boolean {
        val ok = users[email] == password
        // Lazy fields: only built when Debug is enabled
        log.debug("login") { listOf("email" to email, "known" to (email in users)) }
        if (ok) log.info("login", "email" to email) else log.warn("login", "email" to email, "reason" to "bad credentials")
        return ok
    }

    fun delete(email: String) {
        try {
            check(email in users) { "no such user" }
            users.remove(email)
            log.info("delete", "email" to email)
        } catch (ex: IllegalStateException) {
            // The exception goes in the entry, its message is kept and a stack trace follows the logger's setting
            log.error("delete", ex, "email" to email)
        }
    }
}
