package parsing

import org.junit.Assert.assertTrue
import org.junit.Test

class JSONTest {

    @Test
    fun test_literal_null() {
        val json = JSON(Reference)
        val result = Reference.run(json.literal, "null")
        result.map { assertTrue(it === JNull) }
    }

    @Test
    fun test_literal_string() {
        val json = JSON(Reference)
        val result = Reference.run(json.literal, "something")
        result.map { assertTrue(it === JString("something")) }
    }

    @Test
    fun test_literal_number() {
        val json = JSON(Reference)
        val result = Reference.run(json.literal, "1.15")
        result.map {
            assertTrue(it is JNumber)
            println(it)
        }
    }

    @Test
    fun test_literal_true() {
        val json = JSON(Reference)
        val result = Reference.run(json.literal, "true")
        result.map {
            assertTrue(it is JBool)
            println(it)
        }
    }

    @Test
    fun test_obj() {
        val json = JSON(Reference)
        val result = Reference.run(json.obj, "{ \"a\": 12 }")
        result.map {
            assertTrue(it is JObject)
        }
        println(result)
    }

    @Test
    fun test_keyval() {
        val json = JSON(Reference)
        val result = Reference.run(json.keyval, "\"a\": 12")
        result.map {
            assertTrue(it is Pair)
        }
        println(result)
    }

    @Test
    fun test_array() {
        val json = JSON(Reference)
        val result = Reference.run(json.array, "[1, 2, 3]")
        result.map {
            assertTrue(it is JArray)
        }
        println(result)
    }

}