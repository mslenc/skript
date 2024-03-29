package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
import skript.assertStrictlyEqual
import skript.interop.*
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.runScriptWithEmit
import skript.typeError
import skript.util.SkArguments
import skript.values.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.*
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

    fun makeDate(str: String): LocalDate {
        return LocalDate.parse(str)
    }

    companion object {
        val bibi = "Bibi!!!"
    }
}

enum class TestEnum {
    ENUM,
    CLASS,
    TEST
}

class TestNativeClass(val prefix: String) {
    fun getTester(suffix: String): TestSkObject {
        return TestSkObject(prefix + suffix)
    }
}

class TestSkObject(val src: String) : SkObject() {
    override val klass: SkClassDef
        get() = TestSkObjectClassDef

    override suspend fun propertyGet(key: String, env: SkriptEnv): SkValue {
        return when (key) {
            "lower" -> src.lowercase().toSkript()
            "upper" -> src.uppercase().toSkript()
            else -> super.propertyGet(key, env)
        }
    }

    override suspend fun entryGet(key: SkValue, env: SkriptEnv): SkValue {
        return when (key.asString().value) {
            "LOWER" -> src.lowercase().toSkript()
            "UPPER" -> src.uppercase().toSkript()
            else -> super.entryGet(key, env)
        }
    }
}

object TestSkObjectClassDef : SkCustomClass<TestSkObject>("TestSkObject")

class TestMapsObject {
    fun getDailyData(): Map<DayOfWeek, List<Int>> {
        return mapOf(
            DayOfWeek.THURSDAY to listOf(3, 17),
            DayOfWeek.FRIDAY to listOf(22, 11, 0)
        )
    }

    fun processDailyData(data: Map<DayOfWeek, List<Int>>): String {
        return TreeMap(data).toString()
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
            emit(TestObj(234, foo = "def").fooBar(" m"));
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
            emit(TestObj(234, foo = "def").fooBar(" m"));
            emit(TestObj("ghi", bar = 345).fooBar(" cm"));
            emit(TestObj(foo = "jkl", bar = 456).fooBar(" mm"));
            emit(TestObj(foo = "jkl").fooBar(" um"));
            emit(TestObj(bar = 666).fooBar(" nm"));
            emit(TestObj().fooBar(" pm"));
            
            var bibi = TestObj(bar=432);
            bibi.foo = "daFoo";
            emit(bibi.fooBar(" Mm"));
                        
            emit(TestObj.bibi);
            
            emit(bibi.makeDate("2019-12-18") == bibi.makeDate("2019-12-19"));
            emit(bibi.makeDate("2019-12-18") == TestObj().makeDate("2019-12-19"));
            emit(bibi.makeDate("2019-12-19") == TestObj().makeDate("2019-12-19"));
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
            "Bibi!!!".toSkript(),
            false.toSkript(),
            false.toSkript(),
            true.toSkript()
        )

