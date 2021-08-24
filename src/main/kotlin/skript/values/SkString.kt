package skript.values

import skript.*
import skript.interop.HoldsNative
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.util.*
import java.util.*

class SkString(val value: String) : SkScalar() {
    override fun asObject(): SkObject {
        return SkStringObject(this)
    }

    override suspend fun propertyGet(key: String, env: SkriptEnv): SkValue {
        return when (key) {
            "length" -> SkNumber.valueOf(value.length)
            else -> SkUndefined
        }
    }

    override suspend fun propertySet(key: String, value: SkValue, env: SkriptEnv) {
        when (key) {
            "length" -> typeError("Can't set string length - strings are immutable")
            else -> typeError("Can't set properties on strings")
        }
    }

    override suspend fun contains(key: SkValue, env: SkriptEnv): Boolean {
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

    override suspend fun entrySet(key: SkValue, value: SkValue, env: SkriptEnv) {
        typeError("Can't set elements on strings")
    }

    override fun getKind(): SkValueKind {
        return SkValueKind.STRING
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(value.isNotEmpty())
    }

    override fun asNumber(): SkDecimal {
        return asNumberOrNull() ?: typeError("Couldn't parse string (${value.atMostChars(20)}) as a number")
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, env: SkriptEnv, exprDebug: String): SkValue {
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

    override fun equals(other: Any?): Boolean {
        return when {
            other == null -> false
            other === this -> true
            other is SkString -> value == other.value
            else -> false
        }
    }

    override fun hashCode(): Int {
        return value.hashCode()
    }

    override fun unwrap(): String {
        return value
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

    override fun unwrap(): String {
        return value.unwrap()
    }
}

object SkStringClassDef : SkCustomClass<SkStringObject>("String", SkObjectClassDef) {
    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        val str = args.expectString("value", ifUndefined = "")
        args.expectNothingElse()
        return SkStringObject(SkString(str))
    }

    init {
        defineReadOnlyProperty("length") {
            it.value.value.length.toSkript()
        }

        defineMethod("trim").
            withStringParam("chars", defaultValue = "").
            withBooleanParam("start", defaultValue = true).
            withBooleanParam("end", defaultValue = true).
            withImpl { str, chars, start, end, _ ->
                val trimmed = if (chars == "") {
                    when {
                        start && end -> str.value.value.trim()
                        start -> str.value.value.trimStart()
                        end -> str.value.value.trimEnd()
                        else -> str.value.value
                    }
                } else {
                    when {
                        start && end -> str.value.value.trim(*chars.toCharArray())
                        start -> str.value.value.trimStart(*chars.toCharArray())
                        end -> str.value.value.trimEnd(*chars.toCharArray())
                        else -> str.value.value
                    }
                }

                when (trimmed) {
                    str.value.value -> str
                    else -> SkString(trimmed)
                }
            }

        defineMethod("substring"). // TODO: accept ranges as well
            withOptNumberParam("start").
            withOptNumberParam("end").
            withImpl { string, start, end, _ ->
                val startIdx = start?.let { it.toIntOrNull()?.rangeParam(string.value.value.length) ?: typeError("Expected an integer value for parameter start") } ?: 0
                val endIdx = end?.let { it.toIntOrNull()?.rangeParam(string.value.value.length) ?: typeError("Expected an integer value for parameter start") } ?: string.value.value.length

                when {
                    startIdx == endIdx -> SkString.EMPTY
                    startIdx == 0 && endIdx == string.value.value.length -> string
                    else -> SkString(string.value.value.substring(startIdx, endIdx))
                }
            }

        defineMethod("toUpperCase").
            withParam("locale").
            withImpl { string, locale, _ ->
                string.value.value.uppercase(resolveLocale(locale)).toSkript()
            }

        defineMethod("toLowerCase").
            withParam("locale").
            withImpl { string, locale, _ ->
                string.value.value.lowercase(resolveLocale(locale)).toSkript()
            }
    }
}



fun resolveLocale(localeParam: SkValue): Locale {
    return when {
        localeParam == SkNull || localeParam == SkUndefined ->
            Locale.getDefault()

        localeParam is HoldsNative<*> -> {
            localeParam.nativeObj as? Locale ?: typeError("${localeParam.nativeObj} is not a Locale object")
        }

        else -> {
            val localeName = localeParam.asString().value
            if (localeName == "")
                return Locale.getDefault()

            Locale.forLanguageTag(localeName).also {
                if (it.language.isEmpty())
                    typeError("Couldn't parse $localeName as a locale language tag")
            }
        }
    }
}