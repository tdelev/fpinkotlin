package parsing

import datastructures.setHead
import errorhandling.Either
import higherkind.Kind

class ForParser private constructor()
data class ParserC<A>(val parser: (ParseState) -> Result<A>) : Kind<ForParser, A>

fun <A> Kind<ForParser, A>.fix() = this as ParserC<A>


object Reference : IParser<ForParser> {

    override fun <A> run(parser: Kind<ForParser, A>, input: String): Either<ParseError, A> {
        val startState = ParseState(Location(input, 0))
        val parserInstance = parser.fix().parser
        return parserInstance(startState).extract()
    }

    override fun <A> succeed(a: A): Kind<ForParser, A> {
        return ParserC({ _ -> Success(a, 0) })
    }

    override fun string(s: String): Kind<ForParser, String> {
        val msg = "'$s'"

        return ParserC({ state ->
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
        return ParserC({ state ->
            val match = r.find(state.input)
            when (match) {
                null -> Failure(state.location.toError(msg), false)
                else -> Success(match.value, match.value.length)
            }
        })
    }

    override fun <A, B> flatMap(parser: Kind<ForParser, A>, f: (A) -> Kind<ForParser, B>): Kind<ForParser, B> {
        return ParserC({ state ->
            val parserInstance = parser.fix().parser
            val parseResult = parserInstance(state)
            when (parseResult) {
                is Success -> f(parseResult.result).fix().parser(state.advanceBy(parseResult.length))
                        .addCommit(parseResult.length != 0)
                        .advanceSuccess(parseResult.length)
                is Failure -> parseResult
            }
        })
    }

    override fun <A> slice(parser: Kind<ForParser, A>): Kind<ForParser, String> {
        return ParserC({ state ->
            val parserInstance = parser.fix().parser
            val parseResult = parserInstance(state)
            when (parseResult) {
                is Success -> Success(state.slice(parseResult.length), parseResult.length)
                is Failure -> parseResult
            }
        })
    }

    override fun <A> or(parser1: Kind<ForParser, A>, parser2: () -> Kind<ForParser, A>): Kind<ForParser, A> {
        return ParserC({ state ->
            val parser = parser1.fix().parser
            val parseResult = parser(state)
            when (parseResult) {
                is Failure -> parser2().fix().parser(state)
                else -> parseResult
            }
        })
    }

    override fun <A> label(msg: String, parser: Kind<ForParser, A>): Kind<ForParser, A> {
        return ParserC({ state ->
            val parserInstance = parser.fix().parser
            parserInstance(state).mapError { it.label(msg) }
        })
    }

    override fun <A> scope(msg: String, parser: Kind<ForParser, A>): Kind<ForParser, A> {
        return ParserC({ state ->
            val parserInstance = parser.fix().parser
            parserInstance(state).mapError { ParseError(it.stack.setHead(Pair(state.location, msg))) }
        })
    }

    override fun <A> attempt(parser: Kind<ForParser, A>): Kind<ForParser, A> {
        return ParserC({ state ->
            val parserInstance = parser.fix().parser
            parserInstance(state).uncommit()
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