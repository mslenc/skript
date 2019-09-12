package skript.interop.wrappers

import skript.interop.SkCodec
import skript.interop.SkCodecShort
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkShortArray(val array: ShortArray) : SkAbstractNativeArray() {
    override val klass: SkClassDef
        get() = SkShortArrayClassDef

    override fun getSize(): Int {
        return array.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return array[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        array[index] = SkCodecShort.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkShortArrayIterator(array)
    }
}

object SkShortArrayClassDef : SkClassDef("ShortArray", null)

object SkCodecShortArray : SkCodec<ShortArray> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is ShortArray
    override fun toSkript(value: ShortArray, env: SkriptEnv) = SkShortArray(value)
    override fun toKotlin(value: SkValue, env: SkriptEnv): ShortArray {
        if (value is SkShortArray)
            return value.array

        if (value is SkAbstractList) {
            val len = value.getSize()
            val result = ShortArray(len)
            for (i in 0 until len) {
                result[i] = SkCodecShort.toKotlin(value.getSlot(i))
            }
            return result
        }

        throw IllegalStateException("The value can't be converted into a ShortArray")
    }
}

class SkShortArrayIterator(val array: ShortArray) : SkIterator() {
    override val klass: SkClassDef
        get() = SkShortArrayIteratorClassDef

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

object SkShortArrayIteratorClassDef : SkClassDef("ShortArrayIterator", SkIteratorClassDef)