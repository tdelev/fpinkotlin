package testing

val sumOfEqualNElements = forAll(Generator.listOf(Generator.unit(5)), {
    val sum = it.foldRight(0, { a, b -> a + b })
    sum == it.length() * 5
})

val filterWithTrue = forAll(Generator.listOf(Generator.choose(-10, 10)), {
    val filtered = it.filter { true }
    filtered.length() == it.length()
})

val filterWithFalse = forAll(Generator.listOf(Generator.choose(-10, 10)), {
    val filtered = it.filter { false }
    filtered.length() == 0
})

val dropNElements = forAll(Generator.listOf(Generator.choose(-10, 10)), {
    val original = it.length()
    val left = it.drop(10)
    if (original >= 10) {
        original == left.length() + 10
    } else true
})

fun main(args: Array<String>) {
    run(sumOfEqualNElements)
    run(filterWithTrue)
    run(filterWithFalse)
    run(dropNElements)
}