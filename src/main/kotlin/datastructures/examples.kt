import datastructures.Cons
import datastructures.List
import datastructures.Nil

fun sum(ints: List<Int>): Int = ints.foldRight(0) { x, y -> x + y }
val sumF = { list: List<Int> -> list.foldLeft(0) { x, y -> x + y } }

fun product(doubles: List<Double>): Double = when (doubles) {
    Nil -> 1.0
    is Cons -> product(doubles.tail) * doubles.head
}