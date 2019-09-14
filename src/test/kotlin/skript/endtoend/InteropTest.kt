package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
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

    companion object {
        @JvmStatic
        val bibi = "Bibi!!!"
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
                SkNativeParams(listOf(
                    SkNativeParamNormal("suffix", TestObj::fooBar.findParameterByName("suffix") ?: typeError("Couldn't find suffix"), SkCodecString)
                )),
                SkCodecString,
                TestObj::fooBar, TestObjClass)
        )

        TestObjClass.defineInstanceProperty(SkNativeMutableProperty("foo", false, TestObjClass, TestObj::class, SkCodecString, TestObj::foo.getter, TestObj::foo.setter))
        TestObjClass.defineInstanceProperty(SkNativeReadOnlyProperty("bar", false, TestObjClass, TestObj::class, SkCodecInt, TestObj::bar.getter))

        TestObjClass.constructor = SkNativeConstructor("TestObj::constructor", SkNativeParams(listOf(
            SkNativeParamNormal("foo", TestObj::class.primaryConstructor!!.findParameterByName("foo")!!, SkCodecString),
            SkNativeParamNormal("bar", TestObj::class.primaryConstructor!!.findParameterByName("bar")!!, SkCodecInt)
        )), TestObj::class.primaryConstructor!!, TestObjClass)

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

    @Test
    fun testReflectedClass() = runBlocking {
        val result = runScriptWithEmit({ env ->
            env.setClassAsGlobal(TestObj::class) // now isn't this much easier than the 1.2kb/19 lines of code above? :)
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
                        
            emit(TestObj.bibi);
        """.trimIndent())

        val expect = listOf(
            "abc123 km".toSkript(),
            "def234 m".toSkript(),
            "ghi345 cm".toSkript(),
            "jkl456 mm".toSkript(),
            "jkl1000 um".toSkript(),
            "deffoo666 nm".toSkript(),
            "deffoo1000 pm".toSkript(),
            "daFoo432 Mm".toSkript(),
            "Bibi!!!".toSkript()
        )

        assertEmittedEquals(expect, result)
    }
}