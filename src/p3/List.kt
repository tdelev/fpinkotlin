package p3

sealed class List<out T> {

    override fun toString(): String = when (this) {
        Nil -> ""
        is Cons -> "${this.head} -> ${this.tail}"
    }

    fun tail(): List<T> = when (this) {
        Nil -> throw RuntimeException("No tail on Nil")
        is Cons -> this.tail
    }
}

object Nil : List<Nothing>()
class Cons<out T>(val head: T, val tail: List<T>) : List<T>()

fun sum(ints: List<Int>): Int = when (ints) {
    Nil -> 0
    is Cons -> ints.head + sum(ints.tail)
}

fun product(doubles: List<Double>): Double = when (doubles) {
    Nil -> 1.0
    is Cons -> when (doubles.head) {
        0.0 -> 0.0
        else -> product(doubles.tail) * doubles.head
    }
}

fun <T> apply(vararg list: T): List<T> {
    if (list.isEmpty()) return Nil
    return Cons(list[0], apply(*list.sliceArray(1 until list.size)))
}

fun <T> setHead(element: T, list: List<T>): List<T> =
        Cons(element, list.tail())

fun <T> drop(list: List<T>, n: Int): List<T> =
        when (n) {
            0 -> list
            else -> drop(list.tail(), n - 1)
        }

fun <T> dropWhile(list: List<T>, predicate: (T) -> Boolean): List<T> =
        when (list) {
            Nil -> Nil
            is Cons -> when (predicate(list.head)) {
                true -> dropWhile(list.tail, predicate)
                else -> list
            }
        }

fun <T> append(list1: List<T>, list2: List<T>): List<T> =
        when (list1) {
            Nil -> list2
            is Cons -> Cons(list1.head, append(list1.tail, list2))
        }

fun <T> init(list: List<T>): List<T> =
        when (list) {
            Nil -> Nil
            is Cons -> when (list.tail) {
                Nil -> Nil
                is Cons -> Cons(list.head, init(list.tail))
            }
        }

fun main(args: Array<String>) {
    val list = apply(1, 4, 8, 12, 9)
    val list2 = apply(5, 10, 15)
    println(dropWhile(list, { it < 12 }))
    println(append(list, list2))
    println(init(list2))
}