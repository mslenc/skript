package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
import skript.io.toSkript
import skript.runScriptWithEmit

class LoopsTest {
    @Test
    fun testLoopBasics() = runBlocking {
        val outputs = runScriptWithEmit("""
            var a = 5;
            while (a <= 7) {
                emit(a++);
            }
            while (a <= 3) {
                emit(a++);
            }
            emit(a);
            
            var b = 5;
            do {
                emit(b++);
            } while (b <= 7)
            do {
                emit(b++);
            } while (b <= 3);
            emit(b);
            

        """.trimIndent())

        val expect = listOf(
            5, 6, 7,
            // (nothing),
            8,

            5, 6, 7,
            8,
            9
        ).map { it.toSkript() }

        assertEmittedEquals(expect, outputs)
    }

    @Test
    fun testBreak() = runBlocking {
        val outputs = runScriptWithEmit("""
            var a = 5;
            while (a < 100) {
                emit(a++)
                if (a % 8 == 0) {
                    break
                }
            }

        """.trimIndent())

        val expect = listOf(
            5, 6, 7
        ).map { it.toSkript() }

        assertEmittedEquals(expect, outputs)
    }

    @Test
    fun testContinue() = runBlocking {
        val outputs = runScriptWithEmit("""
            var a = 13;
            while (a++ < 21) {
                if (a % 15 == 0) { emit("FizzBuzz"); continue }
                if (a % 5 == 0) { emit("Fizz"); continue }
                if (a % 3 == 0) { emit("Buzz"); continue; }
                emit(a.toString()); 
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

    @Test
    fun testContinueOuter() = runBlocking {
        val outputs = runScriptWithEmit("""
            outer@
            for (i in 1..3) {
                emit(i)
                for (j in 10..13) {
                    emit(i + j)
                    if (j == 12) { 
                        if (i == 2) 
                            continue@outer
                        else
                            continue 
                    }
                    emit(j) 
                }
                emit(i)
            }

        """.trimIndent())

        val cmp = ArrayList<Int>()

        outer@
        for (i in 1..3) {
            cmp += (i)
            for (j in 10..13) {
                cmp += (i + j)
                if (j == 12) {
                    if (i == 2)
                        continue@outer
                    else
                        continue
                }
                cmp += (j)
            }
            cmp += (i)
        }

        val expect = cmp.map { it.toSkript() }

        assertEmittedEquals(expect, outputs)
    }
}