package skript.io

import skript.util.Stack
import skript.values.*
import skript.withTop

fun pack(value: SkValue): String {
    val sb = StringBuilder()
    val currentContainers = Stack<SkValue>()
    pack(value, sb, currentContainers)
    return sb.toString()
}

internal fun pack(value: SkValue, out: StringBuilder, currentContainers: Stack<SkValue>) {
    when (value.getKind()) {
        SkValueKind.NULL -> out.append('U')
        SkValueKind.UNDEFINED -> out.append('u')
        SkValueKind.NUMBER -> {
            packDouble(value.asNumber().toDouble(), out)
        }
        SkValueKind.DECIMAL -> {
            val str = value.asNumber().toBigDecimal().toString()
            when {
                str.length > 9 -> out.append('D').append(str.length.toString().length).append(str.length).append(str)
                else -> out.append('d').append(str.length).append(str)
            }
        }
        SkValueKind.BOOLEAN -> {
            if (value.asBoolean().value) {
                out.append('T')
            } else {
                out.append('F')
            }
        }
        SkValueKind.STRING -> {
            packString(value.asString().value, out)
        }
        SkValueKind.LIST -> {
            packList(value as SkAbstractList, out, currentContainers)
        }
        SkValueKind.MAP -> {
            packMap(value as SkMap, out, currentContainers)
        }

        else -> throw UnsupportedOperationException("Can't pack ${value.getKind()} $value")
    }
}

internal fun packDouble(d: Double, out: StringBuilder) {
    var str = d.toString()
    if (str.endsWith(".0"))
        str = str.substring(0, str.length - 2)

    when {
        str.length == 1 -> out.append(str)
        str.length > 9 -> out.append('N').append(str.length.toString().length).append(str.length).append(str)
        else -> out.append('n').append(str.length).append(str)
    }
}

internal fun packString(str: String, out: StringBuilder) {
    when {
        str == "" -> out.append('"')
        str.length > 9 -> out.append('S').append(str.length.toString().length).append(str.length).append(str)
        else -> out.append('s').append(str.length).append(str)
    }
}

internal fun packList(list: SkAbstractList, out: StringBuilder, currentContainers: Stack<SkValue>) {
    check(!currentContainers.containsRef(list))

    currentContainers.withTop(list) {
        when (val size = list.getSize()) {
            0 -> out.append("[]")
            1 -> {
                out.append('[')
                pack(list.getSlot(0), out, currentContainers)
                out.append(']')
            }
            else -> {
                out.append('[')
                var repStart = out.length
                pack(list.getSlot(0), out, currentContainers)
                var repEnd = out.length
                var numReps = 0
                var pos = 1
                while (pos < size) {
                    pack(list.getSlot(pos++), out, currentContainers)
                    val currEnd = out.length
                    if (currEnd - repEnd == repEnd - repStart && out.regionMatches(repStart, out, repEnd, repEnd - repStart, ignoreCase = false)) {
                        numReps++
                        out.setLength(repEnd)
                    } else
                    if (numReps > 0) {
                        val encodedReps = packReps(numReps)
                        val toInsert = if (encodedReps.length < numReps * (repEnd - repStart)) {
                            encodedReps
                        } else {
                            out.substring(repStart, repEnd).repeat(numReps)
                        }

                        out.insert(repEnd, toInsert)
                        repStart = repEnd + toInsert.length
                        repEnd = currEnd + toInsert.length
                        numReps = 0
                    } else {
                        repStart = repEnd
                        repEnd = currEnd
                    }
                }
                out.append(packReps(numReps))
                out.append(']')
            }
        }
    }
}

internal fun packMap(map: SkMap, out: StringBuilder, currentContainers: Stack<SkValue>) {
    check(!currentContainers.containsRef(map)) { "Can't pack cyclic structures" }

    currentContainers.withTop(map) {
        out.append('{')
        map.entries.forEach { (key, value) ->
            packString(key, out)
            pack(value, out, currentContainers)
        }
        out.append('}')
    }
}

internal fun packReps(numReps: Int): String {
    return when {
        numReps > 9 -> "R${numReps.toString().length}$numReps"
        numReps > 0 -> "r$numReps"
        else -> ""
    }
}