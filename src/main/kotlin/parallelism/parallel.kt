package parallelism

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

typealias Par<A> = (ExecutorService) -> Future<A>

class UnitFuture<A>(private val a: A) : Future<A> {
    override fun isDone() = true

    override fun get() = a

    override fun get(timeout: Long, unit: TimeUnit?) = get()

    override fun cancel(mayInterruptIfRunning: Boolean) = false

    override fun isCancelled() = false
}

fun <A> unit(element: A): Par<A> = { UnitFuture(element) }

fun <A, B, C> map2(a: Par<A>, b: Par<B>, f: (A, B) -> C): Par<C> = {
    val af = a(it)
    val bf = b(it)
    UnitFuture(f(af.get(), bf.get()))
}

fun <A> fork(a: () -> Par<A>): Par<A> = { es ->
    es.submit(Callable<A> { a()(es).get() })
}

fun <A> lazyUnit(a: () -> A): Par<A> = fork({ unit(a()) })

fun <A> runBlocking(es: ExecutorService, a: Par<A>): Future<A> = a(es)

fun <A, B> asyncF(f: (A) -> B): (A) -> Par<B> =
        { lazyUnit({ f(it) }) }

fun sortPar(parList: Par<List<Int>>): Par<List<Int>> =
        map2(parList, unit({}), { l1, _ -> l1.sorted() })

fun <A, B> map(par: Par<A>, f: (A) -> B): Par<B> =
        map2(par, unit({}), { a, _ -> f(a) })

fun <A> sequence(list: List<Par<A>>): Par<List<A>> =
        list.foldRight(unit(listOf()), { element, acc -> map2(element, acc, { el, l -> l + el }) })

fun <A, B> parMap(list: List<A>, f: (A) -> B): Par<List<B>> {
    val parList = list.map(asyncF(f))
    return sequence(parList)
}

fun <A> parFilter(list: List<A>, f: (A) -> Boolean): Par<List<A>> {
    val lifted = list.map(asyncF<A, List<A>>({ if (f(it)) listOf(it) else listOf() }))
    return map(sequence(lifted), { it.flatten() })
}

fun main(args: Array<String>) {
    println("Testing...")
}