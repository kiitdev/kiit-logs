package kiit.logs.data

import kiit.logs.LogData
import kiit.logs.Prefix
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class DataTests {
    @Test
    fun action_has_the_action_label_and_its_name() {
        val data = Action("place_order", "order_id" to "abc", "total" to 42)
        assertEquals(Prefix("ACTION", "place_order"), data.prefix)
        assertEquals(listOf("order_id" to "abc", "total" to 42), data.fields)
        assertEquals("", data.msg)
        assertNull(data.ex)
    }

    @Test
    fun event_has_the_event_label_and_its_name() {
        val data = Event("order_placed", "order_id" to "abc")
        assertEquals(Prefix("EVENT", "order_placed"), data.prefix)
        assertEquals(listOf("order_id" to "abc"), data.fields)
    }

    @Test
    fun the_labels_of_action_and_event_are_the_prefix_constants() {
        assertEquals("ACTION", Prefix.ACTION)
        assertEquals("EVENT", Prefix.EVENT)
        assertEquals(Prefix.ACTION, Action("place_order").prefix.label)
        assertEquals(Prefix.EVENT, Event("order_placed").prefix.label)
    }

    @Test
    fun a_sink_can_tell_an_action_from_an_event_and_from_a_custom_label() {
        val job =
            object : LogData {
                override val prefix = Prefix("JOB", "nightly")
            }
        val kinds =
            listOf(Action("a"), Event("e"), job, Text("t")).map {
                when (it.prefix?.label) {
                    Prefix.ACTION -> "action"
                    Prefix.EVENT -> "event"
                    null -> "none"
                    else -> "other"
                }
            }
        assertEquals(listOf("action", "event", "other", "none"), kinds)
    }

    @Test
    fun action_and_event_take_a_msg_and_an_exception_by_name() {
        val ex = IllegalStateException("boom")
        val action = Action("place_order", "order_id" to "abc", msg = "low stock", ex = ex)
        assertEquals("low stock", action.msg)
        assertSame(ex, action.ex)
        assertEquals(listOf("order_id" to "abc"), action.fields)

        val event = Event("order_failed", msg = "declined", ex = ex)
        assertEquals("declined", event.msg)
        assertSame(ex, event.ex)
        assertEquals(emptyList(), event.fields)
    }

    @Test
    fun a_name_alone_has_no_fields() {
        assertEquals(emptyList(), Action("ping").fields)
        assertEquals(emptyList(), Event("pinged").fields)
    }

    @Test
    fun text_has_no_prefix_and_no_fields() {
        val ex = IllegalStateException("boom")
        val data = Text("charge failed", ex)
        assertNull(data.prefix)
        assertEquals("charge failed", data.msg)
        assertSame(ex, data.ex)
        assertEquals(emptyList(), data.fields)
        assertNull(Text("cache warmed").ex)
    }

    @Test
    fun a_custom_type_overrides_only_the_members_it_has() {
        val custom =
            object : LogData {
                override val prefix = Prefix("JOB", "nightly")
            }
        assertEquals(Prefix("JOB", "nightly"), custom.prefix)
        assertEquals("", custom.msg)
        assertNull(custom.ex)
        assertEquals(emptyList(), custom.fields)
    }
}
