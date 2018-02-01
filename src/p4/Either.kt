package p4

import p3.Cons
import p3.Nil

sealed class Either<out E, out T> {
    fun <R> map(f: (T) -> R): Either<E, R> =
            when (this) {
                is Left -> Left(this.value)
                is Right -> Right(f(this.value))
            }


    override fun toString(): String =
            when (this) {
                is Left -> "Left($value)"
                is Right -> "Right($value)"
            }

}

fun <E, T, R> Either<E, T>.flatMap(f: (T) -> Either<E, R>): Either<E, R> =
        when (this) {
            is Left -> Left(value)
            is Right -> f(value)
        }

fun <E, R> Either<E, R>.orElse(get: () -> R): Either<E, R> =
        when (this) {
            is Left -> Right(get())
            is Right -> this
        }

fun <E, T1, T2, R> Either<E, T1>.map2(second: Either<E, T2>, f: (T1, T2) -> R): Either<E, R> =
        when (this) {
            is Left -> this
            is Right -> when (second) {
                is Left -> second
                is Right -> Right(f(this.value, second.value))
            }
        }

fun <E, T, R> traverse(list: p3.List<T>, f: (T) -> Either<E, R>): Either<E, p3.List<R>> =
        when (list) {
            Nil -> Right(Nil)
            is Cons -> f(list.head).map2(traverse(list.tail, f), { first, second ->
                Cons(first, second)
            })
        }

fun <E, T> sequence(list: p3.List<Either<E, T>>): Either<E, p3.List<T>> =
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