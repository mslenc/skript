package skript

import skript.lexer.Pos
import skript.util.Stack
import skript.values.*
import java.math.BigDecimal
import kotlin.math.max
import kotlin.math.min

fun syntaxError(message: String, pos: Pos): Nothing {
    throw IllegalArgumentException("$message @ $pos") // TODO - make an exception and/or support exceptions; also show pos
}

fun typeError(message: String, pos: Pos? = null): Nothing {
    throw IllegalStateException("Type error - $message") // TODO - make an exception and/or support exceptions; also show pos
}

fun notSupported(message: String = "Not supported"): Nothing {
    throw UnsupportedOperationException(message)
}

fun illegalArg(message: String = "Illegal argument"): Nothing {
    throw IllegalArgumentException(message)
}

fun String.atMostChars(maxLen: Int): String {
    if (maxLen <= 0)
        return ""

    return when (length) {
        1 -> "_"
        2 -> "[]"
        3 -> "[.]"
        4 -> "[..]"
        5 -> "[...]"
        else -> {
            val showLen = length - 5
            val onRight = showLen / 2
            val onLeft = showLen - onRight
            substring(0, onLeft) + "[...]" + substring(length - onRight, length)
        }
    }
}

fun BigDecimal.isInteger(): Boolean {
    return signum() == 0 || scale() <= 0 || stripTrailingZeros().scale() <= 0
}

fun SkValue.isString(str: String): Boolean {
    return getKind() == SkValueKind.STRING && asString().value == str
}

fun Int.rangeParam(rangeLength: Int): Int {
    assert(rangeLength >= 0)

    return when {
        this < 0 -> max(0, this + rangeLength)
        else -> min(rangeLength, this)
    }
}

fun SkValue.toStrictNumberOrNull(): SkNumber? {
    return when (this) {
        is SkNumber -> return this
        is SkNumberObject -> return this.value
        is SkString -> this.asNumberOrNull()
        is SkStringObject -> this.value.asNumberOrNull()
        else -> return null
    }
}

inline fun <T> Stack<T>.withTop(value: T, block: ()->Unit) {
    push(value)
    try {
        block()
    } finally {
        pop()
    }
}