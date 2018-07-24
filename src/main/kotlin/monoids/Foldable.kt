package monoids

import datastructures.Cons
import datastructures.List
import datastructures.Nil
import higherkind.Kind

interface Foldable<F> {

    fun <A, B> foldRight(collection: Kind<F, A>, identity: B, f: (A, B) -> B): B

    fun <A, B> foldLeft(collection: Kind<F, A>, identity: B, f: (B, A) -> B): B

    fun <A, B> foldMap(collection: Kind<F, A>, f: (A) -> B, monoid: Monoid<B>): B

    fun <A> concatenate(collection: Kind<F, A>, monoid: Monoid<A>): A =
            foldLeft(collection, monoid.zero(), monoid::op)

    fun <A> toList(collection: Kind<F, A>): List<A> =
            foldLeft(collection, Nil as List<A>) { list, element -> Cons(element, list) }

}