package skript.endtoend

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import skript.runScriptWithEmit
import java.math.BigDecimal
import kotlin.math.min

@Suppress("unused")
class ArgsTestObj(private val out: MutableList<String>) {
    fun getBytes(): ByteArray = byteArrayOf(2, 5, 8);
    fun outBytes(bytes: ByteArray) {
        out += "bytes: " + bytes.contentToString()
    }

    fun getInts(): IntArray = intArrayOf(2, 4, 3, 5, 1)
    fun outInts(ints: IntArray) {
        out += "ints: " + ints.contentToString()
    }

    fun outVarargs(a: Int, vararg c: String, b: Boolean = true, d: BigDecimal?) {
        out += "varargs: $a $b ${c.contentToString()} $d"
    }
}

class NativeArgsTest {
    @Test
    fun testSomeConversion() = runBlocking {
        val result = ArrayList<String>()

        runScriptWithEmit({ env ->
            env.setNativeGlobal("test", ArgsTestObj(result))
        }, """
            test.outBytes(test.getBytes());
            test.outBytes([ 4, 3, 2, 1 ]);
            
            test.outInts(test.getInts());
            test.outInts([ 6, 5, 4, 3 ]);
            
            test.outVarargs(12, "abc", "def", "ghi", d = 3.5);
            test.outVarargs(13);
            test.outVarargs(14, b = false);
        """.trimIndent())

        val expect = listOf(
            "bytes: [2, 5, 8]",
            "bytes: [4, 3, 2, 1]",
            "ints: [2, 4, 3, 5, 1]",
            "ints: [6, 5, 4, 3]",
            "varargs: 12 true [abc, def, ghi] 3.5",
            "varargs: 13 true [] null",
            "varargs: 14 false [] null"
        )

        for (i in 0 until min(expect.size, result.size)) {
            assertEquals(expect[i], result[i]) { "Line $i" }
        }
        assertEquals(expect.size, result.size)
    }
}