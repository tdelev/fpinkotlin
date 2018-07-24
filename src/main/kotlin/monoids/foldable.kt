package monoids

import datastructures.List
import higherkind.Kind

class ForList private constructor()
data class ListK<out A>(val list: List<A>) : Kind<ForList, A>

fun <A> Kind<ForList, A>.fix() = this as ListK<A>

object FoldableList : Foldable<ForList> {
    override fun <A, B> foldRight(collection: Kind<ForList, A>, identity: B, f: (A, B) -> B): B {
        val list = collection.fix()
        return list.list.foldRight(identity, f)
    }

    override fun <A, B> foldLeft(collection: Kind<ForList, A>, identity: B, f: (B, A) -> B): B {
        val list = collection.fix()
        return list.list.foldLeft(identity) { a, b -> f(b, a) }
    }

    override fun <A, B> foldMap(collection: Kind<ForList, A>, f: (A) -> B, monoid: Monoid<B>): B {
        val list = collection.fix().list
        return list.map(f).foldLeft(monoid.zero(), monoid::op)
    }
}