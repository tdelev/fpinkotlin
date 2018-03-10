package testing

import state.RNG

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