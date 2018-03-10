package testing

import datastructures.List
import state.Random
import state.State
import java.util.concurrent.Executors
import kotlin.math.absoluteValue

object Generator {
    fun <A> unit(a: A): Gen<A> =
            Gen(State.unit(a))

    fun choose(start: Int, stopExclusive: Int): Gen<Int> =
            Gen(State(Random::nonNegativeInt).map { start + it % (stopExclusive - start) })

    fun boolean(): Gen<Boolean> =
            Gen(State(Random.boolean))

    fun string(n: Int): Gen<String> =
            Gen(State({ Random.string(n, it) }))

    fun <A> union(g1: Gen<A>, g2: Gen<A>): Gen<A> =
            boolean().flatMap { if (it) g1 else g2 }

    fun <A> weighted(g1: Pair<Gen<A>, Double>, g2: Pair<Gen<A>, Double>): Gen<A> {
        val threshold = g1.second.absoluteValue / (g1.second.absoluteValue + g2.second.absoluteValue)
        return Gen(State(Random.double)).flatMap { if (it < threshold) g1.first else g2.first }
    }

    val S = weighted(Pair(choose(1, 4).map({ Executors.newFixedThreadPool(it) }), .75),
            Pair(unit(Executors.newCachedThreadPool()), .25))

    fun <A> listOf(gen: Gen<A>): SizedGen<List<A>> =
            SizedGen({ n -> gen.listOfN(n) })

    fun <A> listKOf(gen: Gen<A>): SizedGen<kotlin.collections.List<A>> =
            SizedGen({ n -> gen.listKOfN(n) })

    fun <A> listOf1(gen: Gen<A>): SizedGen<List<A>> =
            SizedGen({ n -> gen.listOfN(Math.max(n, 1)) })

}