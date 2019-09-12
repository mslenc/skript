package skript.values

import skript.exec.RuntimeState
import skript.io.SkriptEnv
import skript.util.SkArguments
import skript.util.expectBoolean
import skript.util.expectString

class SkStringRange(val start: String, val end: String, val endInclusive: Boolean) : SkObject() {
    override val klass: SkClassDef
        get() = SkStringRangeClassDef

    override suspend fun contains(key: SkValue, state: RuntimeState): Boolean {
        val str = key.asString()

        return when {
            str.value < start -> false
            endInclusive && str.value > end -> false
            !endInclusive && str.value >= end -> false
            else -> true
        }
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
