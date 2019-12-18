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
 * * native objects use the native equals() method
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
            SkValueKind.NUMBER -> a.asNumber().compareTo(b.asNumber()) == 0
            SkValueKind.DECIMAL -> a.asNumber().compareTo(b.asNumber()) == 0
            SkValueKind.STRING -> a.asNumber().compareTo(b.asString().asNumberOrNull() ?: return false) == 0
            else -> false
        }

        SkValueKind.DECIMAL -> when (bKind) {
            SkValueKind.NUMBER -> a.asNumber().compareTo(b.asNumber()) == 0
            SkValueKind.DECIMAL -> a.asNumber().compareTo(b.asNumber()) == 0
            SkValueKind.STRING -> a.asNumber().compareTo(b.asString().asNumberOrNull() ?: return false) == 0
            else -> false
        }

        SkValueKind.STRING -> when (bKind) {
            SkValueKind.NUMBER -> (a.asString().asNumberOrNull() ?: return false).compareTo(b.asNumber()) == 0
            SkValueKind.DECIMAL -> (a.asString().asNumberOrNull() ?: return false).compareTo(b.asNumber()) == 0
            SkValueKind.STRING -> a.asString().value == b.asString().value
            else -> false
        }

        SkValueKind.LIST -> when (bKind) {
            SkValueKind.LIST -> deepEquals(a, b, HashSet(), strictElementEqual = false)
            else -> false
        }

        SkValueKind.MAP -> when (bKind) {
            SkValueKind.MAP -> deepEquals(a, b, HashSet(), strictElementEqual = false)
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

fun deepEquals(a: SkValue, b: SkValue, seen: HashSet<RefPair>, strictElementEqual: Boolean): Boolean {
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

        val len = aList.getSize()
        if (bList.getSize() != len)
            return false

        for (i in 0 until len)
            if (!deepEquals(aList.getSlot(i), bList.getSlot(i), seen, strictElementEqual))
                return false

        return true
    }

    if (aKind == SkValueKind.MAP) {
        if (bKind != SkValueKind.MAP)
            return false

        if (!seen.add(RefPair(a, b)))
            return true

        val aMap = a as SkMap
        val bMap = b as SkMap

        if (aMap.entries.size != bMap.entries.size)
            return false

        val aKeys = HashSet<String>()
        val bKeys = HashSet<String>()
        aMap.entries.forEach { (key, _) -> aKeys.add(key) }
        bMap.entries.forEach { (key, _) -> bKeys.add(key) }

        if (aKeys != bKeys)
            return false

        for (key in aKeys)
            if (!deepEquals(aMap.entries[key]!!, bMap.entries[key]!!, seen, strictElementEqual))
                return false

        return true
    }

    if (bKind == SkValueKind.LIST || bKind == SkValueKind.MAP)
        return false

    return if (strictElementEqual) {
        strictlyEqual(a, b)
    } else {
        aboutEqual(a, b)
    }
}