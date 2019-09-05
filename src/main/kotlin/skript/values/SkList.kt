package skript.values

import skript.exec.RuntimeState
import skript.isString
import skript.notSupported

class SkList : SkObject {
    val elements = ArrayList<SkValue?>()

    constructor() : super(ListClass)

    constructor(elements: List<SkValue>) : super(ListClass) {
        this.elements.addAll(elements)
    }

    override suspend fun findMember(key: SkValue): SkValue {
        if (key.isString("length"))
            return SkNumber.valueOf(elements.size)

        key.toNonNegativeIntOrNull()?.let { index ->
            getSlot(index)
            return elements.getOrNull(index) ?: SkUndefined
        }

        return defaultFindMember(key)
    }

    override suspend fun hasOwnMember(key: SkValue): Boolean {
        if (key.isString("length"))
            return true

        key.toNonNegativeIntOrNull()?.let { index ->
            return index in elements.indices
        }

        return defaultHasOwnMember(key)
    }

    override suspend fun setMember(key: SkValue, value: SkValue) {
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

        defaultSetMember(key, value)
    }

    override suspend fun deleteMember(key: SkValue) {
        require(!key.isString("length")) { "Can't delete list length" }

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < elements.size)
                elements[index] = null
            return
        }

        defaultDeleteMember(key)
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

object ListClass : SkClass("List", ObjectClass) {
    init {
        defineInstanceMethod(List_push)
        defineInstanceMethod(List_concat)
        defineInstanceMethod(List_every)
        defineInstanceMethod(List_filter)
    }

    override suspend fun construct(posArgs: List<SkValue>, kwArgs: Map<String, SkValue>): SkValue {
        check(kwArgs.isEmpty()) { "List constructor doesn't support named arguments" }
        return SkList(posArgs)
    }
}

object List_push : SkMethod("push", emptyList()) {
    override val expectedClass: SkClass
        get() = ListClass

    override suspend fun call(thiz: SkValue, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        (thiz as? SkList) ?: throw IllegalStateException("Expected a list in List.push()")
        check(kwArgs.isEmpty()) { "List.push() doesn't accept named parameters" }
        thiz.elements.addAll(posArgs)
        return SkNumber.valueOf(thiz.elements.size)
    }
}

object List_concat : SkMethod("concat", emptyList()) {
    override val expectedClass: SkClass
        get() = ListClass

    override suspend fun call(thiz: SkValue, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        (thiz as? SkList) ?: throw IllegalStateException("Expected a list in List.push()")
        check(kwArgs.isEmpty()) { "List.concat() doesn't accept named parameters" }

        val newList = SkList()
        newList.elements.addAll(thiz.elements)
        for (arg in posArgs) {
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
    override val expectedClass: SkClass
        get() = ListClass

    override suspend fun call(thiz: SkValue, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        (thiz as? SkList) ?: throw IllegalStateException("Expected a list in List.push()")

        val callback = kwArgs["callback"] ?: posArgs.getOrNull(0)
        check(callback != null) { "No callback was specified" }
        check(callback is SkFunction) { "The callback must be a function" }

        for (i in thiz.elements.indices) {
            val value = thiz.elements[i] ?: continue

            val result = callback.call(listOf(value, SkNumber.valueOf(i), thiz), emptyMap(), state)
            if (!result.asBoolean().value)
                return SkBoolean.FALSE
        }

        return SkBoolean.TRUE
    }
}

object List_filter : SkMethod("filter", listOf("callback")) {
    override val expectedClass: SkClass
        get() = ListClass

    override suspend fun call(thiz: SkValue, posArgs: List<SkValue>, kwArgs: Map<String, SkValue>, state: RuntimeState): SkValue {
        (thiz as? SkList) ?: throw IllegalStateException("Expected a list in List.filter()")

        val callback = kwArgs["callback"] ?: posArgs.getOrNull(0)
        check(callback != null) { "No callback was specified" }
        check(callback is SkFunction) { "The callback must be a function" }

        val result = SkList()
        for (i in thiz.elements.indices) {
            val value = thiz.elements[i] ?: continue

            val test = callback.call(listOf(value, SkNumber.valueOf(i), thiz), emptyMap(), state)
            if (test.asBoolean().value) {
                result.elements.add(value)
            }
        }

        return result
    }
}

private fun SkValue.toNonNegativeIntOrNull(): Int? {
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