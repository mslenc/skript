package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
import skript.io.toSkript
import skript.runScriptWithEmit
import skript.values.SkList

class ImportExportTest {
    @Test
    fun testBasicImporting() = runBlocking {
        val sources = mapOf(
            "moduleA" to """
                export fun join(a, b) {
                    return `${ '$' }{ a }${ '$' }{ b }`
                }
                
                export val PI = 3.1415926
                
                fun add(a, b) {
                    return a + b
                }
                
                fun double(a) {
                    return add(a, a)
                }
                
                export { double }
            """.trimIndent(),
            "moduleB" to """
                import * as A from "moduleA"
                
                fun gimmeResults() {
                    return [
                        A.PI,
                        A.join("Hello", "World"),
                        A.double(7),
                    ]
                }
                
                export default gimmeResults;
            """.trimIndent()
        )

        val outputs = runScriptWithEmit("""
            import { join, double } from "moduleA"
            import gimmeResults from "moduleB"
            
            emit(gimmeResults())
            emit(join("Welcome", "Jungle"))
            emit(double(12))
        """.trimIndent(), sources)

        val expect = listOf(
            SkList(listOf(
                3.1415926.toSkript(),
                "HelloWorld".toSkript(),
                14.toSkript()
            )),
            "WelcomeJungle".toSkript(),
            24.toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }
}