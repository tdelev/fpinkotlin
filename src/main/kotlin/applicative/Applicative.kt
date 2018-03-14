package applicative

import datastructures.Cons
import datastructures.List
import datastructures.Nil
import gettingstarted.curry
import higherkind.Kind
import monads.Functor

interface Applicative<F> : Functor<F> {

    fun <A, B> apply(fab: Kind<F, (A) -> B>, fa: Kind<F, A>): Kind<F, B> =
            map2(fab, fa, { f, a -> f(a) })

    fun <A> unit(a: () -> A): Kind<F, A>

    override fun <A, B> map(fa: Kind<F, A>, f: (A) -> B): Kind<F, B> =
            apply(unit { f }, fa)

    fun <A, B, C> map2(fa: Kind<F, A>, fb: Kind<F, B>, f: (A, B) -> C): Kind<F, C> =
            apply(map(fa, f.curry()), fb)

    fun <A, B> traverse(list: List<A>, f: (A) -> Kind<F, B>): Kind<F, List<B>> =
            list.foldRight(unit<List<B>> { Nil }, { element, listBK ->
                map2(f(element), listBK, { b, listB -> Cons(b, listB) })
            })

    fun <A> sequence(listF: List<Kind<F, A>>): Kind<F, List<A>> =
            listF.foldRight(unit<List<A>> { Nil }, { aK, listKA ->
                map2(aK, listKA, { a, listA -> Cons(a, listA) })
            })

    fun <A> replicateM(n: Int, fa: Kind<F, A>): Kind<F, List<A>> =
            if (n <= 0) unit<List<A>> { Nil }
            else map2(fa, replicateM(n - 1, fa), { a, list -> Cons(a, list) })

    fun <A, B> product(fa: Kind<F, A>, fb: Kind<F, B>): Kind<F, Pair<A, B>> =
            map2(fa, fb, { a, b -> Pair(a, b) })

    fun <A, B, C, D> map3(fa: Kind<F, A>, fb: Kind<F, B>, fc: Kind<F, C>, f: (A, B, C) -> D): Kind<F, D> =
            apply(apply(map(fa, f.curry()), fb), fc)

    fun <A, B, C, D, E> map4(fa: Kind<F, A>, fb: Kind<F, B>, fc: Kind<F, C>, fd: Kind<F, D>, f: (A, B, C, D) -> E): Kind<F, E> =
            apply(apply(apply(map(fa, f.curry()), fb), fc), fd)
}

interface Monad<F> : Applicative<F> {

    fun <A, B> flatMap(fa: Kind<F, A>, f: (A) -> Kind<F, B>): Kind<F, B> =
            join(map(fa, f))

    fun <A> join(ffa: Kind<F, Kind<F, A>>): Kind<F, A> =
            flatMap(ffa, { it })

    fun <A, B, C> compose(f: (A) -> Kind<F, B>, g: (B) -> Kind<F, C>): (A) -> Kind<F, C> = { a ->
        flatMap(f(a), g)
    }

    override fun <A, B> map(fa: Kind<F, A>, f: (A) -> B): Kind<F, B> =
            flatMap(fa, { unit { f(it) } })

    override fun <A, B, C> map2(fa: Kind<F, A>, fb: Kind<F, B>, f: (A, B) -> C): Kind<F, C> =
            flatMap(fa, { a -> map(fb, { f(a, it) }) })

}

interface Traverse<F> : Functor<F> {
    fun <A, B> traverse(fa: Kind<F, A>, f: (A) -> Kind<F, B>): Kind<F, Kind<F, B>> =
            sequence(map(fa, f))

    fun <A> sequence(fga: Kind<F, Kind<F, A>>): Kind<F, Kind<F, A>> =
            traverse(fga, { it })

    /*override fun <A, B> map(fa: Kind<F, A>, f: (A) -> B): Kind<F, B> =
            traverse(fa, { f(it) })*/
}