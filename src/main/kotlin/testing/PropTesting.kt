package testing

import datastructures.reduce
import errorhandling.Some
import errorhandling.getOrElse
import laziness.Stream
import laziness.from
import laziness.unfold
import laziness.zipWith
import parallelism.Par
import parallelism.map
import parallelism.map2
import parallelism.unit
import state.RNG
import state.Random
import java.util.concurrent.Executors
import kotlin.math.min

typealias FailedCase = String
typealias SuccessCount = Int
typealias TestCases = Int
typealias MaxSize = Int

fun <A> forAll(gen: Gen<A>, predicate: (A) -> Boolean): Prop = Prop { max, n, rng ->
    randomStream(gen, rng)
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
            .getOrElse { Passed }
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
    }.toList().reduce({ a, b ->
        b.and(a)
    }).getOrElse { Prop { _, _, _ -> Passed } }
    prop.run(max, n, rng)
}

fun <A> forAll(sgen: SizedGen<A>, predicate: (A) -> Boolean): Prop =
        forAll(sgen.forSize, predicate)

fun <A> forAllPar(gen: Gen<A>, f: (A) -> Par<Boolean>): Prop =
        forAll(Generator.S.combine(gen)) { f(it.second)(it.first).get() }

fun check(predicate: () -> Boolean): Prop = Prop { _, _, _ ->
    if (predicate()) Proved else Failed("", 0)
}

fun checkPar(par: Par<Boolean>): Prop =
        forAllPar(Generator.unit(1)) { par }

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

val ANSI_GREEN = "\u001B[32m"
val ANSI_RED = "\u001B[31m"
val ANSI_RESET = "\u001B[0m"

fun success(message: String) = println("$ANSI_GREEN$message$ANSI_RESET")
fun error(message: String) = println("$ANSI_RED$message$ANSI_RESET")

fun run(prop: Prop, maxSize: Int = 100, testCases: Int = 100,
        rng: RNG = Random.SimpleRNG(System.currentTimeMillis())) {
    val result = prop.run(maxSize, testCases, rng)
    when (result) {
        Passed -> success("OK, passed $testCases tests.")
        is Failed -> error("Falsified after ${result.successes} passed tests:\n${result.failure}")
        Proved -> success("OK, proved property.")
    }
}

fun <A> equal(pa: Par<A>, pb: Par<A>): Par<Boolean> =
        map2(pa, pb) { a, b -> a == b }

fun main(args: Array<String>) {
    val generator = Random.SimpleRNG(System.currentTimeMillis())
    val smallInt = Generator.choose(-10, 10)
    val listR = Generator.listOf(smallInt)
    val maxProp = forAll(listR) {
        val max = it.foldRight(Int.MIN_VALUE, Math::max)
        println("Max: $max")
        println("List: $it")
        it.all { it <= max }
    }
    run(maxProp)
    //println(listR.forSize(5).sample.run(generator))
    /*val rndS = randomStream(smallInt, generator).zipWith(from(0), { a, b -> Pair(a, b) })
    println(rndS.take(10).find({ it.first > 9 }))*/
    val ES = Executors.newCachedThreadPool()
    val p1 = check {
        map(unit(1)) { it + 1 }(ES).get() == unit(2)(ES).get()
    }
    run(p1)

    val p2 = checkPar {
        equal(
                map(unit(1)) { it + 1 },
                unit(2)
        )(it)
    }

    val pint = Generator.choose(0, 10).map { unit(1) }
    val p4 = forAllPar(pint) {
        equal(map(it) { it }, it)
    }
    run(p4)
}