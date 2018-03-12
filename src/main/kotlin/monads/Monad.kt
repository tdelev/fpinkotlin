package monads

import higherkind.Kind

interface Monad<F> : Functor<F> {

    fun <A> unit(a: A): Kind<F, A>

    fun <A, B> flatMap(a: Kind<F, A>, f: (A) -> Kind<F, B>): Kind<F, B>

    fun <A, B, C> map2(fa: Kind<F, A>, fb: Kind<F, B>, f: (A, B) -> C): Kind<F, C> =
            flatMap(fa, { a -> map(fb, { f(a, it) }) })

    override fun <A, B> map(a: Kind<F, A>, f: (A) -> B): Kind<F, B> =
            flatMap(a, { unit(f(it)) })
}