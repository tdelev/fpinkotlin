package datastructures

import errorhandling.None
import errorhandling.Option
import errorhandling.Some

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
        tailrec fun flAcc(acc: R, list: List<T>): R =
                when (list) {
                    Nil -> acc
                    is Cons -> {
                        val result = f(list.head, acc)
                        flAcc(result, list.tail)
                    }
                }

        return flAcc(identity, this)
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

    fun head() = when (this) {
        is Nil -> None
        is Cons -> Some(this.head)
    }
}

object Nil : List<Nothing>()
class Cons<out T>(val head: T, val tail: List<T>) : List<T>()

fun <T> empty() = Nil as List<T>

fun <T> List<T>.equal(list: List<T>): Boolean {
    return when (this) {
        Nil -> when (list) {
            Nil -> true
            else -> false
        }
        is Cons -> when (list) {
            Nil -> false
            is Cons -> if (this.head == list.head) this.tail.equal(list.tail)
            else false
        }
    }
}

fun <T> list(vararg list: T): List<T> {
    if (list.isEmpty()) return Nil
    return Cons(list[0], list(*list.sliceArray(1 until list.size)))
}

fun <T> List<T>.setHead(element: T): List<T> = when (this) {
    is Cons -> Cons(element, this)
    is Nil -> Cons(element, Nil)
}
/*
fun <T> List<T>.toArray(): Array<T> {
    val length = this.length
    val result = Array<Any>()
}*/

fun <T> init(list: List<T>): List<T> =
        when (list) {
            Nil -> Nil
            is Cons -> when (list.tail) {
                Nil -> Nil
                is Cons -> Cons(list.head, init(list.tail))
            }
        }

fun <T> List<T>.append(list: List<T>): List<T> =
        this.foldRight(list, { element, left -> Cons(element, left) })

fun <T> concat(list: List<List<T>>): List<T> {
    return list.foldRight(Nil as List<T>, { element, result ->
        result.append(element)
    })
}

fun <T> List<T>.reduce(f: (T, T) -> T): Option<T> =
        when (this) {
            Nil -> None
            is Cons -> Some(this.tail.foldRight(this.head, f))
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

fun <T> List<T>.zipWith(list: List<T>, f: (T, T) -> T): List<T> = when (this) {
    Nil -> list
    is Cons -> when (list) {
        Nil -> this
        is Cons -> Cons(f(this.head, list.head), this.tail.zipWith(list.tail, f))
    }
}
