package parsing

import datastructures.*
import datastructures.List
import errorhandling.Either
import errorhandling.Left
import errorhandling.Right
import errorhandling.getOrElse
import higherkind.Kind
import java.util.regex.Pattern

data class ParseState(val location: Location) {

    val input: String
        get() = location.input.substring(location.offset)

    fun slice(n: Int) = location.input.substring(location.offset, location.offset + n)

    fun advanceBy(numChars: Int): ParseState =
            ParseState(Location(location.input, location.offset + numChars))
}

sealed class Result<out A> {

    fun extract(): Either<ParseError, A> = when (this) {
        is Success -> Right(this.result)
        is Failure -> Left(this.error)
    }

    fun uncommit(): Result<A> = when (this) {
        is Success -> this
        is Failure -> Failure(this.error, false)
    }

    fun addCommit(isCommitted: Boolean): Result<A> = when (this) {
        is Success -> this
        is Failure -> Failure(this.error, this.isCommitted || isCommitted)
    }

    fun mapError(f: (ParseError) -> ParseError): Result<A> = when (this) {
        is Failure -> Failure(f(this.error), this.isCommitted)
        is Success -> this
    }

    fun advanceSuccess(n: Int): Result<A> = when (this) {
        is Success -> Success(this.result, this.length + n)
        is Failure -> this
    }
}

data class Success<out A>(val result: A, val length: Int) : Result<A>() {

}

data class Failure(val error: ParseError, val isCommitted: Boolean) : Result<Nothing>()

data class ParseError(val stack: List<Pair<Location, String>>) {
    fun label(msg: String) =
            ParseError(list(
                    stack.head().map {
                        Pair(it.first, msg)
                    }.getOrElse { Pair(Location("", 0), "") }
            ))

    private fun collapseErrors() = stack.toKList()
            .groupBy { it.first }
            .mapValues { it.value.joinToString(";") { it.second } }
            .toList()
            .sortedBy { it.first.offset }

    override fun toString(): String {
        return if (stack.isEmpty()) "No error messages"
        else {
            // val collapsed = collapseErrors()
            val errors = stack.map { "Error in '${it.second}': ${it.first}" }
                    .foldLeft(StringBuilder()) { str, builder -> builder.append(str) }
            return errors.toString()
        }
    }
}

data class Location(val input: String, val offset: Int = 0) {
    val line: Int by lazy { input.slice(0..offset + 1).count { it == '\n' } }
    val col: Int by lazy {
        val last = input.slice(0..offset + 1).lastIndexOf('\n')
        when (last) {
            -1 -> offset + 1
            else -> offset - last
        }
    }

    fun toError(msg: String) = ParseError(list(Pair(this, msg)))

    val ANSI_RED = "\u001B[31m"
    val ANSI_RESET = "\u001B[0m"

    override fun toString(): String {
        return "\nLine: $line:$col\n${input.substring(0, offset)}$ANSI_RED${input.substring(offset, offset + 1)}$ANSI_RESET\n\n"
    }
}

fun errorLocation(e: ParseError): Location = TODO()
fun errorMessage(e: ParseError): String = TODO()

interface IParser<F> {

    fun <A> run(parser: Kind<F, A>, input: String): Either<ParseError, A>

    fun string(s: String): Kind<F, String>

    fun <A> slice(parser: Kind<F, A>): Kind<F, String>

    fun <A> succeed(a: A): Kind<F, A>

    fun <A> or(parser1: Kind<F, A>, parser2: () -> Kind<F, A>): Kind<F, A>

    fun <A, B> flatMap(parser: Kind<F, A>, f: (A) -> Kind<F, B>): Kind<F, B>

    fun <A> label(msg: String, parser: Kind<F, A>): Kind<F, A>

    fun <A> scope(msg: String, parser: Kind<F, A>): Kind<F, A>

    fun <A> attempt(parser: Kind<F, A>): Kind<F, A>

    fun regex(r: Regex): Kind<F, String>

    fun <A> defaultSucceed(a: A): Kind<F, A> =
            map(string("")) { a }

