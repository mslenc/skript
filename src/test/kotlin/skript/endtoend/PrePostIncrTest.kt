package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
import skript.io.toSkript
import skript.runScriptWithEmit

class PrePostIncrTest {
    @Test
    fun testPreIncrementBasics() = runBlocking {
        val outputs = runScriptWithEmit("""
            val a = 5;
            val b = [ 1, 2, 3 ];
            val c = { value: 8, arr: [ 12, { subVal: 20 } ] };
            
            emit(++a);
            emit(a);
            
            emit(++b[1]);
            emit(b[1]);
            
            emit(++c.value);
            emit(c.value);
            
            emit(++c.arr[0]);
            emit(++c["arr"][0]);
            emit(c.arr[0]);
            
            emit(++c.arr[1].subVal);
            emit(++c["arr"][1].subVal);
            emit(c["arr"][1].subVal);
        """.trimIndent())

        val expect = listOf(
            6.toSkript(),
            6.toSkript(),
            3.toSkript(),
            3.toSkript(),
            9.toSkript(),
            9.toSkript(),
            13.toSkript(),
            14.toSkript(),
            14.toSkript(),
            21.toSkript(),
            22.toSkript(),
            22.toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }

    @Test
    fun testPreDecrementBasics() = runBlocking {
        val outputs = runScriptWithEmit("""
            val a = 5;
            val b = [ 1, 2, 3 ];
            val c = { value: 8, arr: [ 12, { subVal: 20 } ] };
            
            emit(--a);
            emit(a);
            
            emit(--b[1]);
            emit(b[1]);
            
            emit(--c.value);
            emit(c.value);
            
            emit(--c.arr[0]);
            emit(--c["arr"][0]);
            emit(c.arr[0]);
            
            emit(--c.arr[1].subVal);
            emit(--c["arr"][1].subVal);
            emit(c["arr"][1].subVal);
        """.trimIndent())

        val expect = listOf(
            4.toSkript(),
            4.toSkript(),
            1.toSkript(),
            1.toSkript(),
            7.toSkript(),
            7.toSkript(),
            11.toSkript(),
            10.toSkript(),
            10.toSkript(),
            19.toSkript(),
            18.toSkript(),
            18.toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }

    @Test
    fun testPostIncrementBasics() = runBlocking {
        val outputs = runScriptWithEmit("""
            val a = 5;
            val b = [ 1, 2, 3 ];
            val c = { value: 8, arr: [ 12, { subVal: 20 } ] };
            
            emit(a++);
            emit(a);
            
            emit(b[1]++);
            emit(b[1]);
            
            emit(c.value++);
            emit(c.value);
            
            emit(c.arr[0]++);
            emit(c["arr"][0]++);
            emit(c.arr[0]);
            
            emit(c.arr[1].subVal++);
            emit(c["arr"][1].subVal++);
            emit(c["arr"][1].subVal);
        """.trimIndent())

        val expect = listOf(
            5.toSkript(),
            6.toSkript(),
            2.toSkript(),
            3.toSkript(),
            8.toSkript(),
            9.toSkript(),
            12.toSkript(),
            13.toSkript(),
            14.toSkript(),
            20.toSkript(),
            21.toSkript(),
            22.toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }

    @Test
    fun testPostDecrementBasics() = runBlocking {
        val outputs = runScriptWithEmit("""
            val a = 5;
            val b = [ 1, 2, 3 ];
            val c = { value: 8, arr: [ 12, { subVal: 20 } ] };
            
            emit(a--);
            emit(a);
            
            emit(b[1]--);
            emit(b[1]);
            
            emit(c.value--);
            emit(c.value);
            
            emit(c.arr[0]--);
            emit(c["arr"][0]--);
            emit(c.arr[0]);
            
            emit(c.arr[1].subVal--);
            emit(c["arr"][1].subVal--);
            emit(c["arr"][1].subVal);
        """.trimIndent())

        val expect = listOf(
            5.toSkript(),
            4.toSkript(),
            2.toSkript(),
            1.toSkript(),
            8.toSkript(),
            7.toSkript(),
            12.toSkript(),
            11.toSkript(),
            10.toSkript(),
            20.toSkript(),
            19.toSkript(),
            18.toSkript()
        )

        assertEmittedEquals(expect, outputs)
    }
}