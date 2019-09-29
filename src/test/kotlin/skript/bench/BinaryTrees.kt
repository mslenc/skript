package skript.bench

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import skript.assertEmittedEquals
import skript.io.toSkript
import skript.runScriptWithEmit
import skript.values.SkValue

class BinaryTrees {
    fun testBGBinaryTrees(n: Int): List<SkValue> = runBlocking {
        val src =
            """
            fun makeTree(depth) {
                if (depth > 0) {
                    return { left: makeTree(depth - 1), right: makeTree(depth - 1) }                
                } else {
                    return { left: null, right: null }
                }
            }
            
            fun itemCheck(tree) {
                if (tree.left) {
                    return itemCheck(tree.left) + itemCheck(tree.right) + 1 
                } else {
                    return 1
                }
            }
            
            val minDepth = 4
            val n = $n
            val maxDepth = n < (minDepth + 2) ? minDepth + 2 : n
            val stretchDepth = maxDepth + 1

            val longLivedTree = makeTree(maxDepth)
            
            emit(`stretch tree of depth ${'$'}{ stretchDepth }\t check: ${'$'}{ itemCheck(makeTree(stretchDepth)) }`)
            
            var d = minDepth
            while (d <= maxDepth) {
                var check = 0
                val iterations = 2 ** (maxDepth - d + minDepth)
                
                for (i in 1..iterations) {
                    check += itemCheck(makeTree(d))
                }
                
                emit(`${'$'}{ iterations }\t trees of depth ${'$'}{ d }\t check: ${'$'}{ check }`)
            
                d += 2
            }
            
            emit(`long lived tree of depth ${'$'}{ maxDepth }\t check: ${'$'}{ itemCheck(longLivedTree) }`)
        """.trimIndent()

        runScriptWithEmit(src)
    }

    @Test
    @Disabled
    fun testBGBinaryTrees21() {
        val result = testBGBinaryTrees(21)

        val expect = listOf(
            "stretch tree of depth 22\t check: 8388607",
            "2097152\t trees of depth 4\t check: 65011712",
            "524288\t trees of depth 6\t check: 66584576",
            "131072\t trees of depth 8\t check: 66977792",
            "32768\t trees of depth 10\t check: 67076096",
            "8192\t trees of depth 12\t check: 67100672",
            "2048\t trees of depth 14\t check: 67106816",
            "512\t trees of depth 16\t check: 67108352",
            "128\t trees of depth 18\t check: 67108736",
            "32\t trees of depth 20\t check: 67108832",
            "long lived tree of depth 21\t check: 4194303"
        ).map { it.toSkript() }

        assertEmittedEquals(expect, result)
    }

    @Test
    fun testBGBinaryTrees14() {
        val result = testBGBinaryTrees(14)

        val expect = listOf(
            "stretch tree of depth 15\t check: 65535",
            "16384\t trees of depth 4\t check: 507904",
            "4096\t trees of depth 6\t check: 520192",
            "1024\t trees of depth 8\t check: 523264",
            "256\t trees of depth 10\t check: 524032",
            "64\t trees of depth 12\t check: 524224",
            "16\t trees of depth 14\t check: 524272",
            "long lived tree of depth 14\t check: 32767"
        ).map { it.toSkript() }

        assertEmittedEquals(expect, result)
    }
}