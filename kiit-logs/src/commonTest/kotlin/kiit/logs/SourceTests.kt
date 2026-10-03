package kiit.logs

import kotlin.test.Test
import kotlin.test.assertEquals

class SourceTests {
    @Test
    fun text_is_origin_then_scope() {
        assertEquals("shop.example.com:orders.payment", Source("shop.example.com", "orders.payment").text)
    }

    @Test
    fun the_colon_stays_when_there_is_no_scope() {
        assertEquals("myapp:", Source("myapp").text)
        assertEquals("", Source("myapp").scope)
    }

    @Test
    fun default_is_a_placeholder_origin_with_no_scope() {
        assertEquals(Source("app", ""), Source.DEFAULT)
        assertEquals("app:", Source.DEFAULT.text)
    }

    @Test
    fun a_scope_can_be_replaced_with_copy() {
        assertEquals(Source("myapp", "orders"), Source("myapp", "billing").copy(scope = "orders"))
    }

    @Test
    fun prefix_holds_a_label_and_a_value() {
        assertEquals("ACTION", Prefix("ACTION", "place_order").label)
        assertEquals("place_order", Prefix("ACTION", "place_order").value)
    }
}
