package laziness

import datastructures.List
import datastructures.Nil
import errorhandling.None
import errorhandling.Option
import errorhandling.Some

sealed class Stream<out T> {

    fun headOption(): Option<T> =
            when (this) {
                Empty -> None
                is Cons -> Some(this.head())
            }

    fun headOptionFR(): Option<T> =
            foldRight<Option<T>>({ None }, { left, _ -> Some(left) })

    fun toList(): List<T> =
            when (this) {
                Empty -> Nil
                is Cons -> datastructures.Cons(this.head(), this.tail().toList())
            }

    fun take(n: Int): Stream<T> =
            when (n) {
                0 -> Empty
                else -> when (this) {
                    Empty -> Empty
                    is Cons -> Cons(this.head, { this.tail().take(n - 1) })
                }
            }

    fun takeU(n: Int): Stream<T> =
            unfold(Pair(this, n)) {
                when (it.second) {
                    0 -> None
                    else -> {
                        val stream = it.first
                        when (stream) {
                            is Cons -> Some(Pair(stream.head(), Pair(stream.tail(), it.second - 1)))
                            else -> None
                        }
                    }
                }
            }

    fun drop(n: Int): Stream<T> =
            when (n) {
                0 -> when (this) {
                    Empty -> Empty
                    is Cons -> this
                }
                else -> when (this) {
                    Empty -> Empty
                    is Cons -> this.tail().drop(n - 1)
                }
            }

    fun takeWhile(predicate: (T) -> Boolean): Stream<T> =
            when (this) {
                Empty -> Empty
                is Cons -> {
                    if (predicate(this.head())) cons(this.head(), this.tail().takeWhile(predicate))
                    else Empty
                }
            }

    fun takeWhileU(predicate: (T) -> Boolean): Stream<T> =
            unfold(this) {
                when (it) {
                    is Cons -> if (predicate(it.head()))
                        Some(Pair(it.head(), it.tail()))
                    else None
                    else -> None
                }
            }

    fun takeWhileFR(predicate: (T) -> Boolean): Stream<T> =
            foldRight<Stream<T>>({ Empty }, { left, right ->
                if (predicate(left)) cons(left, right())
                else Empty
            })

    fun <R> foldRight(identity: () -> R, f: (T, () -> R) -> R): R =
            when (this) {
                is Cons -> f(this.head()) { this.tail().foldRight(identity, f) }
                else -> identity()
            }

    fun exists(predicate: (T) -> Boolean): Boolean =
            foldRight({ false }, { left, right -> predicate(left) || right() })

    fun find(predicate: (T) -> Boolean): Option<T> =
            foldRight<Option<T>>({ None },
                    { left, right -> if (predicate(left)) Some(left) else right() })

    fun forAll(predicate: (T) -> Boolean): Boolean =
            foldRight({ true }, { left, right -> predicate(left) && right() })

    fun <R> map(f: (T) -> R): Stream<R> =
            foldRight<Stream<R>>({ Empty }, { left, right -> cons(f(left), right()) })

    fun <R> mapU(f: (T) -> R): Stream<R> =
            unfold(this) { stream ->
                when (stream) {
                    is Cons -> Some(Pair(f(stream.head()), stream.tail()))
                    else -> None
                }
            }

    fun filter(predicate: (T) -> Boolean): Stream<T> =
            foldRight<Stream<T>>({ Empty }, { left, right ->
                if (!predicate(left)) cons(left, right())
                else right()
            })


    fun <R> flatMap(f: (T) -> Stream<R>): Stream<R> =
            foldRight<Stream<R>>({ Empty }, { left, right -> f(left).append(right) })

    fun <T> startsWith(other: Stream<T>): Boolean {
        return zipAll(other) { a, b -> Pair(a, b) }
                .takeWhile {
                    it.second != None
                }
                .forAll {
                    it.first == it.second
                }
    }

    fun tails(): Stream<Stream<T>> =
            unfold(this) {
                when (it) {
                    Empty -> None
                    is Cons -> Some(Pair(it, it.drop(1)))
                }
            }

    fun <T> hasSubsequence(stream: Stream<T>): Boolean =
            this.tails().exists { it.startsWith(stream) }
}

object Empty : Stream<Nothing>()
class Cons<out T>(val head: () -> T, val tail: () -> Stream<T>) : Stream<T>()

