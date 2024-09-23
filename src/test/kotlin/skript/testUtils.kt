package skript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import skript.io.*
import skript.opcodes.equals.strictlyEqual
import skript.util.SkArguments
import skript.values.SkFunction
import skript.values.SkUndefined
import skript.values.SkValue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.min
import kotlin.reflect.KClass

fun emitInto(outputs: MutableList<SkValue>): SkFunction {
    return object : SkFunction("emit", listOf("value")) {
        override suspend fun call(args: SkArguments, env: SkriptEnv): SkValue {
            val value = args.extractArg("value")
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
    val sourceProvider = ModuleSourceProvider.static(emptyMap(), emptyMap())
    val moduleProvider = ParsedModuleProvider.from(sourceProvider)
    val skriptEngine = SkriptEngine(moduleProvider, nativeAccessGranter = object : NativeAccessGranter {
        override fun isAccessAllowed(klass: KClass<*>): Boolean {
            return klass == LocalDate::class || klass == LocalDateTime::class
        }
    })

    val outputs = ArrayList<SkValue>()

    val env = skriptEngine.createEnv()
    initEnv(env)
    env.setGlobal("emit", emitInto(outputs))

    env.runAnonymousScript(script)

    return outputs
}

suspend fun runTemplate(template: String): String {
    return runTemplate({ }, template)
}

suspend fun runTemplate(initEnv: (SkriptEnv)->Unit, template: String, escape: String = "raw"): String {
    val sourceProvider = ModuleSourceProvider.static(emptyMap(), emptyMap())
    val moduleProvider = ParsedModuleProvider.from(sourceProvider)
    val skriptEngine = SkriptEngine(moduleProvider, nativeAccessGranter = object : NativeAccessGranter {
        override fun isAccessAllowed(klass: KClass<*>): Boolean {
            return klass == LocalDate::class || klass == LocalDateTime::class
        }
    })

    val env = skriptEngine.createEnv()
    initEnv(env)
    return env.runAnonymousTemplate(template, escape, timeZone = ZoneId.of("America/Los_Angeles"))
}

fun assertEmittedEquals(expected: List<SkValue>, actual: List<SkValue>) {
    for (i in 0 until min(expected.size, actual.size))
        assertStrictlyEqual(expected[i], actual[i]) { "Element $i:\nExpected: ${expected[i]} (${expected[i].getKind()})\n  Actual: ${actual[i]} (${actual[i].getKind()})" }

    assertEquals(expected.size, actual.size, "Number of elements should be the same")
}

inline fun assertStrictlyEqual(expected: SkValue, actual: SkValue, crossinline message: ()->String = { "Expected: $expected (${expected.getKind()})\n  Actual: $actual (${actual.getKind()})" }) {
    assertTrue(strictlyEqual(expected, actual)) { message() }
}