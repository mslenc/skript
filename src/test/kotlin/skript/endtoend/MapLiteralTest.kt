package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
import skript.io.pack
import skript.io.toSkript
import skript.runScriptWithEmit

class MapLiteralTest {
    @Test
    fun testMapLiteralBasics() = runBlocking {
        val outputs = runScriptWithEmit("""
            val first = { a: "A", b: "B", c: 12 };
            
            val second = {
                **first,
                [ first.a ]: "bigA"
            };

            for ((key, value) in first) {
                emit(key);
                emit(value);
            }
            
            for ((key, value) in second) {
                emit(key);
                emit(value);
            }

        """.trimIndent())

        val expect = listOf(
            "a".toSkript(), "A".toSkript(),
            "b".toSkript(), "B".toSkript(),
            "c".toSkript(), 12.toSkript(),

            "a".toSkript(), "A".toSkript(),
            "b".toSkript(), "B".toSkript(),
            "c".toSkript(), 12.toSkript(),
            "A".toSkript(), "bigA".toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }

    @Test
    fun testConversionToJson() = runBlocking {
        val outputs = runScriptWithEmit("""
            
            emit({
                foo: "bar",
                list: [ 1, 2.43, 3.011d ],
                bools: {
                    t: true,
                    f: false
                }
            });
            
        """.trimIndent())

        val json = outputs[0].toJson()

        assertEquals("""
            {"foo":"bar","list":[1.0,2.43,3.011],"bools":{"t":true,"f":false}}
        """.trimIndent(), json.toString())
    }

    @Test
    fun testRemovingWorks() = runBlocking {
        val outputs = runScriptWithEmit("""
            val map1 = { a: "bcd", e: "fgh" }
            emit(map1)
            
            val map2 = { a: "bcd", e: "fgh" }
            map2["e"] = undefined
            emit(map2)
            
            val map3 = { a: "bcd", e: "fgh" }
            map3.remove("e")
            emit(map3)
            
        """.trimIndent())

        assertEquals("{s1as3bcds1es3fgh}", pack(outputs[0]))
        assertEquals("{s1as3bcds1eu}", pack(outputs[1]))
        assertEquals("{s1as3bcd}", pack(outputs[2]))
    }
}