package skript.util

@Suppress("UNCHECKED_CAST")
class Stack<T> {
    private var top = 16
    private var elements = arrayOfNulls<Any?>(top)

    fun top() = elements[top] as T

    fun top(offset: Int) = elements[top + offset] as T

    fun bottom() = if (isNotEmpty()) elements.last() as T else throw IllegalStateException("stack is empty")

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

    fun containsRef(obj: T): Boolean {
        for (i in top until elements.size)
            if (elements[i] === obj)
                return true

        return false
    }

    inline fun any(cond: (T) -> Boolean): Boolean {
        for (i in 0 until size)
            if (cond(top(i)))
                return true

        return false
    }

    val size get() = elements.size - top
}

fun Stack<*>.isEmpty() = size < 1
fun Stack<*>.isNotEmpty() = size > 0