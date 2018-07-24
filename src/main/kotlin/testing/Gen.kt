package testing

import datastructures.List
import datastructures.fill
import state.RNG
import state.State

class Gen<A>(val sample: State<RNG, A>) {

    fun <B> map(f: (A) -> B): Gen<B> =
            flatMap { Generator.unit(f(it)) }
            //Gen(sample.map(f))

    fun <B, C> map2(gen: Gen<B>, f: (A, B) -> C): Gen<C> =
            Gen(sample.map2(gen.sample, f))

    fun <B> flatMap(f: (A) -> Gen<B>): Gen<B> {
        return Gen(sample.flatMap { f(it).sample })
    }

    fun <B> combine(gen: Gen<B>): Gen<Pair<A, B>> =
            Gen(this.sample.map2(gen.sample, { a, b -> Pair(a, b) }))

    fun listOfN(n: Int): Gen<List<A>> =
            Gen(State.sequence(fill(n, sample)))

    fun listKOfN(n: Int): Gen<kotlin.collections.List<A>> =
            Gen(State.sequenceL(kotlin.collections.List(n) { sample }))

    fun unsized(): SizedGen<A> = SizedGen({ this })

    fun listOf(): SizedGen<List<A>> = Generator.listOf(this)

    fun listOf1(): SizedGen<List<A>> = Generator.listOf1(this)

}