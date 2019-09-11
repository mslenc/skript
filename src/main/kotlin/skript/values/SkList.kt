package skript.values

import skript.exec.RuntimeState
import skript.io.SkriptEnv
import skript.isString
import skript.notSupported
import skript.util.SkArguments
import skript.util.expectFunction

class SkList : SkObject {
    val elements = ArrayList<SkValue?>()

    constructor() : super()

    constructor(elements: List<SkValue>) : super() {
        this.elements.addAll(elements)
    }

    override val klass: SkClassDef
        get() = SkListClassDef

    override suspend fun findMember(key: SkValue, state: RuntimeState): SkValue {
        if (key.isString("length"))
            return SkNumber.valueOf(elements.size)

        key.toNonNegativeIntOrNull()?.let { index ->
            getSlot(index)
            return elements.getOrNull(index) ?: SkUndefined
        }

        return defaultFindMember(key.asString().value)
    }

    override suspend fun hasOwnMember(key: SkValue, state: RuntimeState): Boolean {
        if (key.isString("length"))
            return true

        key.toNonNegativeIntOrNull()?.let { index ->
            return index in elements.indices
        }

        return defaultHasOwnMember(key)
    }

    override suspend fun setMember(key: SkValue, value: SkValue, state: RuntimeState) {
        if (key.isString("length")) {
            value.toNonNegativeIntOrNull()?.let { newLength ->
                when {
                    newLength < elements.size -> {
                        repeat (elements.size - newLength) {
                            elements.removeAt(elements.size - 1)
                        }
                    }
                    newLength > elements.size -> {
                        repeat (newLength - elements.size) {
                            elements.add(null)
                        }
                    }
                }
                return
            }

            throw IllegalArgumentException("List length can only be set to a non-negative integer")
        }

        key.toNonNegativeIntOrNull()?.let { index ->
            setSlot(index, value)
            return
        }

        defaultSetMember(key.asString().value, value)
    }

    override suspend fun deleteMember(key: SkValue, state: RuntimeState) {
        require(!key.isString("length")) { "Can't delete list length" }

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < elements.size)
                elements[index] = null
            return
        }

        super.deleteMember(key, state)
    }

    fun getSlot(index: Int): SkValue {
        return elements.getOrNull(index) ?: SkUndefined
    }

    fun setSlot(index: Int, value: SkValue) {
        require(index >= 0) { "Can't set values at negative indices" }

        when {
            index == elements.size -> elements.add(value)
            index < elements.size -> elements[index] = value
            else -> {
                while (elements.size < index)
                    elements.add(null)
                elements.add(value)
            }
        }
    }

    fun getLength(): Int {
        return elements.size
    }

    fun push(value: SkValue) {
        elements.add(value)
    }

    fun pushAll(values: SkList) {
        elements.addAll(values.elements)
    }

    override fun getKind(): SkValueKind {
        return SkValueKind.LIST
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(elements.isNotEmpty())
    }

    override fun asNumber(): SkNumber {
        notSupported("Can't convert a list into a number")
    }

    override fun asString(): SkString {
        return when {
            elements.isEmpty() -> SkString.EMPTY
            else -> {
                val sb = StringBuilder()
                for (i in 0 until elements.size) {
                    if (i > 0)
                        sb.append(',')

                    val element = elements[i]
                    if (element != null && element != SkNull && element != SkUndefined) {
                        sb.append(element.asString().value)
                    }
                }
                SkString(sb.toString())
            }
        }
    }

    override suspend fun makeIterator(): SkValue {
        return SkListIterator(this)
    }
}

object SkListClassDef : SkClassDef("List", SkObjectClassDef) {
    init {
        defineInstanceMethod(List_push)
        defineInstanceMethod(List_concat)
        defineInstanceMethod(List_every)
        defineInstanceMethod(List_filter)
        defineInstanceMethod(List_forEach)
    }

    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        check(args.noKwArgs()) { "List constructor doesn't support named arguments" }
        return SkList(args.getRemainingPosArgs())
    }
}

object List_push : SkMethod("push", emptyList()) {
    override val expectedClass: SkClassDef
        get() = SkListClassDef

    override suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState): SkValue {
        (thiz as? SkList) ?: throw IllegalStateException("Expected a list in List.push()")
        check(args.noKwArgs()) { "List.push() doesn't accept named parameters" }

        thiz.elements.addAll(args.getRemainingPosArgs())

        return SkNumber.valueOf(thiz.elements.size)
    }
}

object List_concat : SkMethod("concat", emptyList()) {
    override val expectedClass: SkClassDef
        get() = SkListClassDef

    override suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState): SkValue {
        (thiz as? SkList) ?: throw IllegalStateException("Expected a list in List.push()")

        check(args.noKwArgs()) { "List.concat() doesn't accept named parameters" }

        val newList = SkList()
        newList.elements.addAll(thiz.elements)
        for (arg in args.getRemainingPosArgs()) {
            if (arg is SkList) {
                newList.elements.addAll(arg.elements)
            } else {
                newList.elements.add(arg)
            }
        }
        return newList
    }
}

object List_every : SkMethod("every", listOf("callback")) {
    override val expectedClass: SkClassDef
        get() = SkListClassDef

    override suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState): SkValue {
        (thiz as? SkList) ?: throw IllegalStateException("Expected a list in List.push()")

        val callback = args.expectFunction("callback")
        args.expectNothingElse()

        for (i in thiz.elements.indices) {
            val value = thiz.elements[i] ?: continue

            val result = callback.call(SkArguments.of(value, SkNumber.valueOf(i), thiz), state)
            if (!result.asBoolean().value)
                return SkBoolean.FALSE
        }

        return SkBoolean.TRUE
    }
}

object List_filter : SkMethod("filter", listOf("callback")) {
    override val expectedClass: SkClassDef
        get() = SkListClassDef

    override suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState): SkValue {
        (thiz as? SkList) ?: throw IllegalStateException("Expected a list in List.filter()")

        val callback = args.expectFunction("callback")
        args.expectNothingElse()

        val result = SkList()
        for (i in thiz.elements.indices) {
            val value = thiz.elements[i] ?: continue

            val test = callback.call(SkArguments.of(value, SkNumber.valueOf(i), thiz), state)
            if (test.asBoolean().value) {
                result.elements.add(value)
            }
        }

        return result
    }
}

object List_forEach : SkMethod("forEach", listOf("callback")) {
    override val expectedClass: SkClassDef
        get() = SkListClassDef

    override suspend fun call(thiz: SkValue, args: SkArguments, state: RuntimeState): SkValue {
        (thiz as? SkList) ?: throw IllegalStateException("Expected a list in List.forEach()")

        val callback = args.expectFunction("callback")
        args.expectNothingElse()

        for (i in thiz.elements.indices) {
            val value = thiz.elements[i] ?: continue
            callback.call(SkArguments.of(value, SkNumber.valueOf(i), thiz), state)
        }

        return SkUndefined
    }
}

fun SkValue.toNonNegativeIntOrNull(): Int? {
    return when (getKind()) {
        SkValueKind.BOOLEAN,
        SkValueKind.NUMBER -> {
            val num = asNumber()
            if (num.isNonNegativeInt()) {
                num.value.toInt()
            } else {
                null
            }
        }
        SkValueKind.STRING -> {
            val str = asString().value
            val int = str.toIntOrNull() ?: return null

            if (int >= 0) {
                int
            } else {
                null
            }
        }
        else -> null
    }
}