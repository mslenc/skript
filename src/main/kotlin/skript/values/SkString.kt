package skript.values

import skript.*
import skript.exec.RuntimeState
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.util.*

class SkString(val value: String) : SkScalar() {
    override fun asObject(): SkObject {
        return SkStringObject(this)
    }

    override suspend fun propGet(key: String, state: RuntimeState): SkValue {
        return when (key) {
            "length" -> SkNumber.valueOf(value.length)
            else -> SkUndefined
        }
    }

    override suspend fun propSet(key: String, value: SkValue, state: RuntimeState) {
        when (key) {
            "length" -> typeError("Can't set string length - strings are immutable")
            else -> typeError("Can't set properties on strings")
        }
    }

    override suspend fun contains(key: SkValue, state: RuntimeState): Boolean {
        val string = when (key) {
            is SkString -> key.value
            is SkNumber -> key.asString().value
            is SkBoolean -> key.asString().value
            is SkNull -> return true
            is SkUndefined -> return false
            else -> return false
        }

        return value.contains(string)
    }

    override suspend fun elementSet(key: SkValue, value: SkValue, state: RuntimeState) {
        typeError("Can't set elements on strings")
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

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
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

    override suspend fun makeIterator(): SkIterator {
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
    override val klass: SkClassDef
        get() = SkStringClassDef

    override fun asBoolean(): SkBoolean {
        return value.asBoolean()
    }

    override fun asNumber(): SkNumber {
        return value.asNumber()
    }

    override fun asString(): SkString {
        return value
    }
}

object SkStringClassDef : SkCustomClass<SkString>("String", SkObjectClassDef) {
    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        val str = args.expectString("value", ifUndefined = "")
        return SkStringObject(SkString(str))
    }

    init {
        defineReadOnlyProperty("length") {
            it.value.length.toSkript()
        }

        defineMethod("trim").
            withStringParam("chars", defaultValue = "").
            withBooleanParam("start", defaultValue = true).
            withBooleanParam("end", defaultValue = true).
            withImpl { str, chars, start, end, _ ->
                val trimmed = if (chars == "") {
                    when {
                        start && end -> str.value.trim()
                        start -> str.value.trimStart()
                        end -> str.value.trimEnd()
                        else -> str.value
                    }
                } else {
                    when {
                        start && end -> str.value.trim(*chars.toCharArray())
                        start -> str.value.trimStart(*chars.toCharArray())
                        end -> str.value.trimEnd(*chars.toCharArray())
                        else -> str.value
                    }
                }

                when (trimmed) {
                    str.value -> str
                    else -> SkString(trimmed)
                }
            }

        defineMethod("substring").
            withOptNumberParam("start").
            withOptNumberParam("end").
            withImpl { string, start, end, _ ->
                val startIdx = start?.let { it.toIntOrNull()?.rangeParam(string.value.length) ?: typeError("Expected an integer value for parameter start") } ?: 0
                val endIdx = end?.let { it.toIntOrNull()?.rangeParam(string.value.length) ?: typeError("Expected an integer value for parameter start") } ?: string.value.length

                when {
                    startIdx == endIdx -> SkString.EMPTY
                    startIdx == 0 && endIdx == string.value.length -> string
                    else -> SkString(string.value.substring(startIdx, endIdx))
                }
            }
    }
}