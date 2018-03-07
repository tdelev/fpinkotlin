package parsing

import datastructures.List
import higherkind.Kind

sealed class Json

object JNull : Json()
data class JNumber(val number: Double) : Json()
data class JString(val str: String) : Json()
data class JBool(val bool: Boolean) : Json()
data class JArray(val array: List<Json>) : Json()
data class JObject(val obj: Map<String, Json>) : Json()


class JSON(private val parser: IParser<ForParser>) {
    fun <A> Kind<ForParser, A>.or(p: () -> Kind<ForParser, A>) = parser.or(this, p)
    fun <A> Kind<ForParser, A>.scope(msg: String) = parser.scope(msg, this)
    fun <A, B> Kind<ForParser, A>.to(b: B) = parser.`as`(this, b)
    fun <A, B> Kind<ForParser, A>.map(f: (A) -> B) = parser.map(this, f)

    fun token(str: String) = parser.token(parser.string(str))

    val literal = token("null").to(JNull).or({
        parser.double().map({ JNumber(it) })
    }).or({
        parser.escapedQuoted().map({ JString(it) })
    }).or({
        token("false").to(JBool(false))
    }).or({
        token("true").to(JBool(true))
    })

    val value: Kind<ForParser, Json> = literal.or { obj }.or { array }

    val keyval = parser.product(parser.escapedQuoted(), { parser.skipLeft(token(":"), { value }) })

    val obj = parser.surround(token("{"), token("}"), {
        parser.sep(keyval, token(",")).map { kvs ->
            val map = kvs.foldLeft(mutableMapOf<String, Json>(), { element, map ->
                map[element.first] = element.second
                map
            })
            JObject(map)
        }.scope("object")
    })

    val array = parser.surround(token("["), token("]"), {
        parser.sep(value, token(",")).map {
            println("IT: $it")
            JArray(it)
        }.scope("array")
    })

    fun parse(): ParserC<Json> {
        return parser.root(parser.skipLeft(parser.whitespace(), { obj.or { array } })).fix()
    }

}

object JSONExample {
    @JvmStatic
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
        println("Result: $result")

    }
}