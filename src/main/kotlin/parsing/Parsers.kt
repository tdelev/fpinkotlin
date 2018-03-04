package parsing

import datastructures.List
import datastructures.empty
import datastructures.setHead
import errorhandling.Either
import testing.Gen
import testing.Prop
import testing.forAll

sealed class ParseState(val location: Location) {

}

sealed class Result<A> {

}

data class Success<A>(val result: A, val length: Int) : Result<A>() {

}

data class Failure(val error: ParseError, val isCommitted: Boolean) : Result<Nothing>()

interface Parser<A> {

    fun <B> map(f: (A) -> B): Parser<B> =
            this.flatMap { succeed(f(it)) }

    fun many(): Parser<List<A>> =
            this.map2({ this.many() }, { a, list -> list.setHead(a) }).or({ succeed(empty()) })

    fun char(c: Char): Parser<Char> =
            string(c.toString()).map({ it[0] })

    fun slice(): Parser<String>

    fun <B> product(p: () -> Parser<B>): Parser<Pair<A, B>> =
            this.flatMap({ a -> p().map { Pair(a, it) } })

    fun <B, C> map2(p: () -> Parser<B>, f: (A, B) -> C): Parser<C> =
            this.flatMap({ a -> p().map { f(a, it) } })

    fun <B> flatMap(f: (A) -> Parser<B>): Parser<B>

    fun <A> succeed(a: A): Parser<A> =
            string("").map({ _ -> a })

    fun <A> listOfN(n: Int, p: Parser<A>): Parser<List<A>> =
            if (n <= 0) succeed(empty())
            else p.map2({ listOfN(n - 1, p) }, { a, list -> list.setHead(a) })


    fun regex(r: Regex): Parser<String>

    fun string(s: String): Parser<String>

    fun <B> to(b: B): Parser<B> =
            this.slice().map { _ -> b }

    fun <A> asStringParser(a: A, f: (A) -> Parser<String>) = f(a)

    fun <B> skipLeft(p: () -> Parser<B>): Parser<B> =
            this.map2(p, { _, b -> b })

    fun <B> skipRight(p: () -> Parser<B>): Parser<A> =
            this.map2(p, { a, _ -> a })

    fun <B, C> surround(right: Parser<C>, p: () -> Parser<B>): Parser<B> =
            this.skipLeft(p).skipRight({ right })

    /** Parser which consumes zero or more whitespace characters. */
    fun whitespace(): Parser<String> = regex(Regex("\\s*"))

    /** Parser which consumes 1 or more digits. */
    fun digits(): Parser<String> = regex(Regex("\\d+"))

    /** Parser which consumes reluctantly until it encounters the given string. */
    fun thru(s: String): Parser<String> = regex(Regex(".*?$s"))

    /** Unescaped string literals, like "foo" or "bar". */
    fun quoted(): Parser<String> = string("\"").skipLeft({ thru("\"") }).map { it.dropLast(1) }

    /** Attempts `p` and strips trailing whitespace, usually used for the tokens of a grammar. */
    fun token(): Parser<A> =
            this.skipRight({ whitespace() })

    /** Unescaped or escaped string literals, like "An \n important \"Quotation\"" or "bar". */
    fun escapedQuoted(): Parser<String> =
            token().quoted()

    /** C/Java style floating point literals, e.g .1, -1.0, 1e9, 1E-23, etc.
     * Result is left as a string to keep full precision
     */
    fun doubleString(): Parser<String> =
            regex(Regex("[-+]?([0-9]*\\.)?[0-9]+([eE][-+]?[0-9]+)?")).token()

    /** Floating point literals, converted to a `Double`. */
    fun double(): Parser<Double> =
            doubleString().map { it.toDouble() }

    /** One or more repetitions of `this`, separated by `p`, whose results are ignored. */
    fun <B> sep1(p: Parser<B>): Parser<List<A>> =
            this.map2({ p.skipLeft { this }.many() }, { left, right -> right.setHead(left) })

    /** One or more repetitions of `this`, separated by `p`, whose results are ignored. */
    fun <B> sep(p: Parser<B>): Parser<List<A>> =
            this.sep1(p).or({ succeed(empty()) })

    fun label(message: String): Parser<A>

    fun scope(message: String): Parser<A>

    fun attempt(): Parser<A>
}

inline fun <A> Parser<A>.or(p: () -> Parser<A>): Parser<A> = TODO()

open class AbstractParser<T>(val parser: Parser<T>) {
    fun <A> run(parser: Parser<A>, input: String): Either<ParseError, A> = TODO()
}

sealed class ParseError(val stack: List<Pair<Location, String>>) {

}

sealed class Location(val input: String, offset: Int = 0) {
    val line: Int by lazy { input.slice(0..offset + 1).count { it == '\n' } }
    val col: Int by lazy {
        val last = input.slice(0..offset + 1).lastIndexOf('\n')
        when (last) {
            -1 -> offset + 1
            else -> offset - last
        }
    }
}

