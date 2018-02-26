package p8

import p3.List
import p3.fill
import p4.Some
import p5.Stream
import p5.from
import p5.unfold
import p5.zipWith
import p6.RNG
import p6.Random
import p6.State
import p6.StateI
import p7.Par
import java.util.concurrent.Executors
import kotlin.math.absoluteValue
import kotlin.math.min

typealias FailedCase = String
typealias SuccessCount = Int
typealias TestCases = Int
typealias MaxSize = Int

fun <A> forAll(gen: Gen<A>, predicate: (A) -> Boolean): Prop = Prop { max, n, rng ->
    val res = randomStream(gen, rng)
            .zipWith(from(0), { a, b -> Pair(a, b) })
            .take(n)
            .map {
                val element = it.first
                val index = it.second
                try {
                    if (predicate(element)) Passed
                    else Failed(element.toString(), index)
                } catch (e: Exception) {
                    Failed(buildMessage(element, e), index)
                }
            }.find({ it.isFalsified })

    if (res is Some) res.value
    else Passed
}

fun <A> randomStream(gen: Gen<A>, rng: RNG): Stream<A> =
        unfold(rng, { Some(gen.sample.run(it)) })

fun <A> buildMessage(a: A, e: Exception): String =
        """Test case: $a
            |generated an exception: ${e.message}
            |stack trace:
            |${e.stackTrace.joinToString("\n")}
            |""".trimMargin()

fun <A> forAll(gen: (Int) -> Gen<A>, predicate: (A) -> Boolean): Prop = Prop { max, n, rng ->
    val casesPerSize = (n - 1) / max + 1
    val props = from(0)
            .take(min(n, max) + 1)
            .map {
                forAll(gen(it), predicate)
            }
    val prop = props.map {
        Prop { max, _, rng ->
            it.run(max, casesPerSize, rng)
        }
    }.toList().foldRight(Prop { _, _, _ -> Passed }, { a, b ->
        b.and(a)
    })
    prop.run(max, n, rng)
}

fun <A> forAll(sgen: SGen<A>, predicate: (A) -> Boolean): Prop =
        forAll(sgen.forSize, predicate)

fun check(predicate: () -> Boolean): Prop = Prop { _, _, _ ->
    if (predicate()) Proved else Failed("", 0)
}

class Prop(val run: (MaxSize, TestCases, RNG) -> Result) {


    fun and(prop: Prop): Prop = Prop { max, n, rng ->
        val result = run(max, n, rng)
        if (result == Passed) prop.run(max, n, rng)
        else result
    }

    fun or(prop: Prop): Prop = Prop { max, n, rng ->
        val result = run(max, n, rng)
        if (result == Passed) result
        else prop.run(max, n, rng)
    }


}

sealed class Result {
    abstract val isFalsified: Boolean
}

object Passed : Result() {
    override val isFalsified = false
}

object Proved : Result() {
    override val isFalsified = false
}

class Failed(val failure: FailedCase, val successes: SuccessCount) : Result() {
    override val isFalsified = true
}

class Gen<A>(val sample: State<RNG, A>) {

    fun <B> map(f: (A) -> B): Gen<B> =
            Gen(sample.map(f))

    fun <B> flatMap(f: (A) -> Gen<B>): Gen<B> {
        return Gen(sample.flatMap { f(it).sample })
    }

    fun listOfN(n: Int): Gen<List<A>> =
            Gen(StateI.sequence(fill(n, sample)))

    fun unsized(): SGen<A> = SGen({ this })

    fun listOf(): SGen<List<A>> = Generator.listOf(this)

    fun listOf1(): SGen<List<A>> = Generator.listOf1(this)

}

class SGen<A>(val forSize: (Int) -> Gen<A>) {
    fun <B> map(f: (A) -> B): SGen<B> =
            SGen({ forSize(it).map(f) })

    fun <B> flatMap(f: (A) -> Gen<B>): SGen<B> {
        return SGen({ forSize(it).flatMap(f) })
    }

    fun listOfN(n: Int): SGen<List<A>> =
            SGen({ forSize(it).listOfN(n) })
}

object Generator {
    fun <A> unit(a: A): Gen<A> =
            Gen(StateI.unit(a))

    fun choose(start: Int, stopExclusive: Int): Gen<Int> =
            Gen(State(Random::nonNegativeInt).map { start + it % (stopExclusive - start) })

    fun boolean(): Gen<Boolean> =
            Gen(State(Random.boolean))

    fun <A> union(g1: Gen<A>, g2: Gen<A>): Gen<A> =
            boolean().flatMap { if (it) g1 else g2 }

    fun <A> weighted(g1: Pair<Gen<A>, Double>, g2: Pair<Gen<A>, Double>): Gen<A> {
        val threshold = g1.second.absoluteValue / (g1.second.absoluteValue + g2.second.absoluteValue)
        return Gen(State(Random.double)).flatMap { if (it < threshold) g1.first else g2.first }
    }

    fun <A> listOf(gen: Gen<A>): SGen<List<A>> =
            SGen({ n -> gen.listOfN(n) })

    fun <A> listOf1(gen: Gen<A>): SGen<List<A>> =
            SGen({ n -> gen.listOfN(Math.max(n, 1)) })

}

fun run(prop: Prop, maxSize: Int = 100, testCases: Int = 100,
        rng: RNG = Random.SimpleRNG(System.currentTimeMillis())) {
    val result = prop.run(maxSize, testCases, rng)
    when (result) {
        Passed -> println("OK, passed $testCases tests.")
        is Failed -> println("Falsified after ${result.successes} passed tests:\n${result.failure}")
        Proved -> println("OK, proved property.")
    }
}

fun main(args: Array<String>) {
    val generator = Random.SimpleRNG(System.currentTimeMillis())
    val smallInt = Generator.choose(-10, 10)
    val listR = Generator.listOf(smallInt)
    val maxProp = forAll(listR, {
        val max = it.foldRight(Int.MIN_VALUE, Math::max)
        println("Max: $max")
        println("List: $it")
        it.all { it <= max }
    })
    run(maxProp)
    //println(listR.forSize(5).sample.run(generator))
    /*val rndS = randomStream(smallInt, generator).zipWith(from(0), { a, b -> Pair(a, b) })
    println(rndS.take(10).find({ it.first > 9 }))*/
    val ES = Executors.newCachedThreadPool()
    val p1 = check({
        Par.map(Par.unit(1), { it + 1 })(ES).get() == Par.unit(2)(ES).get()
    })
    run(p1)
}