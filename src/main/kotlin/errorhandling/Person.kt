package errorhandling

data class Name(val name: String)
data class Age(val age: Int)

data class Person(val name: Name, val age: Age)

fun makeName(name: String): Either<String, Name> =
        if (name.isEmpty()) Left("Name can not be empty")
        else Right(Name(name))

fun makeAge(age: Int): Either<String, Age> =
        if (age < 0) Left("Age can not be negative")
        else Right(Age(age))

fun makePerson(name: String, age: Int): Either<String, Person> =
        makeName(name).map2(makeAge(age)) { name: Name, age: Age -> Person(name, age) }
        /*makeName(name).flatMap { n -> makeAge(age).map { Person(n, it) } }*/

