package monoids

import datastructures.List
import errorhandling.None
import errorhandling.Option
import errorhandling.orElse
import gettingstarted.MyModule.compose
import parallelism.Nonblocking
import parallelism.NonblockingPar
import parallelism.flatMap

val stringMonoid = object : Monoid<String> {
    override fun op(a: String, b: String) = "$a$b"

    override fun zero() = ""
}

fun <A> listMonoid() = object : Monoid<kotlin.collections.List<A>> {
    override fun op(a: kotlin.collections.List<A>, b: kotlin.collections.List<A>) = a + b

    override fun zero() = listOf<A>()
}

val intAddition = object : Monoid<Int> {
    override fun op(a: Int, b: Int) = a + b

    override fun zero() = 0
}

val intMultiplication = object : Monoid<Int> {
    override fun op(a: Int, b: Int) = a * b

    override fun zero() = 1
}

val booleanOr = object : Monoid<Boolean> {
    override fun op(a: Boolean, b: Boolean) = a || b

    override fun zero() = false
}

val booleanAnd = object : Monoid<Boolean> {
    override fun op(a: Boolean, b: Boolean) = a && b

    override fun zero() = true
}

fun <A> optionMonoid() = object : Monoid<Option<A>> {
    override fun op(a: Option<A>, b: Option<A>): Option<A> = a.orElse(b)

    override fun zero() = None
}

typealias EndoFunction<A> = (A) -> A

fun <A> endoFunMonoid() = object : Monoid<EndoFunction<A>> {
    override fun op(a: EndoFunction<A>, b: EndoFunction<A>) = compose(a, b)

    override fun zero() = { x: A -> x }
}

fun <А> concatenate(list: List<А>, monoid: Monoid<А>): А =
        list.foldLeft(monoid.zero(), monoid::op)

fun <A, B> foldMap(list: List<A>, monoid: Monoid<B>, f: (A) -> B): B =
        list.foldLeft(monoid.zero()) { a, b -> monoid.op(f(a), b) }

fun <A, B> foldMapV(arrayList: kotlin.collections.List<A>, monoid: Monoid<B>, f: (A) -> B): B {
    return when {
        arrayList.isEmpty() -> monoid.zero()
        arrayList.size == 1 -> f(arrayList[0])
        else -> {
            val mid = arrayList.size / 2
            val left = arrayList.subList(0, mid)
            val right = arrayList.subList(mid, arrayList.size)
            monoid.op(foldMapV(left, monoid, f), foldMapV(right, monoid, f))
        }
    }
}

fun <A> par(monoid: Monoid<A>) = object : Monoid<NonblockingPar<A>> {
    override fun op(a: NonblockingPar<A>, b: NonblockingPar<A>): NonblockingPar<A> =
            Nonblocking.map2(a, b, monoid::op)

    override fun zero() = Nonblocking.unit(monoid.zero())
}

fun <A, B> parFoldMap(list: kotlin.collections.List<A>, monoid: Monoid<B>, f: (A) -> B): NonblockingPar<B> =
        Nonblocking.parMap(list, f).flatMap { resultList ->
            foldMapV(resultList, par(monoid)) { Nonblocking.lazyUnit { it } }
        }


fun <A, B> productMonoid(ma: Monoid<A>, mb: Monoid<B>): Monoid<Pair<A, B>> = object : Monoid<Pair<A, B>> {
    override fun op(a: Pair<A, B>, b: Pair<A, B>): Pair<A, B> =
            Pair(ma.op(a.first, b.first), mb.op(a.second, b.second))

    override fun zero() = Pair(ma.zero(), mb.zero())
}

fun <K, V> mapMergeMonoid(monoid: Monoid<V>): Monoid<Map<K, V>> = object : Monoid<Map<K, V>> {
    override fun op(a: Map<K, V>, b: Map<K, V>): Map<K, V> {
        return (a.keys + b.keys).fold(mutableMapOf()) { acc, key ->
            val value = monoid.op(a.getOrDefault(key, monoid.zero()), b.getOrDefault(key, monoid.zero()))
            acc[key] = value
            acc
        }
    }

    override fun zero(): Map<K, V> = mapOf()
}

fun <A, B> functionMonoid(monoid: Monoid<B>): Monoid<(A) -> B> = object : Monoid<(A) -> B> {
    override fun op(a: (A) -> B, b: (A) -> B): (A) -> B = {
        monoid.op(a(it), b(it))
    }

    override fun zero(): (A) -> B = { monoid.zero() }
}


fun <A> bag(list: MutableList<A>): Map<A, Int> {
    val merge = mapMergeMonoid<A, Int>(intAddition)
    return foldMapV(list, merge) { mapOf(it to 1) }
}


fun main(args: Array<String>) {
    val sum = foldMapV(listOf(1, 5, 12, 10, 30), intAddition) { it }
    println(sum)

    val m1 = mapOf("a" to 10)
    val m2 = mapOf("b" to 20, "a" to 15)
    val m = mapMergeMonoid<String, Int>(intAddition)
    println(m.op(m1, m2))

    val list = mutableListOf(5, 15, 20, 10, 5, 20, 20, 10, 3)
    println(bag(list))

    val product = productMonoid(intAddition, intAddition)
    val sumAndCount = foldMapV(list, product) { Pair(it, 1) }
    println(sumAndCount)
}