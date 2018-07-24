package iomonad

import higherkind.Kind
import monads.Functor

interface Monad<F> : Functor<F> {

    fun <A> unit(a: () -> A): Kind<F, A>

    fun <A, B> flatMap(a: Kind<F, A>, f: (A) -> Kind<F, B>): Kind<F, B>

    fun <A, B, C> map2(fa: Kind<F, A>, fb: Kind<F, B>, f: (A, B) -> C): Kind<F, C> =
            flatMap(fa) { a -> map(fb) { f(a, it) } }

    override fun <A, B> map(fa: Kind<F, A>, f: (A) -> B): Kind<F, B> =
            flatMap(fa) { unit { f(it) } }


}

open class IO<out A>(val run: () -> A) {

    fun <B> map(f: (A) -> B): IO<B> = IO { f(run()) }
}

class ForIo private constructor()
data class Io<out A>(val io: IO<A>) : Kind<ForIo, A>

fun <A> Kind<ForIo, A>.fix() = this as Io<A>

object IoMonad : Monad<ForIo> {
    override fun <A> unit(a: () -> A): Kind<ForIo, A> = Io(IO(a))

    override fun <A, B> flatMap(a: Kind<ForIo, A>, f: (A) -> Kind<ForIo, B>): Kind<ForIo, B> =
            Io(IO(f(a.fix().io.run()).fix().io.run))

    operator fun <A> invoke(a: () -> A): Kind<ForIo, A> = unit(a)
}

fun readLine() = IoMonad { kotlin.io.readLine() ?: "" }
fun printLine(message: String) = IoMonad { println(message) }

fun fahrenheitToCelsius(f: Double): Double = (f - 32) * 5.0 / 9.0

fun converter(): Io<Unit> = IoMonad.flatMap(printLine("Enter temperature in degrees Fahrenheit: ")) {
    IoMonad.flatMap(IoMonad.map(readLine()) { it.toDouble() }) {
        printLine(fahrenheitToCelsius(it).toString())
    }
}.fix()

val echo = IoMonad.flatMap(readLine()) { printLine(it) }.fix()

fun main(args: Array<String>) {
    converter().io.run()
    echo.io.run()
}