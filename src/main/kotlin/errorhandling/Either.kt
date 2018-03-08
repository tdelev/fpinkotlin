package errorhandling

import datastructures.Cons
import datastructures.List
import datastructures.Nil

sealed class Either<out E, out A> {
    fun <R> map(f: (A) -> R): Either<E, R> =
            flatMap { Right(f(it)) }

    override fun toString(): String =
            when (this) {
                is Left -> "Left($value)"
                is Right -> "Right($value)"
            }

    fun fold(left: (E) -> Unit, right: (A) -> Unit) =
            when (this) {
                is Left -> left(value)
                is Right -> right(value)
            }
}

fun <E, A, B> Either<E, A>.flatMap(f: (A) -> Either<E, B>): Either<E, B> =
        when (this) {
            is Left -> Left(value)
            is Right -> f(value)
        }

fun <E, B> Either<E, B>.orElse(get: () -> B): Either<E, B> =
        when (this) {
            is Left -> Right(get())
            is Right -> this
        }

fun <E, A, B, C> Either<E, A>.map2(second: Either<E, B>, f: (A, B) -> C): Either<E, C> =
        this.flatMap { a -> second.map { b -> f(a, b) } }

fun <E, T, R> traverse(list: List<T>, f: (T) -> Either<E, R>): Either<E, List<R>> =
        when (list) {
            Nil -> Right(Nil)
            is Cons -> f(list.head).map2(traverse(list.tail, f), { first, second ->
                Cons(first, second)
            })
        }

fun <E, T> sequence(list: List<Either<E, T>>): Either<E, List<T>> =
        traverse(list, { it })

class Left<out E>(val value: E) : Either<E, Nothing>()
class Right<out T>(val value: T) : Either<Nothing, T>()

fun <T> Try(f: () -> T): Either<Exception, T> =
        try {
            Right(f())
        } catch (e: Exception) {
            Left(e)
        }

fun main(args: Array<String>) {
    val test = Right(10)
    println(test.map { it / 2 })
}