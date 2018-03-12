package parallelism

import errorhandling.*
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReference

interface Callback<out A> {
    fun onComplete(a: (A) -> Unit)
}
typealias NonblockingPar<A> = (ExecutorService) -> Callback<A>

fun <A, B> NonblockingPar<A>.flatMap(f: (A) -> NonblockingPar<B>) =
        Nonblocking.flatMap(this, f)

object Nonblocking {
    fun <A> run(es: ExecutorService, par: NonblockingPar<A>): A {
        val ref = AtomicReference<A>()
        val latch = CountDownLatch(1)
        par(es).onComplete {
            ref.set(it)
            latch.countDown()
        }
        latch.await()
        return ref.get()
    }

    fun <A> unit(element: A): NonblockingPar<A> = {
        object : Callback<A> {
            override fun onComplete(a: (A) -> Unit) {
                a(element)
            }
        }
    }

    fun <A> lazyUnit(a: () -> A): NonblockingPar<A> =
            fork { unit(a()) }

    fun <A, B, C> map2(a: NonblockingPar<A>, b: NonblockingPar<B>, f: (A, B) -> C): NonblockingPar<C> = {

        object : Callback<C> {
            override fun onComplete(a: (C) -> Unit) {
                var ar: Option<A> = None
                var br: Option<B> = None
                val es = it
                val combiner = Actor<Either<A, B>>(Strategies.fromExecutorService(it), {
                    when (it) {
                        is Left -> {
                            val bb = br.getOrElse { null }
                            if (bb != null) eval(es, { a(f(it.value, bb)) })
                            else ar = Some(it.value)
                        }
                        is Right -> {
                            val aa = ar.getOrElse { null }
                            if (aa != null) eval(es, { a(f(aa, it.value)) })
                            else br = Some(it.value)
                        }
                    }
                })

                a(es).onComplete { combiner.apply(Left(it)) }
                b(es).onComplete { combiner.apply(Right(it)) }
            }
        }

    }

    fun <A, B> map(par: NonblockingPar<A>, f: (A) -> B): NonblockingPar<B> = {
        object : Callback<B> {
            override fun onComplete(a: (B) -> Unit) {
                val es = it
                par(es).onComplete { eval(es, { a(f(it)) }) }
            }
        }
    }

    fun <A> sequenceBalanced(list: List<NonblockingPar<A>>): NonblockingPar<List<A>> = fork({
        when {
            list.isEmpty() -> unit(arrayListOf())
            list.size == 1 -> map(list[0], { arrayListOf(it) })
            else -> {
                val left = list.subList(0, list.size / 2)
                val right = list.subList(list.size / 2, list.size)
                map2(sequenceBalanced(left), sequenceBalanced(right), { a, b -> a + b })
            }
        }
    })

    fun <A> sequence(list: List<NonblockingPar<A>>): NonblockingPar<List<A>> =
            map(sequenceBalanced(list), { it })

    fun <A, B> parMap(list: List<A>, f: (A) -> B): NonblockingPar<List<B>> =
            sequence(list.map(asyncF { a -> f(a) }))

    fun <A, B> flatMap(par: NonblockingPar<A>, f: (A) -> NonblockingPar<B>): NonblockingPar<B> = {
        val es = it
        object : Callback<B> {
            override fun onComplete(a: (B) -> Unit) {
                par(es).onComplete { f(it)(es).onComplete(a) }
            }
        }
    }

    fun <A> delay(element: () -> A): NonblockingPar<A> = {
        object : Callback<A> {
            override fun onComplete(a: (A) -> Unit) {
                a(element())
            }
        }
    }

    fun <A> fork(par: () -> NonblockingPar<A>): NonblockingPar<A> = {
        object : Callback<A> {
            override fun onComplete(a: (A) -> Unit) {
                eval(it, { par()(it).onComplete(a) })
            }
        }
    }

    fun <A> async(f: ((A) -> Unit) -> Unit): NonblockingPar<A> = {
        object : Callback<A> {
            override fun onComplete(a: (A) -> Unit) {
                f(a)
            }
        }
    }

    fun <A, B> asyncF(f: (A) -> B): (A) -> NonblockingPar<B> = {
        lazyUnit { f(it) }
    }


    fun eval(es: ExecutorService, callback: () -> Unit) =
            es.submit(Callable { callback })
}