package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import skript.io.ModuleSourceProvider
import skript.io.ParsedModuleProvider
import skript.io.SkriptEngine
import skript.values.SkNumber
import skript.values.SkValue
import java.math.BigDecimal



class OurFirstTest {
    @Test
    fun testAddition() = runBlocking {
        val sourceProvider = ModuleSourceProvider.static(emptyMap<String, String>())
        val moduleProvider = ParsedModuleProvider.from(sourceProvider)
        val skriptEngine = SkriptEngine(moduleProvider)

        val script = """
            emit(3.5 + 4.6);
            emit(2.1 + 3.9);
            
            var a = 2.2, b = 3.3;
            emit(a + b);
        """.trimIndent()

        val outputs = ArrayList<SkValue>()

        val env = skriptEngine.createEnv()
        env.setGlobal("emit", emitInto(outputs))

        env.runAnonymousScript(script)

        assertEquals(3, outputs.size)

        assertTrue(outputs[0] is SkNumber)
        assertEquals(0, outputs[0].asNumber().value.compareTo(BigDecimal("8.1")))

        assertTrue(outputs[1] is SkNumber)
        assertEquals(0, outputs[1].asNumber().value.compareTo(BigDecimal("6")))

        assertTrue(outputs[2] is SkNumber)
        assertEquals(0, outputs[2].asNumber().value.compareTo(BigDecimal("5.5")))
    }
}