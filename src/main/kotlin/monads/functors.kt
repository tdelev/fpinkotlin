package monads

import higherkind.Kind
import monoids.ForList
import monoids.ListK
import monoids.fix


val listFunctor = object : Functor<ForList> {
    override fun <A, B> map(a: Kind<ForList, A>, f: (A) -> B): Kind<ForList, B> {
        return ListK(a.fix().list.map(f))
    }
}