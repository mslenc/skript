package skript.values

import skript.*
import skript.exec.RuntimeState
import skript.util.ArgsExtractor
import skript.util.expectBoolean
import skript.util.expectInt
import skript.util.expectString
import java.lang.UnsupportedOperationException

class SkString(val value: String) : SkScalar() {
    override fun asObject(): SkObject {
        return SkStringObject(this)
    }

    override suspend fun findMember(key: SkValue): SkValue {
        if (key.isString("length"))
            return SkNumber.valueOf(value.length)

        return super.findMember(key)
    }

    override suspend fun hasOwnMember(key: SkValue): Boolean {
        if (key.isString("length"))
            return true

        return super.hasOwnMember(key)
    }

    override suspend fun setMember(key: SkValue, value: SkValue) {
        if (key.isString("length"))
            throw UnsupportedOperationException("Can't set string length - strings are immutable")

        return super.setMember(key, value)
    }

    override suspend fun deleteMember(key: SkValue) {
        if (key.isString("length"))
            throw UnsupportedOperationException("Can't delete string length - strings are immutable")

        return super.deleteMember(key)
    }

    override fun getKind(): SkValueKind {
        return SkValueKind.STRING
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(value.isNotEmpty())
    }

    override fun asNumber(): SkDecimal {
        return asNumberOrNull() ?: throw IllegalStateException("Couldn't parse string (${value.atMostChars(20)}) as a number")
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState): SkValue {
        val endStr = end.asString()

        return SkStringRange(this.value, endStr.value, endInclusive)
    }

    fun asNumberOrNull(): SkDecimal? {
        value.toBigDecimalOrNull()?.let { return SkDecimal.valueOf(it) }

        return null
    }

    override fun asString(): SkString {
        return this
    }

    override suspend fun makeIterator(): SkValue {
        return SkStringIterator(this)
    }

    override fun toString(sb: StringBuilder) {
        sb.append('"').append(value).append('"')
    }

    companion object {
        val EMPTY = SkString("")
        val NULL = SkString("null")
        val UNDEFINED = SkString("undefined")
        val TRUE = SkString("true")
        val FALSE = SkString("false")
        val MAP = SkString("{Map}")
    }
}

class SkStringObject(override val value: SkString) : SkScalarObject() {
    override val klass: SkClass
        get() = StringClass

    override fun asBoolean(): SkBoolean {
        return value.asBoolean()
    }

    override fun asNumber(): SkNumber {
        return value.asNumber()
    }

    override fun asString(): SkString {
        return value
    }

    override suspend fun findMember(key: SkValue): SkValue {
        if (key.isString("length"))
            return SkNumber.valueOf(value.value.length)

        return super.findMember(key)
    }

    override suspend fun hasOwnMember(key: SkValue): Boolean {
        if (key.isString("length"))
            return true

        return super.hasOwnMember(key)
    }

    override suspend fun setMember(key: SkValue, value: SkValue) {
        if (key.isString("length"))
            throw UnsupportedOperationException("Can't set string length - strings are immutable")

        return super.setMember(key, value)
    }

    override suspend fun deleteMember(key: SkValue) {
        if (key.isString("length"))
            throw UnsupportedOperationException("Can't delete string length - strings are immutable")

        return super.deleteMember(key)
    }
}

object StringClass : SkClass("String", ObjectClass) {
    init {
        defineInstanceMethod(String_trim)
        defineInstanceMethod(String_substring)
    }

    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        val valArg = kwArgs["value"] ?: posArgs.getOrNull(0) ?: SkString.EMPTY
        return SkStringObject(valArg.asString())
    }
}

object String_trim : SkMethod("trim", listOf("chars", "start", "end")) {
    override val expectedClass: SkClass
        get() = StringClass

    override suspend fun call(thiz: SkValue, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkString {
        val args = ArgsExtractor(posArgs, kwArgs, "trim")

        val chars = args.expectString("chars", ifUndefined = "")
        val trimStart = args.expectBoolean("start", ifUndefined = true)
        val trimEnd = args.expectBoolean("end", ifUndefined = true)

        val str = thiz.asString()

        val trimmed = if (chars == "") {
            when {
                trimStart && trimEnd -> str.value.trim()
                trimStart -> str.value.trimStart()
                trimEnd -> str.value.trimEnd()
                else -> str.value
            }
        } else {
            when {
                trimStart && trimEnd -> str.value.trim(*chars.toCharArray())
                trimStart -> str.value.trimStart(*chars.toCharArray())
                trimEnd -> str.value.trimEnd(*chars.toCharArray())
                else -> str.value
            }
        }

        return when (trimmed) {
            str.value -> str
            else -> SkString(trimmed)
        }
    }
}

object String_substring : SkMethod("substring", listOf("start", "end")) {
    override val expectedClass: SkClass
        get() = StringClass

    override suspend fun call(thiz: SkValue, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkString {
        val strVal = thiz.asString()
        val string = strVal.value

        val args = ArgsExtractor(posArgs, kwArgs, "substring")
        val startArg = args.expectInt("start", ifUndefined = 0)
        val endArg = args.expectInt("end", ifUndefined = string.length)
        args.expectNothingElse()

        val start = startArg.rangeParam(string.length)
        val end = endArg.rangeParam(string.length)

        return when {
            start == end -> SkString.EMPTY
            start == 0 && end == string.length -> strVal
            else -> SkString(string.substring(start, end))
        }
    }
}