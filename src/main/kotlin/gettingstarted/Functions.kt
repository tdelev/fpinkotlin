package gettingstarted

fun <A, B, C> ((A, B) -> C).curry(): (A) -> ((B) -> C) =
        { a: A -> { b: B -> this(a, b) } }


object MyModule {

    fun abs(x: Int) = if (x < 0) -x else x

    fun factorial(x: Int): Int {
        tailrec fun go(x: Int, result: Int): Int {
            return if (x == 0) result
            else go(x - 1, x * result)
        }
        return go(x, 1)
    }

    fun fib(n: Int): Int {
        tailrec fun fibTail(n: Int, prev: Int, next: Int): Int {
            return if (n == 0) prev
            else fibTail(n - 1, next, prev + next)
        }
        return fibTail(n, 0, 1)
    }

    fun format(msg: String, x: Int, f: (Int) -> Int): String = msg.format(x, f(x))

    val formatAbsolute = { x: Int -> format("The abs value of %d is %d", x, MyModule::abs) }

    fun formatAbs(x: Int): String {
        /*val msg = "The absolute value of %d is %d"
        return msg.format(x, abs(x))*/
        return "The absolute value of $x is ${abs(x)}"
    }

    fun <T> find(a: Array<T>, equal: (T) -> Boolean): Int {
        fun loop(n: Int): Int {
            if (a.size == n) return -1
            return if (equal(a[n])) n
            else loop(n + 1)
        }
        return loop(0)
    }

    fun <T> isSorted(a: Array<T>, compare: (T, T) -> Boolean): Boolean {
        fun loop(n: Int): Boolean {
            if (a.size - 1 == n || a.size == 1) return true
            return if (!compare(a[n], a[n + 1])) false
            else loop(n + 1)
        }
        return loop(0)
    }

    fun isSortedAsc() =
            partial2<Array<Int>, (Int, Int) -> Boolean, Boolean>({ a: Int, b: Int -> a < b }, this::isSorted)

    fun <A, B, C> partial1(a: A, f: (A, B) -> C): (B) -> C = { f(a, it) }

    fun <A, B, C> partial2(b: B, f: (A, B) -> C): (A) -> C = { f(it, b) }

    fun <A, B, C> curry(f: (A, B) -> C): (A) -> ((B) -> C) =
            { a: A -> { b: B -> f(a, b) } }

    fun <A, B, C> uncurry(f: (A) -> ((B) -> C)): (A, B) -> C =
            { a, b -> f(a)(b) }

    fun <A, B, C> compose(f: (B) -> C, g: (A) -> B): (A) -> C =
            { a: A -> f(g(a)) }

    fun sum(a: Int, b: Int) = a + b

    fun mul(a: Int, b: Int) = a * b

    fun toStr(x: Int): String = "Str $x"

    fun toDouble(s: String) = s.length / 2.0

    val intToDouble = compose(this::toDouble, this::toStr)

    val sumX = curry(this::sum)

    val sumTwo = uncurry(sumX)
}

fun main(args: Array<String>) {
    //println(MyModule.find(arrayOf("a", "b", "c"), { it == "c" }))
    //println(MyModule.find(arrayOf(1, 2, 3, 4), { it == 12 }))
    // println(MyModule.isSorted(arrayOf(5, 3, 3), { a, b -> a >= b }))
    //println(MyModule.partial1(5, { x: Int, y: Int -> x + y })(10))
    //println(MyModule.isSortedAsc()(arrayOf(1, 2, 3)))
    println(MyModule.sumX(10)(12))
    println(MyModule.sumTwo(1, 2))
    println(MyModule.curry({ x: Int, y: Int -> x * y })(5)(12))
    println(MyModule.uncurry(MyModule.curry({ x: Int, y: Int -> x * y }))(5, 12))
    println(MyModule.compose(MyModule.sumX(10), MyModule.curry(MyModule::mul)(5))(6))
    println(MyModule.intToDouble(12))
    println(MyModule::toStr.also() { MyModule::toDouble }(12))
    println({ a: Int, b: Int -> a + b }.curry()(1)(12))
}