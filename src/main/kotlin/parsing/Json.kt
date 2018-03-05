package parsing

import datastructures.List

sealed class Json

object JNull : Json()
data class JNumber(val number: Double) : Json()
data class JString(val str: String) : Json()
data class JBool(val bool: Boolean) : Json()
data class JArray(val array: List<Json>) : Json()
data class JObject(val obj: Map<String, Json>) : Json()

class JsonParser(parser: Parser<Json>) : AbstractParser<Json>(parser) {

    fun token(s: String) = parser.string(s)

    val literal: Parser<Json> = parser.string("null").to(JNull as Json)
            .or({ parser.double().map { JNumber(it) } })
            .or({ parser.escapedQuoted().map { JString(it) } })
            .or({ parser.string("true").to(JBool(true)) })
            .or({ parser.string("false").to(JBool(false)) })

    val keyval: Parser<Pair<String, Json>> =
            parser.escapedQuoted().product({ parser.string(":").skipLeft { value } })

    val obj: Parser<Json> = parser.string("{").surround(parser.string("}"), {
        keyval.sep(parser.string(",")).map {
            it.foldRight(mutableMapOf<String, Json>(), { element, result ->
                result[element.first] = element.second
                result
            })
        }.map { JObject(it) } as Parser<Json>
    })

    val array: Parser<Json> = parser.string("[").surround(parser.string("]"), {
        value.sep(parser.string(",")).map { JArray(it) }
    }) as Parser<Json>

    val value = literal.or({ obj }).or({ array })

}

object JSON {
    fun getParser(parser: IParser<ForParser>): ParserC<Json> {

        val literal = parser.map(parser.string("a"), { JString(it) })

        return parser.root(literal).fix()
    }

}