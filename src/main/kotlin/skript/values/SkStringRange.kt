package skript.values

import skript.util.ArgsExtractor
import skript.util.expectBoolean
import skript.util.expectString

class SkStringRange(val start: String, val end: String, val endInclusive: Boolean) : SkObject(SkStringRangeClass) {
    override suspend fun hasOwnMember(key: SkValue): Boolean {
        val str = key.asString()

        return when {
            str.value < start -> false
            endInclusive && str.value > end -> false
            !endInclusive && str.value >= end -> false
            else -> true
        }
    }
}

object SkStringRangeClass : SkClass("StringRange", ObjectClass) {
    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        val args = ArgsExtractor(posArgs, kwArgs, "StringRange")

        val start = args.expectString("start", coerce = true, ifUndefined = "")
        val end = args.expectString("end", coerce = true, ifUndefined = "")
        val endInclusive = args.expectBoolean("endInclusive", coerce = true, ifUndefined = true)
        args.expectNothingElse()

        return SkStringRange(start, end, endInclusive)
    }
}
