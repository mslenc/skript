package skript.analysis

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import skript.ast.*
import skript.io.ModuleName
import skript.io.ModuleSourceSkript

class VarAllocatorTest {
    @Test
    fun testBasics() {
        val source = """
            fun makeInc() {
                var c = 0;
                fun result() {
                    c += 1;
                    return c;
                }
                return result;
            }
        """.trimIndent()

        val moduleName = ModuleName("testBasics")
        val module = ModuleSourceSkript(moduleName, source).parse()

        VarAllocator(moduleName).visitModule(module)

        val makeInc = module[0] as DeclareFunction

        val makeIncScope = makeInc.innerFunScope
        assertEquals(2, makeIncScope.varsAllocated)
        assertEquals(0, makeIncScope.closureDepthNeeded)

        val result = makeInc.body.parts[1] as DeclareFunction

        val resultScope = result.innerFunScope
        assertEquals(0, resultScope.varsAllocated)
        assertEquals(1, resultScope.closureDepthNeeded)
    }
}