package skript.io

import skript.util.Stack
import skript.values.*

fun unpack(str: String): SkValue {
    val builders = Stack<UnpackBuilder>()
    builders.push(ValueBuilder())

    var pos = 0
    val strLen = str.length

    while (pos < strLen) {
        val top = builders.top()
        when (val c = str[pos++]) {
            'U' -> top.receive(SkNull)
            'u' -> top.receive(SkUndefined)

            'T' -> top.receive(SkBoolean.TRUE)
            'F' -> top.receive(SkBoolean.FALSE)

            '"' -> top.receive(SkString.EMPTY)

            '0' -> top.receive(SkDouble.ZERO)
            '1' -> top.receive(SkDouble.ONE)
            '2' -> top.receive(SkDouble.TWO)
            '3' -> top.receive(SkDouble.THREE)
            '4' -> top.receive(SkDouble.FOUR)
            '5' -> top.receive(SkDouble.FIVE)
            '6' -> top.receive(SkDouble.SIX)
            '7' -> top.receive(SkDouble.SEVEN)
            '8' -> top.receive(SkDouble.EIGHT)
            '9' -> top.receive(SkDouble.NINE)

            '[' -> builders.push(ListBuilder())
            ']' -> {
                val listBuilder = builders.pop() as? ListBuilder ?: throw IllegalStateException("No list, but end of list encountered")
                builders.top().receive(listBuilder.build())
            }

            '{' -> builders.push(MapBuilder())
            '}' -> {
                val mapBuilder = builders.pop() as? MapBuilder ?: throw IllegalStateException("No map, but end of map encountered")
                builders.top().receive(mapBuilder.build())
            }

            'n' -> {
                val len = str[pos++].toInt() - '0'.toInt()
                val num = str.substring(pos, pos + len).toDouble()
                pos += len
                top.receive(SkDouble.valueOf(num))
            }

            'N' -> {
                val lenLen = str[pos++].toInt() - '0'.toInt()
                val len = str.substring(pos, pos + lenLen).toInt()
                pos += lenLen
                val num = str.substring(pos, pos + len).toDouble()
                pos += len
                top.receive(SkDouble.valueOf(num))
            }

            'd' -> {
                val len = str[pos++].toInt() - '0'.toInt()
                val num = str.substring(pos, pos + len).toBigDecimal()
                pos += len
                top.receive(SkDecimal.valueOf(num))
            }

            'D' -> {
                val lenLen = str[pos++].toInt() - '0'.toInt()
                val len = str.substring(pos, pos + lenLen).toInt()
                pos += lenLen
                val num = str.substring(pos, pos + len).toBigDecimal()
                pos += len
                top.receive(SkDecimal.valueOf(num))
            }

            's' -> {
                val len = str[pos++].toInt() - '0'.toInt()
                val string = str.substring(pos, pos + len)
                pos += len
                top.receive(SkString(string))
            }

            'S' -> {
                val lenLen = str[pos++].toInt() - '0'.toInt()
                val len = str.substring(pos, pos + lenLen).toInt()
                pos += lenLen
                val string = str.substring(pos, pos + len)
                pos += len
                top.receive(SkString(string))
            }

            'r' -> {
                val reps = str[pos++].toInt() - '0'.toInt()
                top.repeatLast(reps)
            }

            'R' -> {
                val repsLen = str[pos++].toInt() - '0'.toInt()
                val reps = str.substring(pos, pos + repsLen).toInt()
                pos += repsLen
                top.repeatLast(reps)
            }

            else -> {
                throw IllegalStateException("Unexpected character $c encountered")
            }
        }
    }

    if (builders.size == 1) {
        return builders.pop().build()
    } else {
        throw IllegalStateException("Invalid input - builders stack too big")
    }
}

internal sealed class UnpackBuilder {
    abstract fun receive(value: SkValue)
    abstract fun repeatLast(reps: Int)
    abstract fun build(): SkValue
}

internal class ValueBuilder : UnpackBuilder() {
    lateinit var result: SkValue

    override fun receive(value: SkValue) {
        check(!::result.isInitialized) { "Multiple values outside of a container" }
        result = value
    }

    override fun repeatLast(reps: Int) {
        check(false) { "Repetition outside a list" }
    }

    override fun build(): SkValue {
        check(::result.isInitialized) { "Missing a value" }
        return result
    }
}

internal class ListBuilder : UnpackBuilder() {
    val result = SkList()

    override fun receive(value: SkValue) {
        result.add(value)
    }

    override fun repeatLast(reps: Int) {
        val last = result.listEls.lastOrNull() ?: throw IllegalStateException("Repetition before any values")
        repeat(reps) {
            result.add(duplicate(last))
        }
    }

    override fun build(): SkValue {
        return result
    }
}

private fun duplicate(value: SkValue): SkValue {
    return when (value) {
        is SkList -> {
            SkList().apply {
                value.listEls.forEach { listEls.add(duplicate(it)) }
            }
        }
        is SkMap -> {
            SkMap().apply {
                value.entries.forEach { (k, v) -> entries[k] = duplicate(v) }
            }
        }
        else -> {
            value
        }
    }
}

internal class MapBuilder : UnpackBuilder() {
    val result = SkMap()
    var key: String? = null

    override fun receive(value: SkValue) {
        key?.let {
            result.entries[it] = value
            key = null
            return
        }

        if (value is SkString) {
            key = value.value
            return
        }

        throw IllegalStateException("Expected a string key, but got ${ value.getKind() } instead")
    }

    override fun repeatLast(reps: Int) {
        throw IllegalStateException("Repetition within a map")
    }

    override fun build(): SkValue {
        if (key == null)
            return result

        throw IllegalStateException("Missing value after key")
    }
}
