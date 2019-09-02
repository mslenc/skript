package skript.util

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class StackTest {
    @Test
    fun testAddingAndPeeking() {
        val upToSize = 200

        val elements = DoubleArray(upToSize) { Math.random() }

        val stack = Stack<Double>()

        for (i in 0 until upToSize) {
            stack.push(elements[i])

            assertEquals(elements[i], stack.top())

            for (offset in 0..i) {
                assertEquals(elements[i - offset], stack.top(offset))
            }
        }
    }

    @Test
    fun testLongSequence() {
        val stack = Stack<Double>()
        val cmp = java.util.Stack<Double>()

        repeat(1000) {
            val size = cmp.size

            if (size > 0) {
                assertEquals(cmp.peek(), stack.top())
            }

            for (offset in 0 until size) {
                assertEquals(cmp[size - 1 - offset], stack.top(offset))
            }

            if (size < 1 || Math.random() > 0.5) {
                val num = Math.random()
                stack.push(num)
                cmp.push(num)
            } else {
                stack.pop()
                cmp.pop()
            }
        }
    }
}