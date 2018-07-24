package monads

import datastructures.Cons
import datastructures.List
import datastructures.Nil
import higherkind.Kind

interface Monad<F> : Functor<F> {

    fun <A> unit(a: A): Kind<F, A>

    fun <A, B> flatMap(a: Kind<F, A>, f: (A) -> Kind<F, B>): Kind<F, B>

    fun <A, B, C> map2(fa: Kind<F, A>, fb: Kind<F, B>, f: (A, B) -> C): Kind<F, C> =
            flatMap(fa) { a -> map(fb) { f(a, it) } }

    override fun <A, B> map(fa: Kind<F, A>, f: (A) -> B): Kind<F, B> =
            flatMap(fa) { unit(f(it)) }

    fun <A> sequence(lma: List<Kind<F, A>>): Kind<F, List<A>> =
            lma.foldLeft(unit<List<A>>(Nil)) { element, list ->
                map2(element, list) { a, l -> Cons(a, l) }
            }

    fun <A, B> traverse(la: List<A>, f: (A) -> Kind<F, B>): Kind<F, List<B>> =
            la.foldLeft(unit<List<B>>(Nil)) { a, mb ->
                map2(f(a), mb) { b, list -> Cons(b, list) }
            }

    fun <A> replicateM(n: Int, monad: Kind<F, A>): List<Kind<F, A>> {
        tailrec fun repM(n: Int, acc: List<Kind<F, A>>): List<Kind<F, A>> {
            return if (n == 0) acc
            else repM(n - 1, Cons(monad, acc))
        }
        return repM(n, Nil)
    }

    fun <A, B> flatMapC(a: Kind<F, A>, f: (A) -> Kind<F, B>): Kind<F, B> =
            compose({ _: Unit -> a }, f)(Unit)

    fun <A, B, C> compose(f: (A) -> Kind<F, B>, g: (B) -> Kind<F, C>): (A) -> Kind<F, C> =
            { flatMap(f(it), g) }

    fun <A, B> product(ma: Kind<F, A>, mb: Kind<F, B>): Kind<F, Pair<A, B>> =
            map2(ma, mb) { a, b -> Pair(a, b) }

    fun <A> filterM(list: List<A>, f: (A) -> Kind<F, Boolean>): Kind<F, List<A>> =
            list.foldLeft<Kind<F, List<A>>>(unit(Nil)) { element, result ->
                //map2(f(a), result, { filter, list -> if (filter) list else Cons(a, list) })
                compose(f) {
                    if (it)
                        map2(unit(element), result) { left, right -> Cons(left, right) }
                    else result
                }(element)
            }

    fun <A> join(mma: Kind<F, Kind<F, A>>): Kind<F, A> =
            flatMap(mma) { it }

    fun <A, B> flatMapJ(a: Kind<F, A>, f: (A) -> Kind<F, B>): Kind<F, B> =
            join(map(a, f))

    fun <A, B, C> composeJ(f: (A) -> Kind<F, B>, g: (B) -> Kind<F, C>): (A) -> Kind<F, C> =
            { join(map(f(it), g)) }
}