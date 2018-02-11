package p6

import p3.Cons
import p3.List
import p3.Nil
import p6.Random.doubleM
import p6.Random.ints
import p6.Random.nonNegativeEven
import p6.Random.rollDie


typealias State<S, T> = (S) -> Pair<T, S>
typealias Rand<T> = State<RNG, T>

interface RNG {
    fun nextInt(): Pair<Int, RNG>
}

object Random {

    val int: Rand<Int> = { it.nextInt() }

    fun <T> unit(a: T): Rand<T> = { Pair(a, it) }

    fun <T, R> map(s: Rand<T>, f: (T) -> R): Rand<R> = {
        val r = s(it)
        Pair(f(r.first), r.second)
    }

    fun <S, T, R> mapG(s: (S) -> Pair<T, S>, f: (T) -> R): (S) -> Pair<R, S> = {
        val r = s(it)
        Pair(f(r.first), r.second)
    }

    fun <T, R> _map(s: Rand<T>, f: (T) -> R): Rand<R> =
            flatMap(s, { unit(f(it)) })

    fun <T1, T2, R> map2(r1: Rand<T1>, r2: Rand<T2>, f: (T1, T2) -> R): Rand<R> {
        return {
            val res1 = r1(it)
            val res2 = r2(res1.second)
            Pair(f(res1.first, res2.first), res2.second)
        }
    }

    fun <T1, T2, R> _map2(r1: Rand<T1>, r2: Rand<T2>, f: (T1, T2) -> R): Rand<R> =
            flatMap(r1, { a -> map(r2, { f(a, it) }) })

    fun <A, B> both(ra: Rand<A>, rb: Rand<B>): Rand<Pair<A, B>> =
            map2(ra, rb, { a, b -> Pair(a, b) })

    fun randIntDouble(): Rand<Pair<Int, Double>> = both(int, doubleM)
    fun randDoubleInt(): Rand<Pair<Double, Int>> = both(doubleM, int)

    fun <T> sequence(fs: List<Rand<T>>): Rand<List<T>> =
            fs.foldRight(unit(Nil as List<T>), { element, list -> map2(element, list, { a, b -> Cons(a, b) }) })

    fun <T, R> flatMap(f: Rand<T>, g: (T) -> Rand<R>): Rand<R> = {
        val next = f(it)
        g(next.first)(next.second)
    }

    fun nonNegativeIntLessThen(n: Int): Rand<Int> =
            flatMap(::nonNegativeInt, {
                val mod = it % n
                if (it + (n - 1) - mod >= 0) unit(mod)
                else nonNegativeIntLessThen(n)
            })

    fun nonNegativeEven(): Rand<Int> =
            map(::nonNegativeInt, { it - it % 2 })

    class SimpleRNG(val seed: Long) : RNG {
        override fun nextInt(): Pair<Int, RNG> {
            val newSeed = seed * 0x5DEECE66DL + 0xBL and 0xFFFFFFFFFFFFL
            val newRng = SimpleRNG(newSeed)
            val n = newSeed.ushr(16).toInt()
            return Pair(n, newRng)
        }
    }

    fun randomPair(rng: RNG): Pair<Pair<Int, Int>, RNG> {
        val r1 = rng.nextInt()
        val r2 = r1.second.nextInt()
        return Pair(Pair(r1.first, r2.first), r2.second)
    }

    fun nonNegativeInt(rng: RNG): Pair<Int, RNG> {
        val r = rng.nextInt()
        return if (r.first < 0) {
            if (r.first != Int.MIN_VALUE) {
                Pair(-r.first, r.second)
            } else Pair(Int.MAX_VALUE, r.second)
        } else {
            r
        }
    }

    fun double(rng: RNG): Pair<Double, RNG> {
        val nonNegative = nonNegativeInt(rng)
        return Pair(nonNegative.first / (Int.MAX_VALUE.toDouble() + 1), nonNegative.second)
    }

    val doubleM: Rand<Double> = map(::nonNegativeInt, { it / (Int.MAX_VALUE.toDouble() + 1) })

    fun intDouble(rng: RNG): Pair<Pair<Int, Double>, RNG> {
        val int = rng.nextInt()
        val dbl = double(int.second)
        return Pair(Pair(int.first, dbl.first), dbl.second)
    }

    fun doubleInt(rng: RNG): Pair<Pair<Double, Int>, RNG> {
        val dbl = double(rng)
        val int = dbl.second.nextInt()
        return Pair(Pair(dbl.first, int.first), int.second)
    }

    fun ints(count: Int, rng: RNG): Pair<List<Int>, RNG> {
        return if (count == 0) Pair(Nil, rng)
        else {
            val r = rng.nextInt()
            val next = ints(count - 1, r.second)
            Pair(Cons(r.first, next.first), next.second)
        }
    }

    fun rollDie(): Rand<Int> = map(nonNegativeIntLessThen(6), { it + 1 })

}

fun <S, T> unit(element: T): State<S, T> = { Pair(element, it) }

fun <S, T, R> flatMap(state: State<S, T>, f: (T) -> State<S, R>): State<S, R> = {
    val next = state(it)
    f(next.first)(it)
}

fun <S, T, R> map(state: State<S, T>, f: (T) -> R): State<S, R> =
        flatMap(state, { unit<S, R>(f(it)) })

fun <S, A, B, R> map2(sa: State<S, A>, sb: State<S, B>, f: (A, B) -> R): State<S, R> =
        flatMap(sa, { a -> map(sb, { f(a, it) }) })

fun <S, T> sequence(states: List<State<S, T>>): State<S, List<T>> =
        states.foldRight(unit(Nil as List<T>), { state, stateList ->
            map2(state, stateList, { element, list -> Cons(element, list) })
        })

fun main(args: Array<String>) {
    val rnd = Random.SimpleRNG(10006)
    println(rnd.nextInt())
    println(ints(5, rnd))
    println(nonNegativeEven()(rnd))
    println(doubleM(rnd))
    println(rollDie()(rnd))
}