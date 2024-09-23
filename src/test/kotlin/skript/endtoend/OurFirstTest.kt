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

    @Test
    fun testStringFunctions() = runBlocking {
        val outputs = runScriptWithEmit("""

            emit("abcDE".substring(1, -1))
            emit("abcDE".toLowerCase())
            emit("abcDE".toUpperCase())
            emit("   abcDE   ".trim())
            emit("   abcDE   ".trim(start = false))
            emit("   abcDE   ".trim(end = false))
            emit("   __abc__DE__   ".trim(chars= " _"))
            
        """.trimIndent())

        val expect = listOf(
            "bcD",
            "abcde",
            "ABCDE",
            "abcDE",
            "   abcDE",
            "abcDE   ",
            "abc__DE"
        ).map { it.toSkript() }

        assertEmittedEquals(expect, outputs)
    }

    @Test
    fun testElif() = runBlocking {
        val outputs = runScriptWithEmit("""
            var a = 13;
            while (a++ < 21) {
                if (a % 15 == 0) { emit("FizzBuzz"); }
                elif (a % 5 == 0) { emit("Fizz") }
                elif (a % 3 == 0) emit("Buzz")
                else emit(a.toString()); 
            }

        """.trimIndent())

        val expect = listOf(
            "14",
            "FizzBuzz",
            "16",
            "17",
            "Buzz",
            "19",
            "Fizz",
            "Buzz"
        ).map { it.toSkript() }

        assertEmittedEquals(expect, outputs)
    }
}