package state

import datastructures.Cons
import datastructures.List
import datastructures.Nil
import state.Random.char
import state.Random.double
import state.Random.ints
import state.Random.nonNegativeEven
import state.Random.rollDie
import state.Random.string


typealias StateType<S, T> = (S) -> Pair<T, S>
typealias Rand<T> = StateType<RNG, T>

interface RNG {
    fun nextInt(): Pair<Int, RNG>
}

object Random {

    val int: Rand<Int> = { it.nextInt() }

    val boolean: Rand<Boolean> = map(int, { it % 2 == 0 })

    val char: Rand<Char> = map2(nonNegativeIntLessThen(26), boolean, { i, upper ->
        if (upper) ('A' + i)
        else ('a' + i)
    })

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

    fun randIntDouble(): Rand<Pair<Int, Double>> = both(int, double)
    fun randDoubleInt(): Rand<Pair<Double, Int>> = both(double, int)

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

        override fun toString() = "SimpleRNG(seed=$seed)"
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

    val double: Rand<Double> = map(::nonNegativeInt, { it / (Int.MAX_VALUE.toDouble() + 1) })

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

    fun string(count: Int, rng: RNG): Pair<String, RNG> {
        return if (count == 0) Pair("", rng)
        else {
            val nextChar = char(rng)
            val next = string(count - 1, nextChar.second)
            Pair(nextChar.first.toString() + next.first, next.second)
        }
    }

    fun rollDie(): Rand<Int> = map(nonNegativeIntLessThen(6), { it + 1 })

}


fun main(args: Array<String>) {
    val rnd = Random.SimpleRNG(10006)
    println(rnd.nextInt())
    println(ints(5, rnd))
    println(nonNegativeEven()(rnd))
    println(double(rnd))
    println(rollDie()(rnd))
    println(char(rnd))
    println(string(10, rnd))
}