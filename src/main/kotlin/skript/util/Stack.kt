package skript.util

@Suppress("UNCHECKED_CAST")
class Stack<T> {
    private var top = 16
    private var elements = arrayOfNulls<Any?>(top)

    fun top() = elements[top] as T

    fun top(offset: Int) = elements[top + offset] as T

    fun push(element: T) {
        if (top < 1) {
            grow()
        }

        elements[--top] = element
    }

    fun pop(): T {
        return elements[top++] as T
    }

    private fun grow() {
        val oldSize = elements.size
        val newSize = 2 * oldSize
        check(newSize >= oldSize) { "Stack too big" }

        val newElements = arrayOfNulls<Any?>(newSize)
        System.arraycopy(elements, 0, newElements, oldSize, oldSize)

        elements = newElements
        top += oldSize
    }

    val size get() = elements.size - top
}

fun Stack<*>.isEmpty() = size < 1
fun Stack<*>.isNotEmpty() = size > 0