        assertEmittedEquals(expect, result)
    }

    @Test
    fun testReflectedEnumClass() = runBlocking {
        val testObj = ObjectWithEnumProps()
        val testObj2 = ObjectWithEnumProps()

        val result = runScriptWithEmit({ env ->
            env.setClassAsGlobal(DayOfWeek::class)
            env.setNativeGlobal("testObj", testObj)
            env.setNativeGlobal("testObj2", testObj2)
        }, """
            testObj.day = DayOfWeek.THURSDAY;
            testObj.testSet("abc")
            
            testObj2.testSet(day = DayOfWeek.WEDNESDAY, name = "Wed");
            
            for (value in DayOfWeek.values()) {
              emit(value.name)
            }
        """.trimIndent())

        val expect = listOf(
            "MONDAY".toSkript(),
            "TUESDAY".toSkript(),
            "WEDNESDAY".toSkript(),
            "THURSDAY".toSkript(),
            "FRIDAY".toSkript(),
            "SATURDAY".toSkript(),
            "SUNDAY".toSkript(),
        )

        assertEmittedEquals(expect, result)

        assertEquals(DayOfWeek.THURSDAY, testObj.day)
        assertEquals("abc", testObj.funName)
        assertEquals(DayOfWeek.SATURDAY, testObj.funDay)

        assertEquals("Wed", testObj2.funName)
        assertEquals(DayOfWeek.WEDNESDAY, testObj2.funDay)
    }

    @Test
    fun testReflectedKotlinEnumClass() = runBlocking {
        val testObj = ObjectWithEnumProps()
        val testObj2 = ObjectWithEnumProps()

        val result = runScriptWithEmit({ env ->
            env.setClassAsGlobal(DayOfWeek::class)
            env.setClassAsGlobal(TestEnum::class)
            env.setNativeGlobal("testObj", testObj)
            env.setNativeGlobal("testObj2", testObj2)
        }, """
            testObj.day = DayOfWeek.THURSDAY;
            testObj.testSet("abc")
            
            testObj2.testSet(day = DayOfWeek.WEDNESDAY, name = "Wed");
            testObj2.kotEnum = TestEnum.TEST;
            
            for (value in DayOfWeek.values()) {
              emit(value.name)
            }
        """.trimIndent())

        val expect = listOf(
            "MONDAY".toSkript(),
            "TUESDAY".toSkript(),
            "WEDNESDAY".toSkript(),
            "THURSDAY".toSkript(),
            "FRIDAY".toSkript(),
            "SATURDAY".toSkript(),
            "SUNDAY".toSkript(),
        )

        assertEmittedEquals(expect, result)

        assertEquals(DayOfWeek.THURSDAY, testObj.day)
        assertEquals("abc", testObj.funName)
        assertEquals(DayOfWeek.SATURDAY, testObj.funDay)

        assertEquals("Wed", testObj2.funName)
        assertEquals(DayOfWeek.WEDNESDAY, testObj2.funDay)

        assertEquals(TestEnum.TEST, testObj2.kotEnum)
    }

    @Test
    fun testNativeClassReturningSkObject() = runBlocking {
        val result = runScriptWithEmit({ env ->
            env.setNativeGlobal("testerFactory", TestNativeClass("Test_"))
        }, """
            val tester = testerFactory.getTester("mimI")
            
            emit(tester.upper)
            emit(tester.lower)
            emit(tester.bubu)
            emit(tester["UPPER"])
            emit(tester["LOWER"])
            emit(tester["BUBU"])
        """.trimIndent())

        val expect = listOf(
            "TEST_MIMI".toSkript(),
            "test_mimi".toSkript(),
            SkUndefined,
            "TEST_MIMI".toSkript(),
            "test_mimi".toSkript(),
            SkUndefined
        )

        assertEmittedEquals(expect, result)
    }

    @Test
    fun testBasicMapSupport() = runBlocking {
        val result = runScriptWithEmit({ env ->
            env.setClassAsGlobal(DayOfWeek::class)
            env.setNativeGlobal("tester", TestMapsObject())
        }, """
            val dailyData = tester.getDailyData()
            
            emit(dailyData[DayOfWeek.THURSDAY])
            emit(dailyData[DayOfWeek.FRIDAY])
            emit(dailyData[DayOfWeek.SATURDAY])
            
            emit(tester.processDailyData(dailyData))
        """.trimIndent())

        val firstList = result[0] as SkAbstractList

        assertStrictlyEqual(3.toSkript(), firstList.getSlot(0))
        assertStrictlyEqual(17.toSkript(), firstList.getSlot(1))
        assertEquals(2, firstList.getSize())

        val secondList = result[1] as SkAbstractList

        assertStrictlyEqual(22.toSkript(), secondList.getSlot(0))
        assertStrictlyEqual(11.toSkript(), secondList.getSlot(1))
        assertStrictlyEqual(0.toSkript(), secondList.getSlot(2))
        assertEquals(3, secondList.getSize())

        assertStrictlyEqual(SkUndefined, result[2])

        assertStrictlyEqual("{THURSDAY=[3, 17], FRIDAY=[22, 11, 0]}".toSkript(), result[3])
    }

    @Test
    fun testEventHandler() = runBlocking {
        val ui = UiBuilder()
        lateinit var env: SkriptEnv

        val result = runScriptWithEmit({
            env = it
            it.setNativeGlobal("ui", ui)
        }, """
            ui.setButton("Test", fun(text) {
                emit(123)
                emit(text)
            })
        """.trimIndent())

        ui.handler?.call(SkArguments().apply { addPosArg("test123".toSkript()) }, env)

        assertEquals(2, result.size)
        assertStrictlyEqual(123.toSkript(), result[0])
        assertStrictlyEqual("test123".toSkript(), result[1])
    }
}

class UiBuilder {
    var text: String? = null
    var handler: SkScriptFunction? = null

    fun setButton(text: String, handler: SkScriptFunction) {
        this.text = text
        this.handler = handler
    }
}

class ObjectWithEnumProps {
    var day: DayOfWeek? = null
    var funDay: DayOfWeek? = null
    var funName: String? = null
    var kotEnum: TestEnum? = null

    fun testSet(name: String, day: DayOfWeek = DayOfWeek.SATURDAY) {
        funName = name
        funDay = day
    }
}