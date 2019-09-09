package skript.interop.wrappers

import skript.exec.RuntimeState
import skript.interop.SkCodec
import skript.interop.SkCodecDouble
import skript.io.toSkript
import skript.isString
import skript.notSupported
import skript.opcodes.SkIterator
import skript.values.*

class SkDoubleArray(val array: DoubleArray) : SkObject() {
    override val klass: SkClassDef
        get() = SkDoubleArrayClassDef

    override fun getKind(): SkValueKind {
        return SkValueKind.OBJECT
    }

    override fun asObject(): SkObject {
        return this
    }

    override fun asBoolean(): SkBoolean {
        return SkBoolean.valueOf(array.isNotEmpty())
    }

    override fun asNumber(): SkNumber {
        notSupported("Can't convert a DoubleArray into a number")
    }

    override suspend fun setMember(key: SkValue, value: SkValue, state: RuntimeState) {
        if (key.isString("length"))
            notSupported("Can't change length of a DoubleArray")

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < array.size) {
                array[index] = SkCodecDouble.toKotlin(key, state)
                return
            }
        }

        super.setMember(key, value, state)
    }

    override suspend fun hasOwnMember(key: SkValue, state: RuntimeState): Boolean {
        if (key.isString("length"))
            return true

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < array.size)
                return true
        }

        return super.hasOwnMember(key, state)
    }

    override suspend fun findMember(key: SkValue, state: RuntimeState): SkValue {
        if (key.isString("length"))
            return SkNumber.valueOf(array.size)

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < array.size) {
                return array[index].toSkript()
            }
        }

        return super.findMember(key, state)
    }

    override suspend fun deleteMember(key: SkValue, state: RuntimeState) {
        if (key.isString("length"))
            notSupported("Can't delete length of DoubleArrays")

        super.deleteMember(key, state)
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
        notSupported("DoubleArrays can't be used to make ranges")
    }

    override suspend fun makeIterator(): SkValue {
        return SkDoubleArrayIterator(array)
    }
}

object SkDoubleArrayClassDef : SkClassDef("DoubleArray", null)

object SkCodecDoubleArray : SkCodec<DoubleArray> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is DoubleArray
    override suspend fun toSkript(value: DoubleArray, state: RuntimeState) = SkDoubleArray(value)
    override suspend fun toKotlin(value: SkValue, state: RuntimeState): DoubleArray {
        if (value is SkDoubleArray)
            return value.array

        if (value is SkList) {
            val len = value.getLength()
            val result = DoubleArray(len)
            for (i in 0 until len) {
                result[i] = SkCodecDouble.toKotlin(value.elements[i] ?: SkNull, state)
            }
            return result
        }

        throw IllegalStateException("The value can't be converted into a DoubleArray")
    }
}

class SkDoubleArrayIterator(val array: DoubleArray) : SkObject(), SkIterator {
    override val klass: SkClassDef
        get() = SkDoubleArrayIteratorClassDef

    var pos = -1

    override fun moveToNext(): Boolean {
        return ++pos < array.size
    }

    override fun getCurrentKey(): SkValue {
        return SkNumber.valueOf(pos)
    }

    override fun getCurrentValue(): SkValue {
        return array[pos].toSkript()
    }
}

object SkDoubleArrayIteratorClassDef : SkClassDef("DoubleArrayIterator", null)