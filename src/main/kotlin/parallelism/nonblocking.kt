package parallelism

import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicReference

interface Callback<out A> {
    fun onComplete(a: (A) -> Unit)
}
typealias NonblockingPar<A> = (ExecutorService) -> Callback<A>

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

    fun eval(es: ExecutorService, callback: () -> Unit) =
            es.submit(Callable { callback })
}