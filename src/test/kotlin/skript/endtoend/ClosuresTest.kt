package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import skript.io.toSkript
import skript.opcodes.equals.strictlyEqual
import java.math.BigDecimal

class ClosuresTest {
    @Test
    fun testClosureBasics() = runBlocking {
        val outputs = runScriptWithEmit("""
            fun makeIncMaker(initialStep) {
                fun create(initialValue) {
                    var value = initialValue;
                    var step = initialStep;
                    
                    fun next() {
                        var result = value;
                        value += step;
                        return result;
                    }
                    
                    fun setStep(newStep) {
                        step = newStep;
                    }
                    
                    return [ next, setStep ];
                }
                
                return create;
            }
            
            var create = makeIncMaker(5);
            var pack = create(12);
            var next = pack[0];
            var setStep = pack[1];
            
            emit(next());
            emit(next());
            setStep(3);
            emit(next());
            emit(next());

        """.trimIndent())

        val expect = listOf(
            BigDecimal("12").toSkript(),
            BigDecimal("17").toSkript(),
            BigDecimal("22").toSkript(),
            BigDecimal("25").toSkript()
        )

        assertEquals(expect.size, outputs.size)

        for (i in expect.indices)
            assertTrue(strictlyEqual(expect[i], outputs[i]))
    }
}