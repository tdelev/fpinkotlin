package parallelism

import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

class Actor<A>(private val strategy: Strategy, private val handler: (A) -> Unit,
               private val onError: (Throwable) -> Unit = { throw(it) }) {
    private val tail = AtomicReference(Node<A>())
    private val suspended = AtomicInteger(1)
    private val head = AtomicReference(tail.get())

    fun apply(a: A) {
        val n = Node(a)
        head.getAndSet(n).lazySet(n)
        trySchedule()
    }

    fun <B> contramap(f: (B) -> A): Actor<B> =
            Actor(strategy, { apply(f(it)) }, onError)

    private fun trySchedule() {
        if (suspended.compareAndSet(1, 0)) schedule()
    }

    private fun schedule() {
        strategy.apply { act() }
    }

    private fun act() {
        val t = tail.get()
        val n = batchHandle(t, 1024)
        if (n != t) {
            n.a = null
            tail.lazySet(n)
            schedule()
        } else {
            suspended.set(1)
            if (n.get() != null) trySchedule()
        }
    }

    private tailrec fun batchHandle(t: Node<A>, i: Int): Node<A> {
        val n = t.get()
        return if (n != null) {
            val a = n.a
            try {
                if (a != null) {
                    handler(a)
                } else {
                    onError(RuntimeException("null value"))
                }
            } catch (e: Exception) {
                onError(e)
            }
            if (i > 0) batchHandle(n, i - 1) else n
        } else t
    }
}

private class Node<A>(var a: A? = null) : AtomicReference<Node<A>>()

interface Strategy {
    fun <A> apply(a: () -> A): () -> A
}

object Strategies {
    /**
     * We can create a `Strategy` from any `ExecutorService`. It's a little more
     * convenient than submitting `Callable` objects directly.
     */
    fun fromExecutorService(es: ExecutorService): Strategy = object : Strategy {
        override fun <A> apply(a: () -> A): () -> A {
            val f = es.submit(Callable { a })
            return f.get()
        }
    }

    /**
     * A `Strategy` which begins executing its argument immediately in the calling thread.
     */
    fun sequential(): Strategy = object : Strategy {
        override fun <A> apply(a: () -> A) = a
    }
}