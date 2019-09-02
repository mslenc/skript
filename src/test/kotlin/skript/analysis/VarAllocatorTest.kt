package skript.analysis

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import skript.ast.*
import skript.values.SkNumber
import skript.values.SkValue

class VarAllocatorTest {
    @Test
    fun testBasics() {
        val module = Module("testBasics", listOf(
            DeclareFunction("makeInc", emptyList(), Statements(listOf<Statement>(
                LetStatement(listOf(VarDecl("c", Literal(SkNumber.valueOf(0))))),
                DeclareFunction("result", emptyList(), Statements(listOf(
                    ExpressionStatement(AssignExpression(Variable("c"), BinaryOp.PLUS, Literal(SkNumber.ONE))),
                    ReturnStatement(Variable("c"))
                ))),
                ReturnStatement(Variable("result"))
            )))
        ))

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