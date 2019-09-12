package skript.values

import skript.exec.RuntimeState
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.notSupported
import skript.opcodes.SkIterator
import skript.opcodes.equals.aboutEqual
import skript.typeError
import skript.util.SkArguments

abstract class SkAbstractList : SkObject() {
    // read-only lists:
    abstract fun getSize(): Int
    protected abstract fun getValidSlot(index: Int): SkValue

    // writable, but non-resizable:
    open fun setSlot(index: Int, value: SkValue): Unit = typeError("This list is read-only")

    // full lists:
    open fun setSize(newSize: Int): Unit = typeError("This list is read-only")
    open fun add(value: SkValue): Unit = typeError("This list is read-only")
    open fun addAll(values: List<SkValue>): Unit = typeError("This list is read-only")
    open fun removeLast(): SkValue = typeError("This list is read-only")

    fun getSlot(index: Int): SkValue {
        if (index < 0 || index >= getSize())
            return SkUndefined

        return getValidSlot(index)
    }

    override suspend fun contains(key: SkValue, state: RuntimeState): Boolean {
        for (index in 0 until getSize())
            if (aboutEqual(getSlot(index), key))
                return true

        return false
    }

    override suspend fun elementGet(key: SkValue, state: RuntimeState): SkValue {
        key.toNonNegativeIntOrNull()?.let { index ->
            return getSlot(index)
        }

        return super.elementGet(key, state)
    }

    override suspend fun elementSet(key: SkValue, value: SkValue, state: RuntimeState) {
        key.toNonNegativeIntOrNull()?.let { index ->
            setSlot(index, value)
            return
        }

        super.elementSet(key, value, state)
    }

    override suspend fun elementDelete(key: SkValue, state: RuntimeState): Boolean {
        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < getSize()) {
                setSlot(index, SkUndefined)
                while (getSize() > 0 && getSlot(getSize() - 1) == SkUndefined) {
                    removeLast()
                }
                return true
            }
            return false
        }

        return super.elementDelete(key, state)
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(getSize() > 0)
    }

    override fun asNumber(): SkNumber {
        notSupported("Can't convert a list into a number")
    }

    override fun asString(): SkString {
        return when(val len = getSize()) {
            0 -> SkString.EMPTY
            1 -> getSlot(0).asString()
            else -> {
                val sb = StringBuilder()
                for (i in 0 until len) {
                    if (i > 0)
                        sb.append(',')

                    val element = getSlot(i)
                    if (element != SkNull && element != SkUndefined) {
                        sb.append(element.asString().value)
                    }
                }
                SkString(sb.toString())
            }
        }
    }

    override suspend fun makeIterator(): SkIterator {
        return SkListIterator(this)
    }
}

object SkAbstractListClassDef : SkCustomClass<SkAbstractList>("AbstractList", SkObjectClassDef) {
    init {
        defineMutableProperty("size",
            getter = { it.getSize().toSkript() },
            setter = { list, newLen ->
                val len = newLen.toNonNegativeIntOrNull() ?: typeError("Expected a non-negative int for list size")
                list.setSize(len)
            }
        )

        defineMethod("add").
            withRestPosArgs("elements").
            withImpl { list, elements, _ ->
                list.addAll(elements)
                list.getSize().toSkript()
            }

        defineMethod("addAll").
            withRestPosArgs("arrays").
            withImpl { list, arrays, _ ->
                for (array in arrays) {
                    if (array is SkList) {
                        list.addAll(array.listEls)
                    } else {
                        list.add(array)
                    }
                }
                list.getSize().toSkript()
            }

        defineMethod("every").
            withFunctionParam("callback").
            withImpl { list, callback, state ->
                for (index in 0 until list.getSize()) {
                    if (!callback.call(SkArguments.of(list.getSlot(index), index.toSkript(), list), state).asBoolean().value)
                        return@withImpl SkBoolean.FALSE
                }

                SkBoolean.TRUE
            }

        defineMethod("filter").
            withFunctionParam("callback").
            withImpl { list, callback, state ->
                val result = SkList()

                for (index in 0 until list.getSize()) {
                    val el = list.getSlot(index)
                    if (callback.call(SkArguments.of(el, index.toSkript(), list), state).asBoolean().value)
                        result.add(el)
                }

                result
            }

        defineMethod("forEach").
            withFunctionParam("callback").
            withImpl { list, callback, state ->
                for (index in 0 until list.getSize()) {
                    callback.call(SkArguments.of(list.getSlot(index), index.toSkript(), list), state)
                }

                SkUndefined
            }

        defineMethod("concat").
            withRestPosArgs("arrays").
            withImpl { list, arrays, _ ->
                val result = SkList()

                for (index in 0 until list.getSize())
                    result.add(list.getSlot(index))

                for (array in arrays) {
                    if (array is SkList) {
                        result.addAll(array.listEls)
                    } else {
                        result.add(array)
                    }
                }

                result
            }
    }

    override suspend fun construct(runtimeClass: SkClass, args: SkArguments, env: SkriptEnv): SkObject {
        check(args.noKwArgs()) { "List constructor doesn't support named arguments" }
        return SkList(args.getRemainingPosArgs())
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

fun SkValue.toIntOrNull(): Int? {
    return when (getKind()) {
        SkValueKind.BOOLEAN,
        SkValueKind.NUMBER -> {
            val num = asNumber()
            if (num.isInt()) {
                num.value.toInt()
            } else {
                null
            }
        }
        SkValueKind.STRING -> {
            val str = asString().value
            str.toIntOrNull() ?: return null
        }
        else -> null
    }
}