package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
import skript.io.toSkript
import skript.runScriptWithEmit

class OurFirstTest {
    @Test
    fun testAddition() = runBlocking {
        val outputs = runScriptWithEmit("""

            emit(3.5 + 4.6);
            emit(2.1 + 3.9);
            
            var a = 2.2, b = 3.3;
            emit(a + b);

        """.trimIndent())

        val expect = listOf(
            8.1.toSkript(),
            6.0.toSkript(),
            5.5.toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }

    @Test
    fun testRanges1() = runBlocking {
        val outputs = runScriptWithEmit("""
            
            for (i in 1..10) {
                if (i !in 3..8)
                    emit(i);
            }
            
        """.trimIndent())

        val expect = listOf(
            1.toSkript(),
            2.toSkript(),
            9.toSkript(),
            10.toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }
}