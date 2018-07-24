package parsing

import datastructures.List
import errorhandling.Either
import higherkind.Kind

sealed class Json

object JNull : Json()
data class JNumber(val value: Double) : Json() {
    override fun toString() = value.toString()
}

data class JString(val value: String) : Json() {
    override fun toString() = value
}

data class JBool(val value: Boolean) : Json() {
    override fun toString() = value.toString()
}

data class JArray(val array: List<Json>) : Json() {
    override fun toString() = array.toString()
}

data class JObject(val value: Map<String, Json>) : Json() {
    override fun toString(): String {
        return value.map { "${it.key} : ${it.value}" }.joinToString("\n")
    }
}


class JSON(private val parser: IParser<ForParser>) {
    private fun <A> Kind<ForParser, A>.or(p: () -> Kind<ForParser, A>) = parser.or(this, p)
    private fun <A> Kind<ForParser, A>.scope(msg: String) = parser.scope(msg, this)
    private fun <A, B> Kind<ForParser, A>.to(b: B) = parser.`as`(this, b)
    fun <A, B> Kind<ForParser, A>.map(f: (A) -> B) = parser.map(this, f)

    fun token(str: String) = parser.token(parser.string(str))

    val literal = token("null").to(JNull).or {
        parser.double().map {
            JNumber(it)
        }
    }.or {
        parser.escapedQuoted().map { JString(it) }
    }.or {
        token("false").to(JBool(false))
    }.or {
        token("true").to(JBool(true))
    }.scope("literal")

    val value: Kind<ForParser, Json> = literal.or { obj }.or { array }

    val keyval = parser.product(parser.escapedQuoted()) { parser.skipLeft(token(":")) { value } }

    val obj = parser.surround(token("{"), token("}")) {
        parser.sep(keyval, token(",")).map { kvs ->
            val map = kvs.foldLeft(mutableMapOf<String, Json>()) { element, map ->
                map[element.first] = element.second
                map
            }
            JObject(map)
        }.scope("object")
    }

    val array = parser.surround(token("["), token("]")) {
        parser.sep(value, token(",")).map {
            JArray(it)
        }.scope("array")
    }

    fun parse(): Parser<Json> {
        return parser.root(parser.skipLeft(parser.whitespace()) { obj.or { array } }).fix()
    }

}

fun Either<ParseError, Json>.print() = this.fold(::println, ::println)

fun main(args: Array<String>) {
    val jsonParser = JSON(Reference)

    val jsonTxt = """
{
  "Company name" : "Microsoft Corporation",
  "Ticker"  : "MSFT",
  "Active"  : true,
  "Price"   : 30.66,
  "Shares outstanding" : 8.38e9,
  "Related companies" : [ "HPQ", "IBM", "YHOO", "DELL", "GOOG" ]
}
"""

    val result = Reference.run(jsonParser.parse(), jsonTxt)
    result.print()

    val malformedJson1 = """
{
  "Company name" : "Microsoft Corporation
}
"""
    val errorResult = Reference.run(jsonParser.parse(), malformedJson1)
    errorResult.print()

    val malformedJson2 = """
[
  [ "HPQ", "IBM",
  "YHOO", "DELL" ,
  "GOOG"
  ]
]
"""

    val errorResult2 = Reference.run(jsonParser.parse(), malformedJson2)
    errorResult2.print()
}