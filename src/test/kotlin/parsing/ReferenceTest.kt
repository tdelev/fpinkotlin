package parsing

import errorhandling.Left
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test
import parsing.Reference.regex
import parsing.Reference.string

class ReferenceTest {

    @Test
    fun test_string() {
        val input = "text"
        val parser = string("text")
        val result = Reference.run(parser, input)
        result.map { assertEquals(it, input) }
    }

    @Test
    fun test_string_error() {
        val input = "tem"
        val parser = string("text")
        val result = Reference.run(parser, input)
        assertTrue(result is Left)
    }

    @Test
    fun test_regex() {
        val input = "   "
        val parser = regex(Regex("\\s+"))
        val result = Reference.run(parser, input)
        result.map { assertEquals(it, input) }
    }

    @Test
    fun test_regex_error() {
        val input = "abc"
        val parser = regex(Regex("\\s+"))
        val result = Reference.run(parser, input)
        println(result)
        assertTrue(result is Left)
    }

    @Test
    fun test_surround() {
        val input = "{a}"
        val parser = Reference.surround(string("{"), string("}"), { string("a") })
        val result = Reference.run(parser, input)
        result.map { assertEquals("a", it) }
    }

    @Test
    fun test_skip_left() {
        val input = "abxy"
        val parser = Reference.skipLeft(string("ab"), { string("xy") })
        val result = Reference.run(parser, input)
        result.map { assertEquals("xy", it) }
    }

    @Test
    fun test_thru() {
        val input = "\"abXyyy"
        val parser = Reference.thru("X")
        val result = Reference.run(parser, input)
        result.map { assertEquals("\"abX", it) }
    }

    @Test
    fun test_quoted() {
        val input = "\"ab\""
        val parser = Reference.quoted()
        val result = Reference.run(parser, input)
        result.map { assertEquals("ab", it) }
    }

    @Test
    fun test_escapeQuoted() {
        val input = "\"ab\""
        val parser = Reference.escapedQuoted()
        val result = Reference.run(parser, input)
        result.map { assertEquals("ab", it) }
    }

    @Test
    fun test_flatMap() {
        val input = "abcd"
        val parser = Reference.flatMap(string("ab"), {
            string("cd")
        })
        val result = Reference.run(parser, input)
        result.map { assertEquals("cd", it) }
    }

    @Test
    fun test_map2() {
        val input = "abcd"
        val parser = Reference.map2(string("ab"), { string("cd") }, { a, b -> "$a-$b"})
        val result = Reference.run(parser, input)
        result.map { assertEquals("ab-cd", it) }
    }

    @Test
    fun test_product() {
        val input = "abcd"
        val parser = Reference.product(string("ab"), { string("cd") })
        val result = Reference.run(parser, input)
        result.map { assertEquals(Pair("ab", "cd"), it) }
    }

    @Test
    fun test_many() {
        val input = "aaaa "
        val parser = Reference.many1(string("a"))
        val result = Reference.run(parser, input)
        println(result)
        result.map { assertEquals(Pair("ab", "cd"), it) }
    }

    @Test
    fun test_separate() {
        val input = "a,a,a,a"
        val parser = Reference.sep(string("a"), string(","))
        val result = Reference.run(parser, input)
        println(result)
        result.map { assertEquals(Pair("ab", "cd"), it) }
    }
}