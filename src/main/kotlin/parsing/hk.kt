package parsing

data class Kind<F, A>(val a: Any)
typealias PT<A> = (ParseState) -> Result<A>

val <A> PT<A>.higher: Kind<PT<A>, A>
    get() = Kind(this)

val <A> Kind<PT<A>, A>.lower: PT<A>
    get() = this.a as PT<A>

abstract class ParserA<F, A> {
    abstract fun succeed(a: A): Kind<F, A>

    abstract fun string(s: String): Kind<PT<String>, String>

    abstract fun <B> flatMap(p: F, f: (A) -> B): Kind<F, B>

}

class JParser<A> : ParserA<PT<A>, A>() {
    override fun succeed(a: A): Kind<PT<A>, A> = { _: ParseState ->
        Success(a, 0)
    }.higher

    override fun string(s: String): Kind<PT<String>, String> = {
        state: ParseState ->
        val msg = "'$s'"
        // TODO: complete
        Success("", 0)
    }.higher

}