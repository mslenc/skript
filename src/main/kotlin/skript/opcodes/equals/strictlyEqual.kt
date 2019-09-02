package skript.opcodes.equals

import skript.values.SkNull
import skript.values.SkUndefined
import skript.values.SkValue
import skript.values.SkValueKind

fun strictlyEqual(a: SkValue, b: SkValue): Boolean {
    if (a === b)
        return true

    return when (a.getKind()) {
        SkValueKind.NULL -> b == SkNull
        SkValueKind.UNDEFINED -> b == SkUndefined

        SkValueKind.NUMBER -> if (b.getKind() == SkValueKind.NUMBER) {
            a.asNumber().value.compareTo(b.asNumber().value) == 0
        } else {
            false
        }

        SkValueKind.BOOLEAN -> if (b.getKind() == SkValueKind.BOOLEAN) {
            a.asBoolean().value == b.asBoolean().value
        } else {
            false
        }

        SkValueKind.STRING -> if (b.getKind() == SkValueKind.STRING) {
            a.asString().value == b.asString().value
        } else {
            false
        }

        SkValueKind.LIST,
        SkValueKind.MAP,
        SkValueKind.CLASS,
        SkValueKind.OBJECT,
        SkValueKind.FUNCTION,
        SkValueKind.METHOD -> {
            a === b
        }
    }
}