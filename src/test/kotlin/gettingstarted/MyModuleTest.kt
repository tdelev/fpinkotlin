package gettingstarted

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class MyModuleTest {

    @Test
    fun test_abs_positive() {
        assertThat(MyModule.abs(1), `is`(1))
    }

    @Test
    fun test_abs_negative() {
        assertThat(MyModule.abs(-1), `is`(1))
    }

    @Test
    fun test_factorial() {
        assertThat(MyModule.factorial(5), `is`(120))
    }
}