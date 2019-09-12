package skript.util

import skript.illegalArg
import skript.values.*

class SkArguments : SkObject() {
    override val klass: SkClassDef
        get() = SkArgumentsClassDef

    private var state = 0

    private val posArgs = ArrayList<SkValue>()
    private val kwArgs = LinkedHashMap<String, SkValue>()

    private var posReadIndex = 0

    fun addPosArg(arg: SkValue) {
        check(state == 0) { "Can't add parameters after some have already been extracted" }
        posArgs.add(arg)
    }

    fun addKwArg(key: String, arg: SkValue) {
        check(state == 0) { "Can't add parameters after some have already been extracted" }
        kwArgs[key] = arg
    }

    fun spreadPosArgs(args: SkList) {
        check(state == 0) { "Can't add parameters after some have already been extracted" }

        posArgs.addAll(args.listEls)
    }

    fun spreadPosArgs(args: List<SkValue>) {
        check(state == 0) { "Can't add parameters after some have already been extracted" }

        posArgs.addAll(args)
    }

    fun spreadKwArgs(args: SkMap) {
        check(state == 0) { "Can't add parameters after some have already been extracted" }

        kwArgs.putAll(args.elements)
    }

    fun spreadKwArgs(args: Map<String, SkValue>) {
        check(state == 0) { "Can't add parameters after some have already been extracted" }

        kwArgs.putAll(args)
    }

    fun getParam(name: String): SkValue {
        check(state <= 1) { "Can't call getParam after remainders have been extracted" }
        state = 1

        kwArgs.remove(name)?.let { return it }

        if (posReadIndex < posArgs.size) {
            return posArgs[posReadIndex++]
        }

        return SkUndefined
    }

    fun getRemainingPosArgs(): List<SkValue> {
        check(state < 2) { "Can't call getRemainingPosArgs() twice, or after getRemainingKwArgs()"}
        state = 2

        return when {
            posReadIndex == 0 ->
                posArgs

            posReadIndex < posArgs.size ->
                posArgs.slice(posReadIndex until posArgs.size)

            else ->
                emptyList()
        }
    }

    fun getRemainingKwArgs(): Map<String, SkValue> {
        check(state < 3) { "getRemainingKwArgs() can only be called once after all other parameters have been extracted" }
        state = 3

        return kwArgs
    }

    fun expectNothingElse() {
        if (state < 3 && kwArgs.isNotEmpty()) illegalArg("Unrecognized keyword parameter(s) - ${kwArgs.keys}")
        if (state < 2 && posReadIndex < posArgs.size) illegalArg("Too many parameters")
        state = 3
    }

    fun noKwArgs(): Boolean {
        return kwArgs.isEmpty()
    }

    companion object {
        fun of(vararg posArgs: SkValue): SkArguments {
            val res = SkArguments()
            res.posArgs.apply {
                posArgs.forEach { add(it) }
            }
            return res
        }

        fun of(posArgs: List<SkValue>): SkArguments {
            val res = SkArguments()
            res.posArgs.addAll(posArgs)
            return res
        }
    }
}

object SkArgumentsClassDef : SkClassDef("Arguments")

fun SkArguments.expectBoolean(name: String, coerce: Boolean = true, ifUndefined: Boolean? = null): Boolean {
    val value = getParam(name)

    if (value == SkUndefined) {
        ifUndefined?.let { return it }
        illegalArg("Expected a boolean value for parameter $name")
    }

    return when {
        coerce -> value.asBoolean().value
        value is SkBoolean -> value.value
        value is SkBooleanObject -> value.value.value
        else -> illegalArg("Expected a boolean value for $name")
    }
}

fun SkArguments.expectString(name: String, coerce: Boolean = true, ifUndefined: String? = null): String {
    val value = getParam(name)

    if (value == SkUndefined) {
        ifUndefined?.let { return it }
        illegalArg("Expected a string value for parameter $name")
    }

    return when {
        coerce -> value.asString().value
        value is SkString -> value.value
        value is SkStringObject -> value.value.value
        else -> illegalArg("Expected a string value for parameter $name")
    }
}

fun SkArguments.expectNumber(name: String, coerce: Boolean = true, ifUndefined: SkNumber? = null): SkNumber {
    val value = getParam(name)

    if (value == SkUndefined) {
        ifUndefined?.let { return it }
        illegalArg("Expected a number value for parameter $name")
    }

    return when {
        coerce -> value.asNumber()
        value is SkNumber -> value
        value is SkNumberObject -> value.value
        else -> illegalArg("Expected a number value for parameter $name")
    }
}

fun SkArguments.expectInt(name: String, coerce: Boolean = true, ifUndefined: Int? = null): Int {
    val value = getParam(name)

    if (value == SkUndefined) {
        ifUndefined?.let { return it }
        illegalArg("Expected an integer value for parameter $name")
    }

    val number = when {
        coerce -> value.asNumber()
        value is SkNumber -> value
        value is SkNumberObject -> value.value
        else -> illegalArg("Expected an integer value for parameter $name")
    }

    return when {
        number.isInt() -> number.value.toInt()
        else -> illegalArg("Expected an integer value for parameter $name")
    }
}

fun SkArguments.expectFunction(name: String): SkFunction {
    val value = getParam(name)

    if (value is SkFunction) {
        return value
    } else {
        illegalArg("Expected a function for parameter $name")
    }
}