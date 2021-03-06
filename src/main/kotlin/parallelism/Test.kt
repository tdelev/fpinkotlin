package parallelism

import java.util.concurrent.Executors

fun sum(ints: List<Int>): Par<Int> =
        if (ints.size <= 1) unit(ints[0])
        else {
            val left = ints.subList(0, ints.size / 2)
            val right = ints.subList(ints.size / 2, ints.size)
            println(left)
            println(right)
            map2(fork { sum(left) }, fork { sum(right) }) { a, b -> a + b }
        }

fun main(args: Array<String>) {
    val s = sum(listOf(1, 10, 20, 30, 40, 50))
    val result = runBlocking(Executors.newFixedThreadPool(5), s).get()
    println(result)
}