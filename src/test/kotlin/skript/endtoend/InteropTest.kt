package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
import skript.exec.ParamType
import skript.interop.*
import skript.io.toSkript
import skript.runScriptWithEmit
import skript.typeError
import kotlin.reflect.full.findParameterByName
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.primaryConstructor

data class TestObj(
    var foo: String = "deffoo",
    val bar: Int = 1000
) {
    fun fooBar(suffix: String): String {
        return foo + bar + suffix
    }
}

class InteropTest {
    @Test
    fun testManuallyConstructedClass() = runBlocking {
        val TestObjClass = SkNativeClassDef("TestObj", TestObj::class, null)

        TestObjClass.defineInstanceMethod(
            SkNativeMethod(
                "fooBar",
                TestObj::fooBar.instanceParameter!!,
                listOf(
                    ParamInfo("suffix", TestObj::fooBar.findParameterByName("suffix") ?: typeError("Couldn't find suffix"), ParamType.NORMAL, SkCodecString)
                ),
                SkCodecString,
                TestObj::fooBar, TestObjClass)
        )

        TestObjClass.defineNativeProperty(SkNativeMutableProperty(TestObj::foo, SkCodecString))
        TestObjClass.defineNativeProperty(SkNativeReadOnlyProperty(TestObj::bar, SkCodecInt))

        TestObjClass.constructor = SkNativeConstructor("TestObj::constructor", listOf(
            ParamInfo("foo", TestObj::class.primaryConstructor!!.findParameterByName("foo")!!, ParamType.NORMAL, SkCodecString),
            ParamInfo("bar", TestObj::class.primaryConstructor!!.findParameterByName("bar")!!, ParamType.NORMAL, SkCodecInt)
        ), TestObj::class.primaryConstructor!!, TestObjClass)

        val result = runScriptWithEmit({ env ->
            env.setGlobal("TestObj", env.getClassObject(TestObjClass))
        }, """
            
            emit(TestObj("abc", 123).fooBar(" km"));
            emit(TestObj(foo = "def", 234).fooBar(" m"));
            emit(TestObj("ghi", bar = 345).fooBar(" cm"));
            emit(TestObj(foo = "jkl", bar = 456).fooBar(" mm"));
            emit(TestObj(foo = "jkl").fooBar(" um"));
            emit(TestObj(bar = 666).fooBar(" nm"));
            emit(TestObj().fooBar(" pm"));
            
            var bibi = TestObj(bar=432);
            bibi.foo = "daFoo";
            emit(bibi.fooBar(" Mm"));
        """.trimIndent())

        val expect = listOf(
            "abc123 km".toSkript(),
            "def234 m".toSkript(),
            "ghi345 cm".toSkript(),
            "jkl456 mm".toSkript(),
            "jkl1000 um".toSkript(),
            "deffoo666 nm".toSkript(),
            "deffoo1000 pm".toSkript(),
            "daFoo432 Mm".toSkript()
        )

        assertEmittedEquals(expect, result)
    }
}