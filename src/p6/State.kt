package p6

import p3.Cons
import p3.List
import p3.Nil

class State<S, out A>(val run: (S) -> Pair<A, S>) {

    fun <B> flatMap(f: (A) -> State<S, B>): State<S, B> = State({
        val next = run(it)
        f(next.first).run(next.second)
    })

    fun <B> map(f: (A) -> B): State<S, B> =
            flatMap({ StateI.unit<S, B>(f(it)) })

    fun <B, C> map2(sb: State<S, B>, f: (A, B) -> C): State<S, C> =
            flatMap { a -> sb.map { b -> f(a, b) } }


}

object StateI {
    fun <S, T> unit(element: T): State<S, T> = State({ Pair(element, it) })

    fun <S, T> sequence(states: List<State<S, T>>): State<S, List<T>> =
            states.foldRight(unit(Nil as List<T>), { state, stateList ->
                state.map2(stateList, { element, list -> Cons(element, list) })
            })
}

fun main(args: Array<String>) {

}