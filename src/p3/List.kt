package p3

sealed class List<out T> {

    override fun toString(): String = when (this) {
        Nil -> "-|"
        is Cons -> "${this.head} -> ${this.tail}"
    }

    fun tail(): List<T> = when (this) {
        Nil -> throw RuntimeException("No tail on Nil")
        is Cons -> this.tail
    }

    fun <R> map(f: (T) -> R): List<R> =
            foldRight(Nil as List<R>, { element, result -> Cons(f(element), result) })

    fun <R> foldRight(identity: R, f: (T, R) -> R): R = when (this) {
        Nil -> identity
        is Cons -> f(this.head, foldRight(this.tail, identity, f))
    }
}

object Nil : List<Nothing>()
class Cons<out T>(val head: T, val tail: List<T>) : List<T>()

fun sum(ints: List<Int>): Int = foldRight(ints, 0, { x, y -> x + y })
val sumF = { list: List<Int> -> foldLeft(list, 0, { x, y -> x + y }) }

fun product(doubles: List<Double>): Double = when (doubles) {
    Nil -> 1.0
    is Cons -> product(doubles.tail) * doubles.head
}

fun <T, R> foldRight(list: List<T>, identity: R, f: (T, R) -> R): R = when (list) {
    Nil -> identity
    is Cons -> f(list.head, foldRight(list.tail, identity, f))
}

fun <T, R> foldLeft(list: List<T>, identity: R, f: (T, R) -> R): R {
    tailrec fun flAcc(list: List<T>, acc: R): R =
            when (list) {
                Nil -> acc
                is Cons -> {
                    val result = f(list.head, acc)
                    flAcc(list.tail, result)
                }
            }

    return flAcc(list, identity)
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

fun <T> appendFR(list1: List<T>, list2: List<T>): List<T> =
        foldRight(list1, list2, { element, list -> Cons(element, list) })

fun <T> init(list: List<T>): List<T> =
        when (list) {
            Nil -> Nil
            is Cons -> when (list.tail) {
                Nil -> Nil
                is Cons -> Cons(list.head, init(list.tail))
            }
        }

fun <T> length(list: List<T>) = foldRight(list, 0, { x, y -> 1 + y })
fun <T> reverse(list: List<T>) = foldLeft<T, List<T>>(list, Nil, { x, y -> Cons(x, y) })

fun <T> concat(list: List<List<T>>): List<T> {
    return foldRight<List<T>, List<T>>(list, Nil, { element, result ->
        append(element, result)
    })
}

fun transformInt(list: List<Int>): List<Int> =
        foldRight<Int, List<Int>>(list, Nil, { element, result -> Cons(element + 1, result) })

fun doubleList(list: List<Double>): List<String> =
        foldRight<Double, List<String>>(list, Nil, { element, result -> Cons(element.toString(), result) })

fun <T, R> map(list: List<T>, f: (T) -> R): List<R> =
        foldRight<T, List<R>>(list, Nil, { element, result -> Cons(f(element), result) })

fun <T> filter(list: List<T>, f: (T) -> Boolean): List<T> =
        foldRight<T, List<T>>(list, Nil, { element, result -> if (f(element)) Cons(element, result) else result })

fun pairwise(list1: List<Int>, list2: List<Int>): List<Int> = when (list1) {
    Nil -> list2
    is Cons -> when (list2) {
        Nil -> list1
        is Cons -> Cons(list1.head + list2.head, pairwise(list1.tail, list2.tail))
    }
}

fun <T> zipWith(list1: List<T>, list2: List<T>, f: (T, T) -> T): List<T> = when (list1) {
    Nil -> list2
    is Cons -> when (list2) {
        Nil -> list1
        is Cons -> Cons(f(list1.head, list2.head), zipWith(list1.tail, list2.tail, f))
    }
}

fun main(args: Array<String>) {
    /*var list: List<Int> = Nil
    for (i in 1..10) {
        list = Cons(i, list)
    }*/
    val list1 = apply(1, 2, 3, 99, 100)
    val list2 = apply(5, 6, 7)
    val list3 = apply(8, 9, 10)
    val l = apply(list1, list2, list3)
    println(l)
    println(filter(map(list1, { x -> x * 10 }), { it < 30 }))
    println(pairwise(list1, list2))
    println(zipWith(list1, list2, { x, y -> x * y }))
}