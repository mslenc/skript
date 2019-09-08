package skript.util

import skript.exec.RuntimeState
import skript.illegalArg
import skript.values.*

private val emptyKwMap = LinkedHashMap<String, SkValue>(0)

class ArgsExtractor(val posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, val funcName: String) : SkObject() {
    override val klass: SkClassDef
        get() = ArgsExtractorClassDef

    private var state = 0
    private var posIndex = 0
    private val kwRemain = if (kwArgs.isNotEmpty()) LinkedHashMap(kwArgs) else emptyKwMap

    fun extractParam(name: String): SkValue {
        check(state == 0) { "Can't call extractParam after remainders have been read" }

        kwRemain.remove(name)?.let { return it }

        if (posIndex < posArgs.size) {
            return posArgs[posIndex++]
        }

        return SkUndefined
    }

    fun getRemainingPosArgs(): List<SkValue> {
        check(state == 0) { "Can't call getRemainingPosArgs() twice, or after getRemainingKwArgs()"}
        state = 1

        return when {
            posIndex < posArgs.size ->
                posArgs.slice(posIndex until posArgs.size)
            else ->
                emptyList()
        }
    }

    fun getRemainingKwArgs(): Map<String, SkValue> {
        check(state < 2) { "getRemainingKwArgs() can only be called once after all other parameters have been extracted" }
        state = 2

        return when {
            kwRemain.isNotEmpty() -> kwRemain
            else -> emptyMap()
        }
    }

    fun expectNothingElse() {
        if (state < 2 && kwRemain.isNotEmpty()) illegalArg("Unrecognized keyword parameter(s) when calling $funcName() - ${kwRemain.keys}")
        if (state < 1 && posIndex < posArgs.size) illegalArg("Too many parameters when calling $funcName()")
        state = 2
    }
}

object ArgsExtractorClassDef : SkClassDef("ArgsExtractor", SkObjectClassDef) {
    override suspend fun construct(runtimeClass: SkClass, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkObject {
        throw IllegalStateException("Shouldn't be called")
    }
}

fun ArgsExtractor.expectBoolean(name: String, coerce: Boolean = true, ifUndefined: Boolean? = null): Boolean {
    val value = extractParam(name)

    if (value == SkUndefined) {
        ifUndefined?.let { return it }
        illegalArg("Expected a boolean value for $name when calling $funcName()")
    }

    return when {
        coerce -> value.asBoolean().value
        value is SkBoolean -> value.value
        value is SkBooleanObject -> value.value.value
        else -> illegalArg("Expected a boolean value for $name when calling $funcName()")
    }
}

fun ArgsExtractor.expectString(name: String, coerce: Boolean = true, ifUndefined: String? = null): String {
    val value = extractParam(name)

    if (value == SkUndefined) {
        ifUndefined?.let { return it }
        illegalArg("Expected a string value for $name when calling $funcName()")
    }

    return when {
        coerce -> value.asString().value
        value is SkString -> value.value
        value is SkStringObject -> value.value.value
        else -> illegalArg("Expected a string value for $name when calling $funcName()")
    }
}

fun ArgsExtractor.expectNumber(name: String, coerce: Boolean = true, ifUndefined: SkNumber? = null): SkNumber {
    val value = extractParam(name)

    if (value == SkUndefined) {
        ifUndefined?.let { return it }
        illegalArg("Expected a number value for $name when calling $funcName()")
    }

    return when {
        coerce -> value.asNumber()
        value is SkNumber -> value
        value is SkNumberObject -> value.value
        else -> illegalArg("Expected a number value for $name when calling $funcName()")
    }
}

fun ArgsExtractor.expectInt(name: String, coerce: Boolean = true, ifUndefined: Int? = null): Int {
    val value = extractParam(name)

    if (value == SkUndefined) {
        ifUndefined?.let { return it }
        illegalArg("Expected an integer value for $name when calling $funcName()")
    }

    val number = when {
        coerce -> value.asNumber()
        value is SkNumber -> value
        value is SkNumberObject -> value.value
        else -> illegalArg("Expected an integer value for $name when calling $funcName()")
    }

    return when {
        number.isInt() -> number.value.toInt()
        else -> illegalArg("Expected an integer value for $name when calling $funcName()")
    }
}

fun ArgsExtractor.expectFunction(name: String): SkFunction {
    val value = extractParam(name)

    if (value is SkFunction) {
        return value
    } else {
        illegalArg("Expected a function for $name when calling $funcName()")
    }
}