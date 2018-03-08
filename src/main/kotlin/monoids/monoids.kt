package monoids

import errorhandling.None
import errorhandling.Option
import errorhandling.orElse

val stringMonoid = object : Monoid<String> {
    override fun op(a: String, b: String) = "$a$b"

    override fun zero() = ""
}

fun <T> listMonoid() = object : Monoid<List<T>> {
    override fun op(a: List<T>, b: List<T>) = a + b

    override fun zero() = listOf<T>()
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

fun <T> optionMonoid() = object : Monoid<Option<T>> {
    override fun op(a: Option<T>, b: Option<T>): Option<T> = a.orElse(b) as Option<T>

    override fun zero() = None
}

typealias EndoFunction<T> = (T) -> T

fun <T> endoFunMonoid() = object : Monoid<EndoFunction<T>> {
    override fun op(a: EndoFunction<T>, b: EndoFunction<T>) = { x: T -> a(b(x)) }

    override fun zero() = { x: T -> x }
}
