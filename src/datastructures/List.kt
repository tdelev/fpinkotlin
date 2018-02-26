package datastructures

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
            foldRight<List<R>>(Nil, { element, result -> Cons(f(element), result) })

    fun <R> foldRight(identity: R, f: (T, R) -> R): R = when (this) {
        Nil -> identity
        is Cons -> f(this.head, this.tail.foldRight(identity, f))
    }

    fun all(predicate: (T) -> Boolean): Boolean =
            foldRight(true, { element, exist -> predicate(element) && exist })

    fun any(predicate: (T) -> Boolean): Boolean =
            foldRight(false, { element, exist -> predicate(element) || exist })

    fun <R> foldLeft(identity: R, f: (T, R) -> R): R {
        tailrec fun flAcc(acc: R): R =
                when (this) {
                    Nil -> acc
                    is Cons -> {
                        val result = f(this.head, acc)
                        flAcc(result)
                    }
                }

        return flAcc(identity)
    }

    fun drop(n: Int): List<T> =
            when (n) {
                0 -> this
                else -> if (this == Nil) this else this.tail().drop(n - 1)
            }

    fun dropWhile(predicate: (T) -> Boolean): List<T> =
            when (this) {
                Nil -> this
                is Cons -> when (predicate(this.head)) {
                    true -> dropWhile(predicate)
                    else -> this
                }
            }

    fun filter(predicate: (T) -> Boolean): List<T> =
            foldRight(Nil as List<T>, { element, result ->
                if (predicate(element)) Cons(element, result)
                else result
            })

    val length = { foldRight(0, { _, count -> count + 1 }) }

    val reverse = { foldLeft<List<T>>(Nil, { x, y -> Cons(x, y) }) }
}

object Nil : List<Nothing>()
class Cons<out T>(val head: T, val tail: List<T>) : List<T>()

fun <T> apply(vararg list: T): List<T> {
    if (list.isEmpty()) return Nil
    return Cons(list[0], apply(*list.sliceArray(1 until list.size)))
}

fun <T> setHead(element: T, list: List<T>): List<T> =
        Cons(element, list.tail())


fun <T> init(list: List<T>): List<T> =
        when (list) {
            Nil -> Nil
            is Cons -> when (list.tail) {
                Nil -> Nil
                is Cons -> Cons(list.head, init(list.tail))
            }
        }

fun <T> append(list1: List<T>, list2: List<T>): List<T> =
        list1.foldRight(list2, { element, left -> Cons(element, left) })

fun <T> concat(list: List<List<T>>): List<T> {
    return list.foldRight(Nil as List<T>, { element, result ->
        append(element, result)
    })
}

fun <T> fill(n: Int, element: T): List<T> {
    tailrec fun fillAcc(n: Int, acc: List<T>): List<T> =
            if (n == 0) acc
            else fillAcc(n - 1, Cons(element, acc))

    return fillAcc(n, Nil)
}

fun transformInt(list: List<Int>): List<Int> =
        list.foldRight(Nil as List<Int>, { element, result -> Cons(element + 1, result) })

fun doubleList(list: List<Double>): List<String> =
        list.foldRight(Nil as List<String>, { element, result -> Cons(element.toString(), result) })


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
/*    val list1 = apply(1, 2, 3, 99, 100)
    val list2 = apply(5, 6, 7)
    val list3 = apply(8, 9, 10)
    val l = apply(list1, list2, list3)
    println(l)
    println(filter(map(list1, { x -> x * 10 }), { it < 30 }))
    println(pairwise(list1, list2))
    println(zipWith(list1, list2, { x, y -> x * y }))*/
    val list = apply(1, 2, 3)
    println(list)
}