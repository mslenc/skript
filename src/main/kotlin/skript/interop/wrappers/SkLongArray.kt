package skript.interop.wrappers

import skript.interop.SkCodec
import skript.interop.SkCodecLong
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkLongArray(val array: LongArray) : SkAbstractNativeArray() {
    override val klass: SkClassDef
        get() = SkLongArrayClassDef

    override fun getSize(): Int {
        return array.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return array[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        array[index] = SkCodecLong.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkLongArrayIterator(array)
    }
}

object SkLongArrayClassDef : SkClassDef("LongArray", SkAbstractListClassDef)

object SkCodecLongArray : SkCodec<LongArray> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is LongArray
    override fun toSkript(value: LongArray, env: SkriptEnv) = SkLongArray(value)
    override fun toKotlin(value: SkValue, env: SkriptEnv): LongArray {
        if (value is SkLongArray)
            return value.array

        if (value is SkAbstractList) {
            val len = value.getSize()
            val result = LongArray(len)
            for (i in 0 until len) {
                result[i] = SkCodecLong.toKotlin(value.getSlot(i))
            }
            return result
        }

        throw IllegalStateException("The value can't be converted into a LongArray")
    }
}

class SkLongArrayIterator(val array: LongArray) : SkIterator() {
    override val klass: SkClassDef
        get() = SkLongArrayIteratorClassDef

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

object SkLongArrayIteratorClassDef : SkClassDef("LongArrayIterator", SkIteratorClassDef)