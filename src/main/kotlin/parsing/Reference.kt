package parsing

import datastructures.setHead
import errorhandling.Either
import higherkind.Kind

class ForParser private constructor()
data class Parser<out A>(val parser: (ParseState) -> Result<A>) : Kind<ForParser, A>

fun <A> Kind<ForParser, A>.fix() = this as Parser<A>
fun <A> Kind<ForParser, A>.parser() = this.fix().parser


object Reference : IParser<ForParser> {

    override fun <A> run(parser: Kind<ForParser, A>, input: String): Either<ParseError, A> {
        val startState = ParseState(Location(input, 0))
        val parserInstance = parser.parser()
        return parserInstance(startState).extract()
    }

    override fun <A> succeed(a: A): Kind<ForParser, A> {
        return Parser({ _ -> Success(a, 0) })
    }

    override fun string(s: String): Kind<ForParser, String> {
        val msg = "'$s'"

        return Parser({ state ->
            val i = firstNonmatchingIndex(state.location.input, s, state.location.offset)
            if (i == -1) {
                Success(s, s.length)
            } else {
                Failure(state.location.toError(msg), i != 0)
            }
        })
    }

    override fun regex(r: Regex): Kind<ForParser, String> {
        val msg = "regex $r"
        return Parser({ state ->
            val match = r.find(state.input)
            when (match) {
                null -> Failure(state.location.toError(msg), false)
                else -> Success(match.value, match.value.length)
            }
        })
    }

    override fun <A, B> flatMap(parser: Kind<ForParser, A>, f: (A) -> Kind<ForParser, B>): Kind<ForParser, B> {
        return Parser({ state ->
            val result = parser.parser()(state)
            when (result) {
                is Success -> f(result.result).parser()(state.advanceBy(result.length))
                        .addCommit(result.length != 0)
                        .advanceSuccess(result.length)
                is Failure -> result
            }
        })
    }

    override fun <A> slice(parser: Kind<ForParser, A>): Kind<ForParser, String> {
        return Parser({ state ->
            val result = parser.parser()(state)
            when (result) {
                is Success -> Success(state.slice(result.length), result.length)
                is Failure -> result
            }
        })
    }

    override fun <A> or(parser1: Kind<ForParser, A>, parser2: () -> Kind<ForParser, A>): Kind<ForParser, A> {
        return Parser({ state ->
            val result = parser1.parser()(state)
            when (result) {
                is Failure -> {
                    if (result.isCommitted) result
                    else parser2().parser()(state)
                }
                else -> result
            }
        })
    }

    override fun <A> label(msg: String, parser: Kind<ForParser, A>): Kind<ForParser, A> {
        return Parser({ state ->
            val result = parser.parser()(state)
            result.mapError { it.label(msg) }
        })
    }

    override fun <A> scope(msg: String, parser: Kind<ForParser, A>): Kind<ForParser, A> {
        return Parser({ state ->
            val result = parser.parser()(state)
            result.mapError { ParseError(it.stack.setHead(Pair(state.location, msg))) }
        })
    }

    override fun <A> attempt(parser: Kind<ForParser, A>): Kind<ForParser, A> {
        return Parser({ state ->
            parser.parser()(state).uncommit()
        })
    }

    /** Returns -1 if s1.startsWith(s2), otherwise returns the
     * first index where the two strings differed. If s2 is
     * longer than s1, returns s1.length. */
    fun firstNonmatchingIndex(s1: String, s2: String, offset: Int): Int {
        if (offset >= s1.length) return -1
        var i = 0
        while (i < s1.length && i < s2.length) {
            if (s1[i + offset] != s2[i]) return i
            ++i
        }
        return if (s1.length - offset >= s2.length) -1
        else s1.length - offset
    }
}