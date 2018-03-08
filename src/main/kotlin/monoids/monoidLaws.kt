package monoids

import testing.Gen
import testing.forAll

data class Triple<A>(val a1: A, val a2: A, val a3: A)

fun <A> monoidLaws(monoid: Monoid<A>, gen: Gen<A>) = {
    val gen3A = gen.flatMap { a -> gen.flatMap { b -> gen.map { Triple(a, b, it) } } }
    forAll(gen3A, {
        // Associativity
        monoid.op(monoid.op(it.a1, it.a2), it.a3) == monoid.op(it.a1, monoid.op(it.a2, it.a3))
    }).and(forAll(gen, {
        // Identity
        monoid.op(it, monoid.zero()) == it && monoid.op(monoid.zero(), it) == it
    }))
}