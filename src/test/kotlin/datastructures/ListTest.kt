package datastructures

import gettingstarted.MyModule
import gettingstarted.MyModule.sum
import junit.framework.TestCase.*
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class ListTest {

    @Test
    fun test_equal() {
        val list1 = list(1, 2)
        val list2 = list(1, 2)
        assertTrue(list1.equal(list2))
    }

    @Test
    fun test_not_equal() {
        val list1 = list(1, 2, 3)
        val list2 = list(1, 2)
        assertFalse(list1.equal(list2))
    }

    @Test
    fun test_equal_empty() {
        val list1 = Nil
        val list2 = Nil
        assertTrue(list1.equal(list2))
    }

    @Test
    fun test_head() {
        val list = list(1, 2)
        (list as Cons).let {
            assertThat(it.head, `is`(1))
        }
    }

    @Test
    fun test_tail() {
        val list = list(1, 2)
        val tail = list(2)
        (list as Cons).let {
            assertTrue(it.tail.equal(tail))
        }
    }

    @Test
    fun test_toString() {
        val list = list(1, 2)
        assertEquals(list.toString(), "1 -> 2 -> -|")
    }

    @Test
    fun test_map() {
        val list = list(1, 2)
        val mapped = list.map { it * 2 }
        assertTrue(mapped.equal(list(2, 4)))
    }

    @Test
    fun test_fold_left() {
        val list = list(1, 2, 3)
        val sum = list.foldLeft(0, MyModule::sum)
        assertTrue(sum == 6)
    }

    @Test
    fun test_fold_right() {
        val list = list(1, 2, 3)
        val sum = list.foldRight(0, MyModule::sum)
        assertTrue(sum == 6)
    }

    @Test
    fun test_reduce() {
        val list = list(1, 2, 3)
        val sum = list.reduce(MyModule::sum)
        sum.map {
            println(it)
            assertTrue(it == 6)
        }
    }

    @Test
    fun test_toArray() {
        val list = list(1, 2, 3)
        val array = list.toArray()
        assertTrue(array.size == 3)
    }
}