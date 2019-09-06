package skript.endtoend

import skript.exec.RuntimeState
import skript.io.ModuleSourceProvider
import skript.io.ParsedModuleProvider
import skript.io.SkriptEngine
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

suspend fun runScriptWithEmit(script: String): List<SkValue> {
    val sourceProvider = ModuleSourceProvider.static(emptyMap<String, String>())
    val moduleProvider = ParsedModuleProvider.from(sourceProvider)
    val skriptEngine = SkriptEngine(moduleProvider)

    val outputs = ArrayList<SkValue>()

    val env = skriptEngine.createEnv()
    env.setGlobal("emit", emitInto(outputs))

    env.runAnonymousScript(script)

    return outputs
}