package skript.interop.wrappers

import skript.interop.SkCodec
import skript.interop.SkCodecDouble
import skript.io.SkriptEnv
import skript.io.toSkript
import skript.opcodes.SkIterator
import skript.opcodes.SkIteratorClassDef
import skript.values.*

class SkDoubleArray(val array: DoubleArray) : SkAbstractNativeArray() {
    override val klass: SkClassDef
        get() = SkDoubleArrayClassDef

    override fun getSize(): Int {
        return array.size
    }

    override fun getValidSlot(index: Int): SkValue {
        return array[index].toSkript()
    }

    override fun setValidSlot(index: Int, value: SkValue) {
        array[index] = SkCodecDouble.toKotlin(value)
    }

    override suspend fun makeIterator(): SkIterator {
        return SkDoubleArrayIterator(array)
    }
}

object SkDoubleArrayClassDef : SkClassDef("DoubleArray", SkAbstractListClassDef)

object SkCodecDoubleArray : SkCodec<DoubleArray> {
    override fun isMatch(kotlinVal: Any) = kotlinVal is DoubleArray
    override fun toSkript(value: DoubleArray, env: SkriptEnv) = SkDoubleArray(value)
    override fun toKotlin(value: SkValue, env: SkriptEnv): DoubleArray {
        if (value is SkDoubleArray)
            return value.array

        if (value is SkAbstractList) {
            val len = value.getSize()
            val result = DoubleArray(len)
            for (i in 0 until len) {
                result[i] = SkCodecDouble.toKotlin(value.getSlot(i))
            }
            return result
        }

        throw IllegalStateException("The value can't be converted into a DoubleArray")
    }
}

class SkDoubleArrayIterator(val array: DoubleArray) : SkIterator() {
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

object SkDoubleArrayIteratorClassDef : SkClassDef("DoubleArrayIterator", SkIteratorClassDef)