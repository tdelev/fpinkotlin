package monads

import errorhandling.Either
import errorhandling.Left
import errorhandling.Right
import higherkind.Kind

interface Functor<F> {
    fun <A, B> map(a: Kind<F, A>, f: (A) -> B): Kind<F, B>

    fun <A, B> distribute(ab: Kind<F, Pair<A, B>>): Pair<Kind<F, A>, Kind<F, B>> =
            Pair(map(ab, { it.first }), map(ab, { it.second }))

    fun <A, B> codistribute(e: Either<Kind<F, A>, Kind<F, B>>): Kind<F, Either<A, B>> =
            when (e) {
                is Left -> map(e.value, { Left(it) })
                is Right -> map(e.value, { Right(it) })
            }
}