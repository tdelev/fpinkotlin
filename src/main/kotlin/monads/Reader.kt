package monads


data class Reader<in R, out A>(val run: (R) -> A)

