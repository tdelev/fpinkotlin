package monoids

import datastructures.List
import errorhandling.None
import errorhandling.Option
import errorhandling.orElse
import gettingstarted.MyModule.compose

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
        list.foldLeft(monoid.zero(), { a, b -> monoid.op(f(a), b) })