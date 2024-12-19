package skript

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import skript.io.*
import skript.opcodes.equals.strictlyEqual
import skript.templates.TemplateRuntime
import skript.util.SkArguments
import skript.values.*
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

suspend fun runScriptWithEmit(script: String, moduleSources: Map<String, String> = emptyMap()): List<SkValue> {
    return runScriptWithEmit({ }, script, moduleSources)
}

suspend fun runScriptWithEmit(initEnv: (SkriptEnv)->Unit, script: String, moduleSources: Map<String, String> = emptyMap()): List<SkValue> {
    val sourceProvider = ModuleSourceProvider.static(moduleSources, emptyMap())
    val moduleProvider = ModuleProvider.from(sourceProvider)
    val skriptEngine = SkriptEngine(nativeAccessGranter = object : NativeAccessGranter {
        override fun isAccessAllowed(klass: KClass<*>): Boolean {
            return klass == LocalDate::class || klass == LocalDateTime::class
        }
    })

    val outputs = ArrayList<SkValue>()

    val env = skriptEngine.createEnv(moduleProvider = moduleProvider)
    initEnv(env)
    env.setGlobal("emit", emitInto(outputs))

    env.runAnonymousScript(script)

    return outputs
}

suspend fun runTemplate(ctx: Map<String, Any?>, template: String, escape: String = "raw"): String {
    val skriptEngine = SkriptEngine(nativeAccessGranter = object : NativeAccessGranter {
        override fun isAccessAllowed(klass: KClass<*>): Boolean {
            return klass == LocalDate::class || klass == LocalDateTime::class
        }
    })

    val env = skriptEngine.createEnv()
    val sb = StringBuilder()
    val out = TemplateRuntime.createWithDefaults(sb, defaultEscapeKey = escape, timeZone = ZoneId.of("America/Los_Angeles"))
    env.runAnonymousTemplate(template, ctx, out)
    return sb.toString()
}

fun assertEmittedEquals(expected: List<SkValue>, actual: List<SkValue>) {
    for (i in 0 until min(expected.size, actual.size)) {
        val a = expected[i]
        val b = actual[i]

        if (a is SkList && b is SkList) {
            assertEmittedEquals(a.listEls, b.listEls)
        } else {
            assertStrictlyEqual(expected[i], actual[i]) { "Element $i:\nExpected: ${expected[i]} (${expected[i].getKind()})\n  Actual: ${actual[i]} (${actual[i].getKind()})" }
        }
    }

    assertEquals(expected.size, actual.size, "Number of elements should be the same")
}

inline fun assertStrictlyEqual(expected: SkValue, actual: SkValue, crossinline message: ()->String = { "Expected: $expected (${expected.getKind()})\n  Actual: $actual (${actual.getKind()})" }) {
    assertTrue(strictlyEqual(expected, actual)) { message() }
}