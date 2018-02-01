package p3

import java.lang.Math.max

sealed class Tree<out T> {
    fun size(): Int =
            when (this) {
                is Leaf -> 1
                is Branch -> this.left.size() + this.right.size()
            }

    fun max(compare: (T, T) -> Boolean): T = when (this) {
        is Leaf -> this.value
        is Branch -> {
            val maxLeft = this.left.max(compare)
            val maxRight = this.right.max(compare)
            if (compare(maxLeft, maxRight)) maxLeft
            else maxRight
        }
    }

    fun depth(): Int =
            when (this) {
                is Leaf -> 0
                is Branch -> {
                    val leftDepth = this.left.depth() + 1
                    val rightDepth = this.right.depth() + 1
                    if (leftDepth > rightDepth) leftDepth
                    else rightDepth
                }
            }

    fun <R> map(f: (T) -> R): Tree<R> = when (this) {
        is Leaf -> Leaf(f(this.value))
        is Branch -> Branch(this.left.map(f), this.right.map(f))
    }

    fun <R> fold(f: (T) -> R, g: (R, R) -> R): R = when (this) {
        is Leaf -> f(this.value)
        is Branch -> {
            val left = left.fold(f, g)
            val right = right.fold(f, g)
            g(left, right)
        }
    }
}

class Leaf<out T>(val value: T) : Tree<T>()
class Branch<out T>(val left: Tree<T>, val right: Tree<T>) : Tree<T>()

fun main(args: Array<String>) {
    val tree = Branch(
            Branch(Leaf(1), Leaf(2)),
            Branch(Leaf(13),
                    Branch(Leaf(4),
                            Branch(Leaf(5),
                                    Branch(Leaf(6), Leaf(7))))
            )
    )
    println(tree.size())
    val sum = { x: Int, y: Int -> x + y }
    println(tree.fold({ x -> 1 + x }, sum))
    println(tree.max({ x, y -> x > y }))
    println(tree.fold({ it }, Math::max))
    println(tree.depth())
    println(tree.fold({ 0 }, { x, y -> Math.max(x, y) + 1 }))
    println(tree.map({ x -> x * 10 }).max({x, y -> x < y}))
    println(tree.fold<Tree<Int>>({ Leaf(it * 20) }, { left, right -> Branch(left, right) }).max({x, y -> x < y}))
}