fun errorLocation(e: ParseError): Location = TODO()
fun errorMessage(e: ParseError): String = TODO()


class Laws<T>(val parser: AbstractParser<T>) {
    fun <A> equal(p1: Parser<A>, p2: Parser<A>, g: Gen<String>): Prop =
            forAll(g, { parser.run(p1, it) == parser.run(p2, it) })

    fun <A> mapLaw(p: Parser<A>, g: Gen<String>): Prop =
            equal(p, p.map({ it }), g)
}

class ParserOps<A>(val parser: Parser<A>) {

    fun numA() = parser.char('a').many().map({ it.length })
}

interface ParserType {

    fun <A, B> map(p: ParserKind<A>, f: (A) -> B): ParserKind<B> =
            flatMap(p, { succeed(f(it)) })

    fun <A> many(p: ParserKind<A>): ParserKind<List<A>> =
            or(map2(p, { many(p) }, { a, list -> list.setHead(a) }), { succeed(empty()) })

    fun <A> or(left: ParserKind<A>, right: () -> ParserKind<A>): ParserKind<A>

/*    fun char(c: Char): Parser<Char> =
            map(string(c.toString()), { it })*/

    fun <A> slice(p: ParserKind<A>): ParserKind<String>

    /*fun <B> product(p: () -> Parser<B>): Parser<Pair<A, B>> =
            this.flatMap({ a -> p().map { Pair(a, it) } })*/

    fun <A, B, C> map2(p1: ParserKind<A>, p2: () -> ParserKind<B>, f: (A, B) -> C): ParserKind<C> =
            flatMap(p1, { a: A -> map(p2(), { f(a, it) }) })

    fun <A, B> flatMap(p: ParserKind<A>, f: (A) -> ParserKind<B>): ParserKind<B>

    fun <A> succeed(a: A): ParserKind<A>

    fun <A> listOfN(n: Int, p: ParserKind<A>): ParserKind<List<A>> =
            if (n <= 0) succeed(empty())
            else map2(p, { listOfN(n - 1, p) }, { a, list -> list.setHead(a) })


    fun regex(r: Regex): ParserKind<String>

    fun string(s: String): ParserKind<String>

    /*
    fun <B> to(p: ParserKind<A>, b: B): ParserKind<B> =
            this.slice().map { _ -> b }*/

    fun <A> asStringParser(a: A, f: (A) -> Parser<String>) = f(a)

    fun <A, B> skipLeft(left: ParserKind<A>, right: () -> ParserKind<B>): ParserKind<B> =
            map2(left, right, { _, b -> b })

    fun <A, B> skipRight(left: ParserKind<A>, right: () -> ParserKind<B>): ParserKind<A> =
            map2(left, right, { a, _ -> a })

    /*fun <A, B, C> surround(left: ParserKind<A>, right: ParserKind<C>, p: () -> ParserKind<B>): ParserKind<B> =
            skipRight(skipLeft(left, { p }), { right })*/

    /** Parser which consumes zero or more whitespace characters. */
    fun whitespace(): ParserKind<String> = regex(Regex("\\s*"))

    /** Parser which consumes 1 or more digits. */
    fun digits(): ParserKind<String> = regex(Regex("\\d+"))

    /** Parser which consumes reluctantly until it encounters the given string. */
    fun thru(s: String): ParserKind<String> = regex(Regex(".*?$s"))

    /** Unescaped string literals, like "foo" or "bar". */
    //fun quoted(): Parser<String> = string("\"").skipLeft({ thru("\"") }).map { it.dropLast(1) }

    /** Attempts `p` and strips trailing whitespace, usually used for the tokens of a grammar. */
/*    fun token(): ParserKind<A> =
            this.skipRight({ whitespace() })*/

    /** Unescaped or escaped string literals, like "An \n important \"Quotation\"" or "bar". */
   /* fun escapedQuoted(): Parser<String> =
            token().quoted()*/

    /** C/Java style floating point literals, e.g .1, -1.0, 1e9, 1E-23, etc.
     * Result is left as a string to keep full precision
     */
/*    fun doubleString(): Parser<String> =
            regex(Regex("[-+]?([0-9]*\\.)?[0-9]+([eE][-+]?[0-9]+)?")).token()*/

    /** Floating point literals, converted to a `Double`. */
/*    fun double(): Parser<Double> =
            doubleString().map { it.toDouble() }*/

    /** One or more repetitions of `this`, separated by `p`, whose results are ignored. */
    /*fun <B> sep1(p: Parser<B>): Parser<List<A>> =
            this.map2({ p.skipLeft { this }.many() }, { left, right -> right.setHead(left) })*/

    /** One or more repetitions of `this`, separated by `p`, whose results are ignored. */
/*    fun <B> sep(p: Parser<B>): Parser<List<A>> =
            this.sep1(p).or({ succeed(empty()) })*/

    //fun label(message: String): Parser<A>

    //fun scope(message: String): Parser<A>

   // fun attempt(): Parser<A>
}
