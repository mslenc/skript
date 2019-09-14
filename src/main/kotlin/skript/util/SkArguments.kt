package skript.util

import skript.illegalArg
import skript.syntaxError
import skript.values.*

class SkArguments : SkObject() {
    override val klass: SkClassDef
        get() = SkArgumentsClassDef

    private var state = 0 // 0 -> initial, parameters are being added
                          // 1 -> after regular args; all reading is allowed
                          // 2 -> after *posArgs; kwOnly and **kwArgs are allowed
                          // 3 -> after **kwArgs; nothing else is allowed

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

    fun spreadPosArgs(args: SkAbstractList) {
        check(state == 0) { "Can't add parameters after some have already been extracted" }

        if (args is SkList) {
            posArgs.addAll(args.listEls)
        } else {
            for (i in 0 until args.getSize())
                posArgs.add(args.getSlot(i))
        }
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

    fun extractArg(name: String, kwOnly: Boolean): SkValue = when {
        kwOnly -> extractKwOnlyArg(name)
        else -> extractArg(name)
    }



    fun extractArg(name: String): SkValue {
        check(state <= 1) { "Can't extract regular arguments after *posArgs or **kwArgs" }
        state = 1

        kwArgs.remove(name)?.let { return it }

        if (posReadIndex < posArgs.size) {
            return posArgs[posReadIndex++]
        }

        return SkUndefined
    }

    fun extractAllPosArgs(): List<SkValue> {
        check(state < 2) { "Can't extract *posArgs anymore"}
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

    fun extractPosVarArgs(name: String): SkList {
        check(state < 2) { "Can't extract *posArgs anymore" }

        if (kwArgs.containsKey(name)) {
            syntaxError("$name is a *posArgs parameter and can't be set directly with $name = values - use spread operator *values instead")
        }

        state = 2

        return when {
            posReadIndex == 0 ->
                SkList(posArgs)

            posReadIndex < posArgs.size ->
                SkList(posArgs.slice(posReadIndex until posArgs.size)) // TODO: don't copy twice

            else ->
                SkList()
        }
    }

    fun extractKwOnlyArg(name: String): SkValue {
        check(state < 3) { "Can't extract arguments anymore" }

        kwArgs.remove(name)?.let { return it }

        return SkUndefined
    }

    fun extractAllKwArgs(): Map<String, SkValue> {
        check(state < 3) { "Can't extract arguments anymore" }
        state = 3

        return kwArgs
    }

    fun extractKwVarArgs(name: String): SkMap {
        check(state < 3) { "Can't extract arguments anymore" }

        if (kwArgs.containsKey(name)) {
            syntaxError("$name is a **kwArgs parameter and can't be set directly with $name = values - use spread operator **values instead")
        }

        state = 3

        return SkMap(kwArgs)
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

fun SkArguments.expectBoolean(name: String, coerce: Boolean = true, ifUndefined: Boolean? = null, kwOnly: Boolean = false): Boolean {
    val value = extractArg(name, kwOnly)

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

fun SkArguments.expectString(name: String, coerce: Boolean = true, ifUndefined: String? = null, kwOnly: Boolean = false): String {
    val value = extractArg(name, kwOnly)

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

fun SkArguments.expectNumber(name: String, coerce: Boolean = true, ifUndefined: SkNumber? = null, kwOnly: Boolean = false): SkNumber {
    val value = extractArg(name, kwOnly)

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

fun SkArguments.expectInt(name: String, coerce: Boolean = true, ifUndefined: Int? = null, kwOnly: Boolean = false): Int {
    val value = extractArg(name, kwOnly)

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

fun SkArguments.expectFunction(name: String, kwOnly: Boolean = false): SkFunction {
    val value = extractArg(name, kwOnly)

    if (value is SkFunction) {
        return value
    } else {
        illegalArg("Expected a function for parameter $name")
    }
}