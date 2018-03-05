package parsing

import datastructures.List
import datastructures.empty
import datastructures.setHead
import errorhandling.Either
import higherkind.Kind

interface IParser<F> {

    fun <A> run(parser: Kind<F, A>, input: String): Either<ParseError, A>

    fun string(s: String): Kind<F, String>

    fun <A> slice(parser: Kind<F, A>): Kind<F, String>

    fun <A> succeed(a: A): Kind<F, A>

    fun <A> or(parser1: Kind<F, A>, parser2: () -> Kind<F, A>): Kind<F, A>

    fun <A, B> flatMap(parser: Kind<F, A>, f: (A) -> Kind<F, B>): Kind<F, B>

    fun regex(r: Regex): Kind<F, String>

    fun <A> defaultSucceed(a: A): Kind<F, A> =
            map(string(""), { a })

    fun <A> many(parser: Kind<F, A>): Kind<F, List<A>> =
            or(map2(parser, many(parser), { a, list -> list.setHead(a) }), { succeed(empty()) })

    fun <A> many1(parser: Kind<F, A>): Kind<F, List<A>> =
            map2(parser, many(parser), { a, list -> list.setHead(a) })

    fun <A, B> map(parser: Kind<F, A>, f: (A) -> B): Kind<F, B> =
            flatMap(parser, { a: A -> succeed(f(a)) })

    fun <A, B, C> map2(pa: Kind<F, A>, pb: Kind<F, B>, f: (A, B) -> C): Kind<F, C> =
            flatMap(pa, { a -> map(pb, { f(a, it) }) })

    fun char(c: Char): Kind<F, Char> =
            map(string(c.toString()), { it[0] })

    fun <A, B> product(parser1: Kind<F, A>, parser2: () -> Kind<F, B>): Kind<F, Pair<A, B>> =
            flatMap(parser1, { a -> map(parser2(), { Pair(a, it) }) })

    fun <A, B> `as`(parser: Kind<F, A>, b: B) =
            map(slice(parser), { _ -> b })

    fun <A, B> skipLeft(left: Kind<F, A>, right: () -> Kind<F, B>): Kind<F, B> =
            map2(slice(left), right(), { _, b -> b })

    fun <A, B> skipRight(left: Kind<F, A>, right: () -> Kind<F, B>): Kind<F, A> =
            map2(left, slice(right()), { a, _ -> a })

    fun <A, B, C> surround(start: Kind<F, A>, stop: Kind<F, B>, parser: () -> Kind<F, C>): Kind<F, C> =
            skipLeft(start, { skipRight(parser(), { stop }) })

    fun <A> root(parser: Kind<F, A>): Kind<F, A> = TODO()
}
