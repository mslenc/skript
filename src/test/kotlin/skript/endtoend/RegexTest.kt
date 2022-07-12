package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
import skript.io.toSkript
import skript.runScriptWithEmit

class RegexTest {
    @Test
    fun testRegexBasics() = runBlocking {
        val outputs = runScriptWithEmit("""
            fun surroundMatch(match) {
                return `[${'$'}{ match.value }]`;
            }
            
            val regex = Regex("[a-z]+", ignoreCase = true);
            val result = regex.replace("This is it!", surroundMatch);
            
            emit(result);
            emit(regex matches "123");
            emit(regex matches "abc");
            emit(regex matches "Abc?");
        """.trimIndent())

        val expect = listOf(
            "[This] [is] [it]!".toSkript(),
            false.toSkript(),
            true.toSkript(),
            false.toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }

    @Test
    fun testRegexBasics2() = runBlocking {
        val outputs = runScriptWithEmit("""
            fun cleanUp(email) {
                val atRE = Regex.fromLiteral("@")
                if (!atRE.containsMatchIn(email))
                    return email
                    
                val parts = atRE.split(email, 2)
                
                val dotRE = Regex.fromLiteral(".")
                val before = dotRE.replace(parts[0], "")
                val after = parts[1]
                
                return `${'$'}{ before }@${'$'}{ after }`
            }
            
            emit(cleanUp(false))
            emit(cleanUp("john.doe@gmail.com"))            
        """.trimIndent())

        val expect = listOf(
            false.toSkript(),
            "johndoe@gmail.com".toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }
}