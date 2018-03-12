package monads

import higherkind.Kind
import testing.Gen
import testing.Generator

class ForGen private constructor()
data class GenK<A>(val gen: Gen<A>) : Kind<ForGen, A>

fun <A> Kind<ForGen, A>.fix() = this as GenK<A>

val genMonad = object : Monad<ForGen> {
    override fun <A> unit(a: A): Kind<ForGen, A> = GenK(Generator.unit(a))

    override fun <A, B> flatMap(a: Kind<ForGen, A>, f: (A) -> Kind<ForGen, B>): Kind<ForGen, B> =
            GenK(a.fix().gen.flatMap { f(it).fix().gen })
}