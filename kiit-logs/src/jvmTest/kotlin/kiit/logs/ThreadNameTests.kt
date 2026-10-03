package kiit.logs

import kiit.logs.sinks.MemorySink
import kotlin.test.Test
import kotlin.test.assertEquals

class ThreadNameTests {
    @Test
    fun entry_has_the_name_of_the_thread_that_logged_it() {
        val sink = MemorySink()
        val log = testLogger(testSettings(), sink)
        val worker = Thread { log.info("place") }
        worker.name = "worker-1"
        worker.start()
        worker.join()
        assertEquals("worker-1", sink.entries.single().thread)
    }
}
