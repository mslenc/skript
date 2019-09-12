package skript.interop.wrappers

import skript.interop.SkCodec
import skript.interop.SkCodecBoolean
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkBooleanArray(val array: BooleanArray) : SkAbstractNativeArray() {
    override val klass: SkClassDef
        get() = SkBooleanArrayClassDef

    override fun getSize(): Int {
        return array.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return array[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        array[index] = SkCodecBoolean.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkBooleanArrayIterator(array)
    }
}

object SkBooleanArrayClassDef : SkClassDef("BooleanArray", SkAbstractListClassDef)

object SkCodecBooleanArray : SkCodec<BooleanArray> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is BooleanArray
    override fun toSkript(value: BooleanArray, env: SkriptEnv) = SkBooleanArray(value)
    override fun toKotlin(value: SkValue, env: SkriptEnv): BooleanArray {
        if (value is SkBooleanArray)
            return value.array

        if (value is SkAbstractList) {
            val len = value.getSize()
            val result = BooleanArray(len)
            for (i in 0 until len) {
                result[i] = SkCodecBoolean.toKotlin(value.getSlot(i))
            }
            return result
        }

        throw IllegalStateException("The value can't be converted into a BooleanArray")
    }
}

class SkBooleanArrayIterator(val array: BooleanArray) : SkIterator() {
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

object SkBooleanArrayIteratorClassDef : SkClassDef("BooleanArrayIterator", SkIteratorClassDef)