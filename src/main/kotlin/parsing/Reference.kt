package parsing

import errorhandling.Either
import higherkind.Kind

class ForParser private constructor()
data class ParserC<A>(val parser: (ParseState) -> Result<A>) : Kind<ForParser, A>

fun <A> Kind<ForParser, A>.fix() = this as ParserC<A>

object Reference : IParser<ForParser> {

    override fun <A> run(parser: Kind<ForParser, A>, input: String): Either<ParseError, A> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <A> succeed(a: A): Kind<ForParser, A> {
        return ParserC({ _ -> Success(a, 0) })
    }

    override fun string(s: String): Kind<ForParser, String> {
        return ParserC({ _ -> Success("", 0) })
    }

    override fun regex(r: Regex): Kind<ForParser, String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <A, B> flatMap(parser: Kind<ForParser, A>, f: (A) -> Kind<ForParser, B>): Kind<ForParser, B> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <A> slice(parser: Kind<ForParser, A>): Kind<ForParser, String> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <A> or(parser1: Kind<ForParser, A>, parser2: () -> Kind<ForParser, A>): Kind<ForParser, A> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <A> label(msg: String, parser: Kind<ForParser, A>): Kind<ForParser, A> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <A> scope(msg: String, parser: Kind<ForParser, A>): Kind<ForParser, A> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <A> attempt(parser: Kind<ForParser, A>): Kind<ForParser, A> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}