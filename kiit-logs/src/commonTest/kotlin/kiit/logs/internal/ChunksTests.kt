package kiit.logs.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChunksTests {
    private fun pieces(text: String, max: Int) = chunkText(text, max).map { it.replace("\n", "|") }

    @Test
    fun short_text_is_one_piece() {
        assertEquals(listOf("abc"), chunkText("abc", 10))
        assertEquals(listOf("abc"), chunkText("abc", 3))
    }

    @Test
    fun no_limit_means_one_piece() {
        val text = "x".repeat(50)
        assertEquals(listOf(text), chunkText(text, 0))
        assertEquals(listOf(text), chunkText(text, -1))
    }

    @Test
    fun whole_lines_are_grouped_up_to_the_limit() {
        val lines = (1..6).joinToString("\n") { "line$it".padEnd(9, '.') }
        assertEquals(listOf("line1....|line2....|line3....", "line4....|line5....|line6...."), pieces(lines, 30))
    }

    @Test
    fun a_line_longer_than_the_limit_is_cut() {
        assertEquals(listOf(10, 10, 5), chunkText("y".repeat(25), 10).map { it.length })
        assertEquals("y".repeat(25), chunkText("y".repeat(25), 10).joinToString(""))
    }

    @Test
    fun a_long_line_after_short_ones_starts_a_new_piece() {
        assertEquals(listOf("ab", "cccc", "cccc", "cc"), pieces("ab\n" + "c".repeat(10), 4))
    }

    @Test
    fun blank_lines_are_kept() {
        assertEquals(listOf("a||b", "c"), pieces("a\n\nb\n\nc", 4))
    }

    @Test
    fun the_boundary_is_inclusive() {
        assertEquals(1, chunkText("12345\n67890", 11).size)
        assertEquals(listOf("12345", "67890"), pieces("12345\n67890", 10))
    }

    @Test
    fun a_surrogate_pair_is_never_split() {
        val text = "a" + "😀".repeat(6)
        val parts = chunkText(text, 4)
        assertEquals(text, parts.joinToString(""))
        parts.forEach { assertTrue(it.isEmpty() || !it.last().isHighSurrogate(), "ends with half a pair") }
        assertTrue(parts.all { it.length <= 4 })
    }

    @Test
    fun no_text_is_lost_or_added_when_lines_are_joined_back() {
        val lines = (1..6).joinToString("\n") { "line$it".padEnd(9, '.') }
        assertEquals(lines, chunkText(lines, 30).joinToString("\n"))
    }
}
