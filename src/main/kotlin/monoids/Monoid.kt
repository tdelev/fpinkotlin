package monoids

interface Monoid<A> {
    fun op(a: A, b: A): A

    fun zero(): A
}

