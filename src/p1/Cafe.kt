package p1

class Cafe {
    fun buyCoffee(creditCard: CreditCard): Coffee {
        val cup = Coffee()
        creditCard.charge(cup.price)
        return cup
    }
}

class CreditCard {
    fun charge(price: Int) {

    }
}

class Coffee(var price: Int = 10) {

}


fun main(args: Array<String>) {
    println("Hello Kotlin")
}