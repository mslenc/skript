package skript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import skript.exec.RuntimeState
import skript.io.ModuleSourceProvider
import skript.io.ParsedModuleProvider
import skript.io.SkriptEngine
import skript.io.SkriptEnv
import skript.opcodes.equals.strictlyEqual
import skript.util.ArgsExtractor
import skript.values.SkFunction
import skript.values.SkUndefined
import skript.values.SkValue
import kotlin.math.min

fun emitInto(outputs: MutableList<SkValue>): SkFunction {
    return object : SkFunction("emit", listOf("value")) {
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
    return runScriptWithEmit({ }, script)
}

suspend fun runScriptWithEmit(initEnv: (SkriptEnv)->Unit, script: String): List<SkValue> {
    val sourceProvider = ModuleSourceProvider.static(emptyMap<String, String>())
    val moduleProvider = ParsedModuleProvider.from(sourceProvider)
    val skriptEngine = SkriptEngine(moduleProvider)

    val outputs = ArrayList<SkValue>()

    val env = skriptEngine.createEnv()
    initEnv(env)
    env.setGlobal("emit", emitInto(outputs))

    env.runAnonymousScript(script)

    return outputs
}

fun assertEmittedEquals(expected: List<SkValue>, actual: List<SkValue>) {
    for (i in 0 until min(expected.size, actual.size))
        assertStrictlyEqual(expected[i], actual[i]) { "Element $i:\nExpected: ${expected[i]} (${expected[i].getKind()})\n  Actual: ${actual[i]} (${actual[i].getKind()})" }

    assertEquals(expected.size, actual.size, "Number of elements should be the same")
}

inline fun assertStrictlyEqual(expected: SkValue, actual: SkValue, crossinline message: ()->String = { "Expected: $expected (${expected.getKind()})\n  Actual: $actual (${actual.getKind()})" }) {
    assertTrue(strictlyEqual(expected, actual)) { message() }
}