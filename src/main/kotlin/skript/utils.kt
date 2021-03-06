package skript

import skript.io.SkSyntaxError
import skript.io.SkTypeError
import skript.parser.Pos
import skript.util.Stack
import skript.values.*
import java.math.BigDecimal
import kotlin.math.max
import kotlin.math.min

fun syntaxError(message: String, pos: Pos? = null, cause: Throwable? = null): Nothing {
    throw when (pos) {
        null -> SkSyntaxError(message, cause, pos)
        else -> SkSyntaxError("$pos: $message", cause, pos)
    }
}

fun typeError(message: String, pos: Pos? = null, cause: Throwable? = null): Nothing {
    throw SkTypeError(message, cause, pos)
}

fun String.atMostChars(maxLen: Int): String {
    if (maxLen <= 0)
        return ""

    if (length <= maxLen)
        return this

    return when (maxLen) {
        1 -> "_"
        2 -> "[]"
        3 -> "[.]"
        4 -> "[..]"
        5 -> "[...]"
        else -> {
            val showLen = maxLen - 5
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

fun doubleCompare(a: Double, b: Double): Int {
    // the original makes a distinction between -0.0 and +0.0, which is nice and all, but we don't want it
    if (a == 0.0 && b == 0.0)
        return 0

    return a.compareTo(b)
}