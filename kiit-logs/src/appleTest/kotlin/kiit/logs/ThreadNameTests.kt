package kiit.logs

import kiit.logs.internal.currentThreadName
import platform.Foundation.NSThread
import kotlin.test.Test
import kotlin.test.assertEquals

class ThreadNameTests {
    @Test
    fun the_main_thread_is_named_main_when_it_has_no_name() {
        if (NSThread.isMainThread && NSThread.currentThread.name.isNullOrEmpty()) {
            assertEquals("main", currentThreadName())
        }
    }
}
