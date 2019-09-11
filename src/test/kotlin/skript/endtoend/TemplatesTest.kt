package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
import skript.io.toSkript
import skript.runScriptWithEmit

class TemplatesTest {
    @Test
    fun testTemplateBasics() = runBlocking {
        val result = runScriptWithEmit("""
            var num1 = 12;
            var num2 = 25;
            var str1 = "abc";
            var str2 = "def";
            
            var t1 = `Hello ${'$'}{ (2 + num1) * (3 + num2) }!`;
            var t2 = `Hello ${'$'}{ str1 } ${'$'}{ str2 }!`;
            
            emit(t1);
            emit(t2);
            emit(`Abc ${'$'}{ str2 }`);
        """.trimIndent())

        val expect = listOf(
            "Hello 392!".toSkript(),
            "Hello abc def!".toSkript(),
            "Abc def".toSkript()
        )

        assertEmittedEquals(expect, result)
    }
}