package skript.values

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.github.mslenc.utils.ComparableRangeEx
import skript.io.SkriptEnv
import skript.util.SkArguments
import skript.util.expectBoolean
import skript.util.expectString

class SkStringRange(val start: String, val end: String, val endInclusive: Boolean) : SkObject() {
    override val klass: SkClassDef
        get() = SkStringRangeClassDef

    override suspend fun contains(key: SkValue, env: SkriptEnv): Boolean {
        val str = key.asString()

        return when {
            str.value < start -> false
            endInclusive && str.value > end -> false
            !endInclusive && str.value >= end -> false
            else -> true
        }
    }

    override fun unwrap(): Any = when {
        endInclusive -> start..end
        else -> ComparableRangeEx(start, end)
    }

    override suspend fun toJson(factory: JsonNodeFactory): JsonNode {
        val list = factory.arrayNode()

        list.add(start)
        list.add(end)
        list.add(endInclusive)

        return list
    }
}

object SkStringRangeClassDef : SkClassDef("StringRange", SkObjectClassDef) {
    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        val start = args.expectString("start", coerce = true, ifUndefined = "")
        val end = args.expectString("end", coerce = true, ifUndefined = "")
        val endInclusive = args.expectBoolean("endInclusive", coerce = true, ifUndefined = true)
        args.expectNothingElse()

        return SkStringRange(start, end, endInclusive)
    }
}
