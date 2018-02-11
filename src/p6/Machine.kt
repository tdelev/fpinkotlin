package p6

import p3.List
import p3.apply
import p3.reverse

interface Input
object Coin : Input
object Turn : Input

data class Machine(var locked: Boolean, val candies: Int, val coins: Int)

object Candy {
    fun update(input: Input): (Machine) -> Machine = {
        when (input) {
            Coin -> {
                if (it.locked && it.candies > 0) {
                    Machine(false, it.candies, it.coins)
                } else it
            }
            Turn -> {
                if (!it.locked && it.candies > 0) {
                    Machine(true, it.candies - 1, it.coins + 1)
                } else {
                    it
                }
            }
            else -> it
        }
    }

    fun simulateMachine(inputs: List<Input>): State<Machine, Pair<Int, Int>> {
        return {
            reverse(inputs).foldRight(Pair(Pair(it.candies, it.coins), it), { input, acc ->
                val updated = update(input)(acc.second)
                Pair(Pair(updated.candies, updated.coins), updated)
            })
        }
    }
}

fun main(args: Array<String>) {
    val state = Candy.simulateMachine(apply(Coin, Turn, Coin, Turn))
    val machine = state(Machine(true, 5, 0))
    println(machine)
}