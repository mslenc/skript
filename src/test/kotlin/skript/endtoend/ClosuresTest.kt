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
            
            [ 3, 5, 2, 4, 1 ].forEach(fun(i) {
                emit(next());
                setStep(i);
            });
            emit(next());
            emit(next());

        """.trimIndent())

        val expect = listOf(
            BigDecimal("12").toSkript(),
            BigDecimal("17").toSkript(),
            BigDecimal("20").toSkript(),
            BigDecimal("25").toSkript(),
            BigDecimal("27").toSkript(),
            BigDecimal("31").toSkript(),
            BigDecimal("32").toSkript()
        )

        assertEquals(expect.size, outputs.size)

        for (i in expect.indices)
            assertTrue(strictlyEqual(expect[i], outputs[i]))
    }

    @Test
    fun testClosureViaNative() = runBlocking {
        val outputs = runScriptWithEmit("""
            fun makeIncMakers() {
                val result = [];            
            
                [ 1, 2, 3 ].forEach(fun(step) {
                    val holder = [];
                    [ step ].forEach(fun(stepAgain) {
                        holder.push(fun() {
                            val cur = 0;
                            return fun() {
                                return cur += step;
                            };
                        }());
                    });
                    
                    result.push(holder[0]);
                });
                
                return result;
            }
            
            val incMakers = makeIncMakers();
            
            emit(incMakers[0]());
            emit(incMakers[0]());
            emit(incMakers[1]());
            emit(incMakers[1]());
            emit(incMakers[2]());
            emit(incMakers[2]());
        """.trimIndent())

        val expect = listOf(
            BigDecimal("1").toSkript(),
            BigDecimal("2").toSkript(),
            BigDecimal("2").toSkript(),
            BigDecimal("4").toSkript(),
            BigDecimal("3").toSkript(),
            BigDecimal("6").toSkript()
        )

        assertEquals(expect.size, outputs.size)

        for (i in expect.indices)
            assertTrue(strictlyEqual(expect[i], outputs[i])) { "${expect[i]} vs ${outputs[i]}" }
    }
}