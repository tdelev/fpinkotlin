package monads

import datastructures.Nil
import datastructures.list
import errorhandling.Option
import errorhandling.Some
import higherkind.Kind
import laziness.Empty
import laziness.Stream
import monoids.ForList
import monoids.ListK
import monoids.fix
import parallelism.Nonblocking
import parallelism.NonblockingPar
import parsing.ForParser
import parsing.Reference
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

class ForPar private constructor()
data class ParK<A>(val par: NonblockingPar<A>) : Kind<ForPar, A>

fun <A> Kind<ForPar, A>.fix() = this as ParK<A>

val parMonad = object : Monad<ForPar> {
    override fun <A> unit(a: A) = ParK(Nonblocking.unit(a))

    override fun <A, B> flatMap(a: Kind<ForPar, A>, f: (A) -> Kind<ForPar, B>): Kind<ForPar, B> =
            ParK(Nonblocking.flatMap(a.fix().par) { f(it).fix().par })
}

val parserMonad = object : Monad<ForParser> {
    override fun <A> unit(a: A): Kind<ForParser, A> = Reference.succeed(a)

    override fun <A, B> flatMap(a: Kind<ForParser, A>, f: (A) -> Kind<ForParser, B>): Kind<ForParser, B> =
            Reference.flatMap(a, f)
}

class ForOption private constructor()
data class OptionK<out A>(val option: Option<A>) : Kind<ForOption, A>

fun <A> Kind<ForOption, A>.fix() = this as OptionK<A>

val optionMonad = object : Monad<ForOption> {
    override fun <A> unit(a: A): Kind<ForOption, A> = OptionK(Some(a))

    override fun <A, B> flatMap(a: Kind<ForOption, A>, f: (A) -> Kind<ForOption, B>): Kind<ForOption, B> =
            OptionK(a.fix().option.flatMap { f(it).fix().option })
}

class ForStream private constructor()
data class StreamK<out A>(val stream: Stream<A>) : Kind<ForStream, A>

fun <A> Kind<ForStream, A>.fix() = this as StreamK<A>

val streamMonad = object : Monad<ForStream> {
    override fun <A> unit(a: A): Kind<ForStream, A> = StreamK(Empty)

    override fun <A, B> flatMap(a: Kind<ForStream, A>, f: (A) -> Kind<ForStream, B>): Kind<ForStream, B> =
            StreamK(a.fix().stream.flatMap { f(it).fix().stream })
}

val listMonad = object : Monad<ForList> {
    override fun <A> unit(a: A): Kind<ForList, A> = ListK(Nil)

    override fun <A, B> flatMap(a: Kind<ForList, A>, f: (A) -> Kind<ForList, B>): Kind<ForList, B> =
            ListK(a.fix().list.flatMap { f(it).fix().list })
}

class ForId private constructor()
data class Id<out A>(val a: A) : Kind<ForId, A>

fun <A> Kind<ForId, A>.fix() = this as Id<A>

val idMonad = object : Monad<ForId> {
    override fun <A> unit(a: A) = Id(a)

    override fun <A, B> flatMap(a: Kind<ForId, A>, f: (A) -> Kind<ForId, B>): Kind<ForId, B> =
            f(a.fix().a)
}

fun main(args: Array<String>) {
    val replicated = listMonad.replicateM(10, ListK(list(1, 2)))
    println(replicated)
}