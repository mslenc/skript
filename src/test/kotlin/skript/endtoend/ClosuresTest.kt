package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
import skript.io.toSkript
import skript.runScriptWithEmit

class ClosuresTest {
    @Test
    fun testClosureBasics() = runBlocking {
        val outputs = runScriptWithEmit("""
            fun makeIncMaker(initialStep) {
                fun create(initialValue) {
                    var value = initialValue
                    var step = initialStep
                    
                    fun next() {
                        var result = value
                        value += step
                        return result
                    }
                    
                    fun setStep(newStep) {
                        step = newStep
                    }
                    
                    return [ next, setStep ]
                }
                
                return create
            }
 
            var create = makeIncMaker(5)
            var pack = create(12)
            var next = pack[0]
            var setStep = pack[1]
            
            [ 3, 5, 2, 4, 1 ].forEach(fun(i) {
                emit(next())
                setStep(i)
            });
            emit(next())
            emit(next())

        """.trimIndent())

        val expect = listOf(
            12.toSkript(),
            17.toSkript(),
            20.toSkript(),
            25.toSkript(),
            27.toSkript(),
            31.toSkript(),
            32.toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }

    @Test
    fun testClosureViaNative() = runBlocking {
        val outputs = runScriptWithEmit("""
            fun makeIncMakers() {
                val result = [];            
            
                [ 1, 2, 3 ].forEach(fun(step) {
                    val holder = [];
                    [ step ].forEach(fun(stepAgain) {
                        holder.add(fun() {
                            val cur = 0;
                            return fun() {
                                return cur += step;
                            };
                        }());
                    });
                    
                    result.add(holder[0]);
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
            1.toSkript(),
            2.toSkript(),
            2.toSkript(),
            4.toSkript(),
            3.toSkript(),
            6.toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }
}