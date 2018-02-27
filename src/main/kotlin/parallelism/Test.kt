package parallelism

import parallelism.Par.fork
import parallelism.Par.map2
import java.util.concurrent.Executors

fun sum(ints: List<Int>): ParType<Int> =
        if (ints.size <= 1) Par.unit(ints[0])
        else {
            val left = ints.subList(0, ints.size / 2)
            val right = ints.subList(ints.size / 2, ints.size)
            println(left)
            println(right)
            map2(fork({ sum(left) }), fork({ sum(right) }), { a, b -> a + b })
        }

fun main(args: Array<String>) {
    val s = sum(listOf(1, 10, 20, 30, 40, 50))
    val result = Par.run(Executors.newFixedThreadPool(5), s).get()
    println(result)
}