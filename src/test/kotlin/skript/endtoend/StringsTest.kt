package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
import skript.io.toSkript
import skript.runScriptWithEmit

class StringsTest {
    @Test
    fun testStringSplit() = runBlocking {
        val result = runScriptWithEmit("""
            val str = "50036: 115.00; 50037: 750.00"
            val parts = str.split(";")
            emit(parts.size)
            emit(parts[0].trim())
            emit(parts[1].trim())
            
            val str2 = "50250: 780.00"
            val parts2 = str2.split(";")
            emit(parts2.size)
            emit(parts2[0])            
        """.trimIndent())

        val expect = listOf(
            2.toSkript(),
            "50036: 115.00".toSkript(),
            "50037: 750.00".toSkript(),

            1.toSkript(),
            "50250: 780.00".toSkript()
        )

        assertEmittedEquals(expect, result)
    }
}