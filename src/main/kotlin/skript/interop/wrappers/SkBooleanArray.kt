package skript.interop.wrappers

import skript.exec.RuntimeState
import skript.interop.SkCodec
import skript.interop.SkCodecBoolean
import skript.io.toSkript
import skript.isString
import skript.notSupported
import skript.opcodes.SkIterator
import skript.values.*

class SkBooleanArray(val array: BooleanArray) : SkObject() {
    override val klass: SkClassDef
        get() = SkBooleanArrayClassDef

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
        notSupported("Can't convert a BooleanArray into a number")
    }

    override suspend fun setMember(key: SkValue, value: SkValue, state: RuntimeState) {
        if (key.isString("length"))
            notSupported("Can't change length of a BooleanArray")

        key.toNonNegativeIntOrNull()?.let { index ->
            if (index < array.size) {
                array[index] = SkCodecBoolean.toKotlin(key, state)
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
            notSupported("Can't delete length of BooleanArrays")

        super.deleteMember(key, state)
    }

    override suspend fun makeRange(end: SkValue, endInclusive: Boolean, state: RuntimeState, exprDebug: String): SkValue {
        notSupported("BooleanArrays can't be used to make ranges")
    }

    override suspend fun makeIterator(): SkValue {
        return SkBooleanArrayIterator(array)
    }
}

object SkBooleanArrayClassDef : SkClassDef("BooleanArray", null)

object SkCodecBooleanArray : SkCodec<BooleanArray> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is BooleanArray
    override suspend fun toSkript(value: BooleanArray, state: RuntimeState) = SkBooleanArray(value)
    override suspend fun toKotlin(value: SkValue, state: RuntimeState): BooleanArray {
        if (value is SkBooleanArray)
            return value.array

        if (value is SkList) {
            val len = value.getLength()
            val result = BooleanArray(len)
            for (i in 0 until len) {
                result[i] = SkCodecBoolean.toKotlin(value.elements[i] ?: SkNull, state)
            }
            return result
        }

        throw IllegalStateException("The value can't be converted into a BooleanArray")
    }
}

class SkBooleanArrayIterator(val array: BooleanArray) : SkObject(), SkIterator {
    override val klass: SkClassDef
        get() = SkBooleanArrayIteratorClassDef

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

object SkBooleanArrayIteratorClassDef : SkClassDef("BooleanArrayIterator", null)