fun <T> cons(head: T, tail: Stream<T>): Stream<T> {
    val lh: T by lazy { head }
    val lt: Stream<T> by lazy { tail }
    return Cons({ lh }, { lt })
}


fun empty() = Empty

fun <T> apply(vararg args: T): Stream<T> =
        if (args.isEmpty()) Empty
        else cons(args[0], apply(*args.sliceArray(1 until args.size)))

val ones: Cons<Int> by lazy { Cons({ 1 }, { ones }) }

fun <T> constant(element: T): Stream<T> =
        Cons({ element }, { constant(element) })

fun from(start: Int): Stream<Int> =
        Cons({ start }, { from(start + 1) })

fun fibs(): Stream<Int> {
    fun fibs(first: Int, second: Int): Stream<Int> =
            Cons({ first }, { fibs(second, first + second) })

    return fibs(0, 1)
}

fun <A, S> unfold(initial: S, f: (S) -> Option<Pair<A, S>>): Stream<A> {
    val option = f(initial)
    return when (option) {
        is Some -> Cons({ option.value.first }, { unfold(option.value.second, f) })
        else -> Empty
    }
}

fun constantU(value: Int) = unfold(value) { Some(Pair(it, it)) }

fun fromU(start: Int) = unfold(start) { Some(Pair(it + 1, it + 1)) }

fun fibsU() = unfold(Pair(0, 1)) { state ->
    Some(Pair(Pair(state.second, state.first + state.second),
            Pair(state.second, state.first + state.second)))
}

fun <T> Stream<T>.append(element: () -> Stream<T>): Stream<T> =
        foldRight(element) { left, right -> cons(left, right()) }

fun <A, B, R> Stream<A>.zipWith(stream: Stream<B>, f: (A, B) -> R): Stream<R> =
        unfold(Pair(this, stream)) {
            val first = it.first
            val second = it.second

            when (first) {
                is Cons -> {
                    when (second) {
                        is Cons -> Some(Pair(f(first.head(), second.head()), Pair(first.tail(), second.tail())))
                        else -> None
                    }
                }
                else -> None
            }
        }

fun <T1, T2, R> Stream<T1>.zipAll(stream: Stream<T2>, f: (T1, T2) -> R): Stream<R> =
        unfold(Pair(this, stream)) {
            val first = it.first
            val second = it.second

            when (first) {
                is Cons -> {
                    when (second) {
                        is Cons -> Some(Pair(f(first.head(), second.head()), Pair(first.tail(), second.tail())))
                        else -> Some(Pair(Pair(first.head(), None), Pair(first.tail(), Empty)))
                    }
                }
                else -> when (second) {
                    is Cons -> Some(Pair(Pair(None, second.head()), Pair(Empty, second.tail())))
                    else -> None
                }
            }
        } as Stream<R>


fun main(args: Array<String>) {
    val stream = apply(1, 2, 3, 4, 5)
    /*println(stream.map({ it * 12 }).toList())
    println(stream.takeWhileFR({ it < 4 }).toList())
    println(stream.exists({ it == 12 }))
    println(stream.filter({ it % 2 == 0 }).toList())
    println(stream.map { it + 10 }.filter { it % 2 == 0 }.toList())
    println(ones.take(10).toList())
    val twos = constant(2)
    println(twos.take(3).toList())
    println(fromU(5).take(10).toList())
    println(fibs().take(10).toList())
    println(constantU(10).mapU({ it / 5 }).take(20).toList())
    println(fibsU().take(15).toList())*/
/*    println(stream.mapU({ it * 5 }).toList())
    println(stream.takeU(2).toList())
    println(stream.zipWith(constantU(5), { a, b -> a + b }).toList())
    println(stream.zipAll(constantU(5).take(3), { a, b -> a + b }).toList())
    println(from(1).zipAll(constant("A"), { num, letter -> "$letter$num" }).take(10).toList())
    val stream2 = apply(1, 2, 3, 4, 5, 6, 10)
    println(stream.startsWith(stream2))
    println(stream.tails().toList().map { it.toList() })
    val subseq = apply(3, 4, 5)
    println(stream2.hasSubsequence(subseq))
    println(stream2.exists { it == 2 })*/
}
