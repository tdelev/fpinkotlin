package testing

import datastructures.List

class SizedGen<A>(val forSize: (Int) -> Gen<A>) {
    fun <B> map(f: (A) -> B): SizedGen<B> =
            SizedGen { forSize(it).map(f) }

    fun <B> flatMap(f: (A) -> Gen<B>): SizedGen<B> {
        return SizedGen { forSize(it).flatMap(f) }
    }

    fun listOfN(n: Int): SizedGen<List<A>> =
            SizedGen { forSize(it).listOfN(n) }
}