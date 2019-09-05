package skript.endtoend

import skript.exec.RuntimeState
import skript.util.ArgsExtractor
import skript.values.SkFunction
import skript.values.SkUndefined
import skript.values.SkValue

fun emitInto(outputs: MutableList<SkValue>): SkFunction {
    return object : SkFunction("emit") {
        override suspend fun call(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
            val args = ArgsExtractor(posArgs, kwArgs, "emit")
            val value = args.extractParam("value")
            args.expectNothingElse()
            outputs += value
            return SkUndefined
        }
    }
}