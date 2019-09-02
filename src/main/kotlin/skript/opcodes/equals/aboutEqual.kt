package skript.opcodes.equals

import skript.values.*

/**
 * Determines whether two values are "equal for most intents and purposes". We will avoid a large mess by not doing
 * too many implicit conversions. Here are the rules:
 *
 * * objects representing primitives (the Number, String, Boolean types) get unboxed
 * * null == undefined, but they are not equal to anything else
 * * true and false are only equal to themselves
 * * strings that parse as numbers are equal to those numbers
 * * lists are equal if they have the same elements (as in, about equal, recursively)
 * * maps are equal if they have the same members (as in, about equal, recursively)
 * * other objects, functions, classes and methods only equal themselves
 */
fun aboutEqual(aObj: SkValue, bObj: SkValue): Boolean {
    val a = if (aObj is SkScalarObject) aObj.value else aObj
    val b = if (bObj is SkScalarObject) bObj.value else bObj

    if (a === b)
        return true

    val aKind = a.getKind()
    val bKind = b.getKind()

    // number boolean string null undefined
    // list map
    // class object function method

    // then we'll go quite restrictively
    // null == undefined
    // 123 == "123"  (number == string.toNumber() ?: return false)
    // [ 1, 2, 3 ] == [ 1, 2, 3 ]
    // { ... } == { ... }

    return when (aKind) {
        SkValueKind.NULL,
        SkValueKind.UNDEFINED -> b == SkNull || b == SkUndefined

        SkValueKind.BOOLEAN -> when (bKind) {
            SkValueKind.BOOLEAN -> a.asBoolean().value == b.asBoolean().value
            else -> false
        }

        SkValueKind.NUMBER -> when (bKind) {
            SkValueKind.NUMBER -> a.asNumber().value.compareTo(b.asNumber().value) == 0
            SkValueKind.STRING -> (b.asString().asNumberOrNull() ?: return false).value.compareTo(a.asNumber().value) == 0
            else -> false
        }

        SkValueKind.STRING -> when (bKind) {
            SkValueKind.STRING -> a.asString().value == b.asString().value
            SkValueKind.NUMBER -> (a.asString().asNumberOrNull() ?: return false).value.compareTo(b.asNumber().value) == 0
            else -> false
        }

        SkValueKind.LIST -> when (bKind) {
            SkValueKind.LIST -> deepEquals(a, b, HashSet())
            else -> false
        }

        SkValueKind.MAP -> when (bKind) {
            SkValueKind.MAP -> deepEquals(a, b, HashSet())
            else -> false
        }

        SkValueKind.CLASS,
        SkValueKind.OBJECT,
        SkValueKind.FUNCTION,
        SkValueKind.METHOD -> {
            a == b
        }
    }
}

class RefPair(val a: SkValue, val b: SkValue) {
    override fun equals(other: Any?): Boolean {
        if (other !is RefPair) return false

        return a === other.a && b === other.b
    }

    override fun hashCode(): Int {
        return System.identityHashCode(a) * 31 + System.identityHashCode(b)
    }
}

fun deepEquals(a: SkValue, b: SkValue, seen: HashSet<RefPair>): Boolean {
    // (seen serves to detect cyclic structures)

    val aKind = a.getKind()
    val bKind = b.getKind()

    if (aKind == SkValueKind.LIST) {
        if (bKind != SkValueKind.LIST)
            return false

        if (!seen.add(RefPair(a, b)))
            return true // we'll get it right in the original call?

        val aList = a as SkList
        val bList = b as SkList

        val len = aList.getLength()
        if (bList.getLength() != len)
            return false

        for (i in 0 until len)
            if (!deepEquals(aList.getSlot(i), bList.getSlot(i), seen))
                return false

        return true
    }

    if (aKind == SkValueKind.MAP) {
        if (bKind != SkValueKind.MAP)
            return false;

        if (!seen.add(RefPair(a, b)))
            return true

        val aMap = a as SkMap
        val bMap = b as SkMap

        if (aMap.props.size != bMap.props.size)
            return false

        val aKeys = HashSet<String>()
        val bKeys = HashSet<String>()
        aMap.props.forEach { key, _ -> aKeys.add(key) }
        bMap.props.forEach { key, _ -> bKeys.add(key) }

        if (aKeys != bKeys)
            return false

        for (key in aKeys)
            if (!deepEquals(aMap.getMapMember(key)!!, bMap.getMapMember(key)!!, seen))
                return false

        return true
    }

    if (bKind == SkValueKind.LIST || bKind == SkValueKind.MAP)
        return false

    return aboutEqual(a, b)
}