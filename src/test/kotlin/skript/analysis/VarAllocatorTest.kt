package skript.analysis

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import skript.ast.*
import skript.io.ModuleSource
import skript.io.parse

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

        val module = ModuleSource(source, "testBasics", "testBasics").parse()

        VarAllocator(GlobalScope()).visitModule(module)

        val makeInc = module.content[0] as DeclareFunction
        assertTrue(makeInc.props[Scope] is FunctionScope)

        val makeIncScope = makeInc.props[Scope] as FunctionScope
        assertEquals(2, makeIncScope.varsAllocated)
        assertEquals(0, makeIncScope.closureDepthNeeded)

        val result = makeInc.body.parts[1] as DeclareFunction
        assertTrue(result.props[Scope] is FunctionScope)

        val resultScope = result.props[Scope] as FunctionScope
        assertEquals(0, resultScope.varsAllocated)
        assertEquals(1, resultScope.closureDepthNeeded)
    }
}