package skript.opcodes.compare

import skript.opcodes.equals.RefPair
import skript.opcodes.equals.aboutEqual
import skript.toStrictNumberOrNull
import skript.values.*
import kotlin.math.min
import kotlin.math.sign

/**
 * Compares two values. We will take a hint from aboutEqual and not do too many implicit conversions, so the result
 * is undefined in most cross-type cases.
 *
 * Determines whether two values are "equal for most intents and purposes". We will avoid a large mess by not doing
 * too many implicit conversions, and will . Here are the rules:
 *
 * * objects representing primitives (the Number, String, Boolean types) get unboxed
 * * false < true; booleans otherwise don't compare with anything
 * * null <=> null == 0, but otherwise comparison with null is undefined
 * * undefined <=> anything == undefined (including undefined <=> undefined)
 * * strings that parse as numbers can compare with numbers as if they were numbers
 * * other strings only compare with strings, else the result is undefined
 * * lists compare element-wise; if any element comparison is undefined, so is the overall result
 * * for everything else (maps, objects, ...), (a <=> b) == (a == b ? 0 else undefined)
 * * TODO: objects of the same class will eventually be comparable by calling their compareTo
 */
fun compare(aObj: SkValue, bObj: SkValue): Int? {
    val a = if (aObj is SkScalarObject) aObj.value else aObj
    val b = if (bObj is SkScalarObject) bObj.value else bObj

    if (a == SkUndefined || b == SkUndefined)
        return null

    if (a === b)
        return 0

    val aKind = a.getKind()
    val bKind = b.getKind()

    if (aKind == SkValueKind.NUMBER || bKind == SkValueKind.NUMBER || aKind == SkValueKind.DECIMAL || bKind == SkValueKind.DECIMAL) {
        val aNum = a.toStrictNumberOrNull() ?: return null
        val bNum = b.toStrictNumberOrNull() ?: return null

        return aNum.compareTo(bNum)
    }

    if (aKind != bKind)
        return null

    return when (aKind) {
        SkValueKind.STRING -> a.asString().value.compareTo(b.asString().value).sign

        SkValueKind.BOOLEAN -> {
            val aVal = if (a.asBoolean().value) 1 else 0
            val bVal = if (b.asBoolean().value) 1 else 0
            aVal.compareTo(bVal)
        }

        SkValueKind.LIST -> deepCompare(a as SkList, b as SkList, HashSet())

        else -> if (aboutEqual(a, b)) 0 else null
    }
}

fun deepCompare(a: SkList, b: SkList, seen: HashSet<RefPair>): Int? {
    // (seen serves to detect cyclic structures)

    if (!seen.add(RefPair(a, b)))
        return 0 // TODO: does this make sense?

    for (i in 0 until min(a.getSize(), b.getSize())) {
        val elA = a.getSlot(i)
        val elB = b.getSlot(i)

        val cmp = when {
            elA is SkList && elB is SkList -> deepCompare(elA, elB, seen) ?: return null
            elA is SkList -> return null
            elB is SkList -> return null
            else -> compare(elA, elB) ?: return null
        }

        if (cmp != 0)
            return cmp
    }

    return a.getSize().compareTo(b.getSize())
}