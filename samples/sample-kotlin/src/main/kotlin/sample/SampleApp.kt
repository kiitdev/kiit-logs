package sample

import kiit.logs.LogEntry
import kiit.logs.LogLevel
import kiit.logs.LogSettings
import kiit.logs.Logs
import kiit.logs.policies.KeyMatch
import kiit.logs.policies.Policy
import kiit.logs.policies.RedactAction
import kiit.logs.policies.RedactPolicy
import kiit.logs.policies.StackTraces
import kiit.logs.sinks.CompositeSink
import kiit.logs.sinks.ConsoleSink
import kiit.logs.sinks.LogSink

fun main() {
    structured()
    freeText()
    levels()
    boundFields()
    redaction()
    policies()
    stackTraces()
    sinks()
    service()
}

private fun section(title: String) = println("\n== $title")

// Safe settings log only errors, so the samples turn the level up. origin and scope are stamped on every entry
private fun settings(level: LogLevel = LogLevel.Debug) = LogSettings.safe(origin = "shop.example.com", scope = "orders").copy(level = level)

private fun expensive() = 6 * 7

// An action plus key/value fields: what was attempted, then the values that matter
fun structured() {
    section("Structured logging")
    val log = Logs.console(settings()).getLogger("OrderService")
    log.info("place", "order_id" to "abc", "total" to 42)
    log.warn("place", "order_id" to "abc", "reason" to "low stock")
    // The fields lambda only runs when Debug is enabled
    log.debug("place") { listOf("total" to expensive()) }
}

// Free text is also supported, with log(level, ...)
fun freeText() {
    section("Free text")
    val log = Logs.console(settings()).getLogger("Free")
    log.log(LogLevel.Info, "app started")
    log.log(LogLevel.Error, "payment failed", IllegalStateException("card declined"))
    log.log(LogLevel.Debug, "cache") { "hits=${expensive()}" }
}

// One level for everything, a level per logger name, and changing both while the app runs
fun levels() {
    section("Levels")
    val logs = Logs.console(settings(LogLevel.Warn))
    val orders = logs.getLogger("shop.orders")
    val card = logs.getLogger("shop.payments.card")

    orders.info("before", "note" to "Info is below Warn, not printed")
    logs.setLevel("shop.payments", LogLevel.Debug)
    card.debug("charge", "note" to "this name and the names under it are now at Debug")
    orders.debug("charge", "note" to "not printed, still at Warn")

    logs.setLevel(LogLevel.Debug)
    orders.debug("after", "note" to "the global level was changed at runtime")
}

// A logger that adds fixed fields to every entry, e.g. one request
fun boundFields() {
    section("Bound fields")
    val log = Logs.console(settings()).getLogger("Request")
    val request = log.with("trace_id" to "t-123")
    request.info("place", "order_id" to "abc")
    request.info("pay", "order_id" to "abc")
}

// The default policy masks sensitive keys. Replace it to change what is masked or dropped, or add your own
fun redaction() {
    section("Redaction")
    val fields = arrayOf("email" to "a@b.com", "account_no" to "1234", "plan" to "pro")

    val byDefault = Logs.console(settings()).getLogger("Default")
    byDefault.info("signup", *fields)

    val custom =
        settings().copy(
            policies = listOf(RedactPolicy(keys = RedactPolicy.defaults + "account_no", match = KeyMatch.Suffix, action = RedactAction.Drop)),
        )
    Logs.console(custom).getLogger("Dropped").info("signup", *fields)

    val values =
        Policy { entry ->
            entry.copy(fields = entry.fields.map { (k, v) -> if (v is String && "@" in v) k to "<email>" else k to v })
        }
    Logs.console(settings().copy(policies = listOf(values))).getLogger("ByValue").info("signup", *fields)
}

// Policies run in list order for every entry. Return the entry, a changed copy, or null to drop it
fun policies() {
    section("Policies")
    val build = Policy { it.copy(fields = it.fields + ("build" to "sample-1")) }
    val noHeartbeat = Policy.filter { it.action != "heartbeat" }
    val settings = settings().let { it.copy(policies = it.policies + build + noHeartbeat) }
    val log = Logs.console(settings).getLogger("Policies")
    log.info("heartbeat")
    log.info("place", "order_id" to "abc", "password" to "hunter2")
}

// How an exception is shown: not at all, the cause chain, or the stack trace capped by maxTraceLines
fun stackTraces() {
    section("Stack traces")
    val ex = IllegalStateException("outer", IllegalArgumentException("inner"))
    listOf(StackTraces.Off, StackTraces.Summary, StackTraces.Full).forEach { mode ->
        println("-- $mode")
        val log = Logs.console(settings().copy(stackTraces = mode, maxTraceLines = 3)).getLogger("Traces")
        log.error("place", ex, "order_id" to "abc")
    }
}

// A sink is where entries end up. Send them to the console and anywhere else
class CountingSink : LogSink {
    var count = 0

    override fun emit(entry: LogEntry) {
        count++
    }
}

fun sinks() {
    section("Sinks")
    val counting = CountingSink()
    val logs = Logs.sink(settings(), CompositeSink(ConsoleSink(), counting))
    val log = logs.getLogger("Sinks")
    log.info("one")
    log.info("two")
    println("the custom sink got ${counting.count} entries")
    logs.flush()
}

// Hold a logger in a class
fun service() {
    section("A service with a logger")
    val users = UserService(Logs.console(settings()).getLogger(UserService::class))
    users.register("a@b.com", "hunter2")
    users.register("a@b.com", "hunter2")
    users.login("a@b.com", "wrong")
    users.login("a@b.com", "hunter2")
    users.delete("a@b.com")
    users.delete("a@b.com")
}
