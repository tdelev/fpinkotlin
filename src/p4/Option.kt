package p4

import p3.Cons
import p3.Nil
import p3.apply

sealed class Option<out T> {
    inline fun <B : Any> map(f: (T) -> B): Option<B> =
            when (this) {
                is Some -> Some(f(this.value))
                None -> None
            }

    fun <B> flatMap(f: (T) -> Option<B>): Option<B> =
            when (this) {
                None -> None
                is Some -> f(this.value)
            }

    inline fun <T> Option<T>.getOrElse(get: () -> T): T =
            when (this) {
                None -> get()
                is Some -> this.value
            }

    inline fun <T> Option<T>.orElse(other: T): T =
            when (this) {
                None -> other
                is Some -> this.value
            }

    fun filter(predicate: (T) -> Boolean): Option<T> =
            when (this) {
                None -> None
                is Some -> if (predicate(this.value)) this else None
            }

}

object None : Option<Nothing>()
data class Some<out T>(val value: T) : Option<T>()

fun variance(list: List<Double>): Option<Double> =
        mean(list).flatMap { m -> mean(list.map { Math.pow(it - m, 2.0) }) }

fun mean(list: List<Double>) =
        if (list.isEmpty()) None
        else Some(list.sum() / list.size)

fun <A, B, C> map2(a: Option<A>, b: Option<B>, f: (A, B) -> C): Option<C> =
        when (a) {
            is Some ->
                when (b) {
                    is Some -> Some(f(a.value, b.value))
                    else -> None
                }
            else -> None
        }

fun <T> sequence(list: p3.List<Option<T>>): Option<p3.List<T>> =
        when (list) {
            Nil -> Some(Nil)
            is Cons -> list.head.flatMap { h ->
                sequence(list.tail).map { Cons(h, it) }
            }
        }

fun main(args: Array<String>) {
    val list = apply(Some(1), Some(2), Some(3))
    sequence(list).map {
        println(it)
    }
}