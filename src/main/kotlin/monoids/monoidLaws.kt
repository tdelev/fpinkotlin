package monoids

import errorhandling.Option
import errorhandling.Some
import testing.Gen
import testing.Generator
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

fun main(args: Array<String>) {
    val smallInt = Generator.choose(-10, 10)
    val listR = Generator.listKOf(smallInt)
    val prop = monoidLaws(listMonoid(), listR.forSize(5))
    testing.run(prop())

    val stringProp = monoidLaws(stringMonoid, Generator.string(10))
    testing.run(stringProp())

    val intAddProp = monoidLaws(intAddition, smallInt)
    testing.run(intAddProp())

    val intMulProp = monoidLaws(intMultiplication, smallInt)
    testing.run(intMulProp())

    val booleanAndProp = monoidLaws(booleanAnd, Generator.boolean())
    testing.run(booleanAndProp())

    val optionProp = monoidLaws(optionMonoid(), smallInt.map { Some(it) as Option<Int> })
    testing.run(optionProp())
}