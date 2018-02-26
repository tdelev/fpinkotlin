package p7

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

typealias ParType<A> = (ExecutorService) -> Future<A>

class UnitFuture<A>(val a: A) : Future<A> {
    override fun isDone() = true

    override fun get() = a

    override fun get(timeout: Long, unit: TimeUnit?) = get()

    override fun cancel(mayInterruptIfRunning: Boolean) = false

    override fun isCancelled() = false
}
object Par {
    fun <A> unit(element: A): ParType<A> = { UnitFuture(element) }

    fun <A, B, C> map2(a: ParType<A>, b: ParType<B>, f: (A, B) -> C): ParType<C> = {
        val af = a(it)
        val bf = b(it)
        UnitFuture(f(af.get(), bf.get()))
    }

    fun <A> fork(a: () -> ParType<A>): ParType<A> = { es ->
        es.submit(Callable<A> { a()(es).get() })
    }

    fun <A> lazyUnit(a: () -> A): ParType<A> = fork({ unit(a()) })

    fun <A> run(es: ExecutorService, a: ParType<A>): Future<A> = a(es)

    fun <A, B> asyncF(f: (A) -> B): (A) -> ParType<B> =
            { lazyUnit({ f(it) }) }

    fun sortPar(parList: ParType<List<Int>>): ParType<List<Int>> =
            map2(parList, unit({}), { l1, _ -> l1.sorted() })

    fun <A, B> map(par: ParType<A>, f: (A) -> B): ParType<B> =
            map2(par, unit({}), { a, _ -> f(a) })

    fun <A> sequence(list: List<ParType<A>>): ParType<List<A>> =
            list.foldRight(unit(listOf()), { element, acc -> map2(element, acc, { el, l -> l + el }) })

    fun <A, B> parMap(list: List<A>, f: (A) -> B): ParType<List<B>> {
        val parList = list.map(asyncF(f))
        return sequence(parList)
    }

    fun <A> parFilter(list: List<A>, f: (A) -> Boolean): ParType<List<A>> {
        val lifted = list.map(asyncF<A, List<A>>({ if (f(it)) listOf(it) else listOf() }))
        return map(sequence(lifted), { it.flatten() })
    }
}
fun main(args: Array<String>) {
    println("Testing...")
}