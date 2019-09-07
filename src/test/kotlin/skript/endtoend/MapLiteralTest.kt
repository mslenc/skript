package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
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
}