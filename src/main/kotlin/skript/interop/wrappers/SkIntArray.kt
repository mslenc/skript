package skript.interop.wrappers

import skript.interop.SkCodec
import skript.interop.SkCodecInt
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkIntArray(val array: IntArray) : SkAbstractNativeArray() {
    override val klass: SkClassDef
        get() = SkIntArrayClassDef

    override fun getSize(): Int {
        return array.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return array[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        array[index] = SkCodecInt.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkIntArrayIterator(array)
    }
}

object SkIntArrayClassDef : SkClassDef("IntArray", SkAbstractListClassDef)

object SkCodecIntArray : SkCodec<IntArray> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is IntArray
    override fun toSkript(value: IntArray, env: SkriptEnv) = SkIntArray(value)
    override fun toKotlin(value: SkValue, env: SkriptEnv): IntArray {
        if (value is SkIntArray)
            return value.array

        if (value is SkAbstractList) {
            val len = value.getSize()
            val result = IntArray(len)
            for (i in 0 until len) {
                result[i] = SkCodecInt.toKotlin(value.getSlot(i))
            }
            return result
        }

        throw IllegalStateException("The value can't be converted into a IntArray")
    }
}

class SkIntArrayIterator(val array: IntArray) : SkIterator() {
    override val klass: SkClassDef
        get() = SkIntArrayIteratorClassDef

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

object SkIntArrayIteratorClassDef : SkClassDef("IntArrayIterator", SkIteratorClassDef)