    fun <A> many(parser: Kind<F, A>): Kind<F, List<A>> =
            or(map2(parser, { many(parser) }, { a, list -> list.setHead(a) })) { succeed(empty()) }

    fun <A> many1(parser: Kind<F, A>): Kind<F, List<A>> =
            map2(parser, { many(parser) }, { a, list -> list.setHead(a) })

    fun <A, B> map(parser: Kind<F, A>, f: (A) -> B): Kind<F, B> =
            flatMap(parser) { a: A -> succeed(f(a)) }

    fun <A, B, C> map2(pa: Kind<F, A>, pb: () -> Kind<F, B>, f: (A, B) -> C): Kind<F, C> =
            flatMap(pa) { a -> map(pb()) { f(a, it) } }

    fun char(c: Char): Kind<F, Char> =
            map(string(c.toString())) { it[0] }

    fun <A, B> product(parser1: Kind<F, A>, parser2: () -> Kind<F, B>): Kind<F, Pair<A, B>> =
            flatMap(parser1) { a -> map(parser2()) { Pair(a, it) } }

    fun <A, B> `as`(parser: Kind<F, A>, b: B) =
            map(slice(parser)) { _ -> b }

    fun <A, B> skipLeft(left: Kind<F, A>, right: () -> Kind<F, B>): Kind<F, B> =
            map2(slice(left), { right() }, { _, b -> b })

    fun <A, B> skipRight(left: Kind<F, A>, right: () -> Kind<F, B>): Kind<F, A> =
            map2(left, { slice(right()) }, { a, _ -> a })

    fun <A, B, C> surround(start: Kind<F, A>, stop: Kind<F, B>, parser: () -> Kind<F, C>): Kind<F, C> =
            skipLeft(start) { skipRight(parser()) { stop } }

    /** Parser which consumes zero or more whitespace characters. */
    fun whitespace() = regex(Regex("\\s*"))

    /** Parser which consumes 1 or more digits. */
    fun digits() = regex(Regex("\\d+"))

    /** Parser which consumes reluctantly until it encounters the given string. */
    fun thru(s: String) = regex(Regex(".*?${Pattern.quote(s)}"))

    /** Unescaped string literals, like "foo" or "bar". */
    fun quoted() = map(skipLeft(string("\"")) { thru("\"") }) { it.dropLast(1) }

    /** Attempts `p` and strips trailing whitespace, usually used for the tokens of a grammar. */
    fun <A> token(parser: Kind<F, A>): Kind<F, A> =
            skipRight(attempt(parser)) { whitespace() }

    /** Unescaped or escaped string literals, like "An \n important \"Quotation\"" or "bar". */
    fun escapedQuoted(): Kind<F, String> =
            token(label("string literal", quoted()))

    /** C/Java style floating point literals, e.g .1, -1.0, 1e9, 1E-23, etc.
     * Result is left as a string to keep full precision
     */
    fun doubleString(): Kind<F, String> =
            token(regex(Regex("^[-+]?([0-9]*\\.)?[0-9]+([eE][-+]?[0-9]+)?")))

    /** Floating point literals, converted to a `Double`. */
    fun double(): Kind<F, Double> =
            map(doubleString()) { it.toDouble() }

    /** One or more repetitions of `parser`, separated by `ignored`, whose results are ignored. */
    fun <A, B> sep1(parser: Kind<F, A>, ignored: Kind<F, B>): Kind<F, List<A>> =
            map2(parser, { many(skipLeft(ignored) { parser }) }, { element, list: List<A> -> list.setHead(element) })

    /** Zero or more repetitions of `parser`, separated by `ignored`, whose results are ignored. */
    fun <A, B> sep(parser: Kind<F, A>, ignored: Kind<F, B>): Kind<F, List<A>> =
            or(sep1(parser, ignored)) { succeed(empty()) }

    fun eof(): Kind<F, String> =
            label("unexpected trailing character", regex(Regex("\\z")))

    fun <A> root(parser: Kind<F, A>): Kind<F, A> =
            skipRight(parser) { eof() }
}
