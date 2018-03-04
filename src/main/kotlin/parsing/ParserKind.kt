package parsing

import higherkind.Kind

interface ParserKind<A> : Kind<ParseState, Result<A>>

interface FunctionK

class JsonParse : ParserKind<Json> {
}